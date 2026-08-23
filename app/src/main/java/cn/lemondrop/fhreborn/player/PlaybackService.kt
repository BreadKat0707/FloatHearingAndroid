package cn.lemondrop.fhreborn.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import cn.lemondrop.fhreborn.R
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.entity.PlaybackState
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.viewmodel.toMediaItem
import cn.lemondrop.fhreborn.player.effects.AudioEffectsManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PlaybackService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private var audioEffectsManager: AudioEffectsManager? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var periodicSaveJob: Job? = null

    /** 防抖：多个回调在短时间内的保存请求合并为一次 */
    private var saveRequested = false
    private var debouncedSaveJob: Job? = null

    private val db by lazy { AppDatabase.getInstance(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // FFmpeg 解码器兜底：系统解码器（MediaCodec）优先，系统不支持/解码失败的格式
        // （如 ALAC 变体）自动回退到 FFmpeg 软件解码器（EXTENSION_RENDERER_MODE_PREFER）
        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // 监听音频会话 ID 变化，用于初始化音频效果器
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    audioEffectsManager?.release()
                    audioEffectsManager = AudioEffectsManager(audioSessionId)
                    applySavedAudioSettings()
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                // 暂停/停止时立即保存，防止切出应用后位置丢失
                if (!playing) requestSavePlaybackState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                requestSavePlaybackState()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                requestSavePlaybackState()
            }
        })

        player = exoPlayer

        // 后台持续播放时定期保存进度（防抖合并后的兜底保存）
        periodicSaveJob = serviceScope.launch {
            while (isActive) {
                delay(10_000)
                savePlaybackState()
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            exoPlayer,
            object : MediaLibrarySession.Callback {
                override fun onGetLibraryRoot(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    params: MediaLibraryService.LibraryParams?
                ): ListenableFuture<LibraryResult<MediaItem>> {
                    val rootItem = MediaItem.Builder()
                        .setMediaId("root")
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle("媒体库")
                                .build()
                        )
                        .build()
                    return Futures.immediateFuture(
                        LibraryResult.ofItem(rootItem, params)
                    )
                }

                override fun onGetChildren(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    parentId: String,
                    page: Int,
                    pageSize: Int,
                    params: MediaLibraryService.LibraryParams?
                ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                    return Futures.immediateFuture(
                        LibraryResult.ofItemList(ImmutableList.of(), params)
                    )
                }

                override fun onGetItem(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    mediaId: String
                ): ListenableFuture<LibraryResult<MediaItem>> {
                    return Futures.immediateFuture(
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN)
                    )
                }
            }
        ).build()

        // Service 被系统回收或重新启动后，从数据库恢复播放状态，
        // 确保通知栏/控制中心能继续显示当前歌曲和控制。
        // 延迟恢复：setMediaItems(数百首队列) 在模拟器上可能耗时数十秒，
        // 立即执行会把主线程占满，触发 executing service 20s 超时 ANR。
        // 先让 onCreate 快速完成、startForeground 生效，再慢慢恢复。
        serviceScope.launch {
            delay(2000)
            restorePlaybackState()
        }

        // 立即进入前台，避免切出 App 后被系统回收
        startForeground(
            PLAYBACK_NOTIFICATION_ID,
            createInitialNotification().notification
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        periodicSaveJob?.cancel()
        debouncedSaveJob?.cancel()
        // 释放前同步保存一次。runBlocking 在 IO 线程执行，不阻塞主线程，
        // 避免主线程等待 Room 写入造成 ANR；内部读取会切回主线程（ExoPlayer 线程约束）。
        runBlocking(Dispatchers.IO) {
            savePlaybackState()
        }
        serviceScope.cancel()
        audioEffectsManager?.release()
        mediaLibrarySession?.release()
        player?.release()
        super.onDestroy()
    }

    private fun createInitialNotification(): MediaNotification {
        val notification = NotificationCompat.Builder(this, PLAYBACK_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(mediaLibrarySession!!))
            .build()
        return MediaNotification(PLAYBACK_NOTIFICATION_ID, notification)
    }

    private fun restorePlaybackState() {
        serviceScope.launch {
            try {
                val state = db.playbackStateDao().getPlaybackState().first() ?: return@launch
                val songIds = state.queueJson.split(",").mapNotNull { it.toLongOrNull() }
                if (songIds.isEmpty()) return@launch
                val songs = songIds.mapNotNull { db.songDao().getSongById(it) }
                if (songs.isEmpty()) return@launch

                val currentIndex = if (state.currentSongId != null) {
                    songs.indexOfFirst { it.id == state.currentSongId }.coerceAtLeast(0)
                } else 0

                // MediaItem 构建（轻量但量大）在 IO 线程完成，主线程只做 setMediaItems
                val mediaItems = songs.map { it.toMediaItem() }

                // 拆分批次恢复：一次 setMediaItems 处理几百首在模拟器上会长时间占满
                // 主线程（ANR 风险），先恢复当前歌曲 + 小范围队列，剩余分批补全。
                val batchSize = 100
                withContext(Dispatchers.Main) {
                    val currentPlayer = player ?: return@withContext
                    // 已有媒体项说明用户/前端已经设置了新队列（或热启动），
                    // 此时若用 DB 旧状态覆盖，会抢走新队列并还原旧的 shuffle/repeat
                    //（典型现象：点歌单随机播放，几秒后变成按上次的顺序播放）。
                    if (currentPlayer.mediaItemCount > 0) return@withContext
                    // 第一批：包含当前歌曲的 100 首窗口
                    val start = (currentIndex - batchSize / 2).coerceAtLeast(0)
                    val firstBatch = mediaItems.subList(start, (start + batchSize).coerceAtMost(mediaItems.size))
                    currentPlayer.setMediaItems(firstBatch)
                    currentPlayer.seekTo(
                        (currentIndex - start).coerceIn(0, firstBatch.size - 1),
                        state.position
                    )
                    currentPlayer.repeatMode = state.repeatMode
                    currentPlayer.shuffleModeEnabled = state.shuffleMode
                    currentPlayer.prepare()

                    // 剩余部分分批补全（让主线程有空隙处理输入事件）
                    if (firstBatch.size < mediaItems.size) {
                        serviceScope.launch {
                            delay(500)
                            withContext(Dispatchers.Main) {
                                val remaining = ArrayList(mediaItems)
                                remaining.removeAll(firstBatch.toSet())
                                currentPlayer.addMediaItems(remaining)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "恢复播放状态失败", e)
            }
        }
    }

    /**
     * 防抖保存请求：多个高频回调（切歌/seek/暂停）在短时间内的保存合并为一次，
     * 避免并发保存风暴压垮主线程和数据库。
     */
    private fun requestSavePlaybackState() {
        if (saveRequested) return
        saveRequested = true
        debouncedSaveJob = serviceScope.launch {
            delay(500)
            saveRequested = false
            savePlaybackState()
        }
    }

    private suspend fun savePlaybackState() {
        // ExoPlayer 必须在主线程访问，先切到主线程读取快照，再切到 IO 写入数据库
        val state = withContext(Dispatchers.Main.immediate) {
            val currentPlayer = player ?: return@withContext null
            try {
                val songs = mutableListOf<Song>()
                for (i in 0 until currentPlayer.mediaItemCount) {
                    val tag = currentPlayer.getMediaItemAt(i).localConfiguration?.tag
                    if (tag is Song) songs.add(tag)
                }
                val currentSong = currentPlayer.currentMediaItem?.localConfiguration?.tag as? Song
                PlaybackState(
                    id = 1,
                    currentSongId = currentSong?.id,
                    position = currentPlayer.currentPosition.coerceAtLeast(0L),
                    queueJson = songs.map { it.id }.joinToString(","),
                    repeatMode = currentPlayer.repeatMode,
                    shuffleMode = currentPlayer.shuffleModeEnabled
                )
            } catch (e: Exception) {
                Log.w(TAG, "读取播放状态失败", e)
                null
            }
        } ?: return

        withContext(Dispatchers.IO) {
            try {
                db.playbackStateDao().save(state)
            } catch (e: Exception) {
                Log.w(TAG, "保存播放状态失败", e)
            }
        }
    }

    /**
     * 应用保存的音频设置（从 SharedPreferences 或 DataStore 读取）
     * 当前为占位实现，后续可通过设置页面配置
     */
    private fun applySavedAudioSettings() {
        val effects = audioEffectsManager ?: return
        // 默认关闭所有效果器，由用户手动开启
        effects.setEqualizerEnabled(false)
        effects.setBassBoostEnabled(false)
        effects.setVirtualizerEnabled(false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "播放控制",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放通知"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "playback_channel"
        const val PLAYBACK_NOTIFICATION_ID = 1001
        private const val TAG = "PlaybackService"
    }
}
