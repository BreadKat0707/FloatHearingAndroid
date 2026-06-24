package cn.lemondrop.fhreborn.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.entity.PlaybackState
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.data.lyrics.LyricReader
import cn.lemondrop.fhreborn.data.repository.PlayStatisticsRepository
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import cn.lemondrop.fhreborn.player.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val playbackStateDao = db.playbackStateDao()
    private val songDao = db.songDao()
    private val statisticsRepository = PlayStatisticsRepository(db.playRecordDao())

    private var mediaController: MediaController? = null
    private var hasRestoredState = false

    // 播放统计计时
    private var playStartTime: Long = 0L
    private var currentPlaySongId: Long? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _timerRemaining = MutableStateFlow(0L)
    val timerRemaining: StateFlow<Long> = _timerRemaining.asStateFlow()
    private var timerJob: Job? = null

    private val _endOfSongTimer = MutableStateFlow(false)
    val isEndOfSongTimer: StateFlow<Boolean> = _endOfSongTimer.asStateFlow()

    private val _lyrics = MutableStateFlow<SyncedLyrics?>(null)
    val lyrics: StateFlow<SyncedLyrics?> = _lyrics.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    // 外部（通知栏/媒体控件）请求打开播放器页面的一次性事件
    private val _openPlayerEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openPlayerEvent: SharedFlow<Unit> = _openPlayerEvent.asSharedFlow()

    init {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                val controller = controllerFuture.get()
                mediaController = controller
                setupPlayerListener(controller)
                syncState(controller)

                // MediaController 就绪后恢复上次状态
                viewModelScope.launch {
                    restoreLastState(controller)
                }
            },
            MoreExecutors.directExecutor()
        )

        // 定期更新播放进度并定时保存
        viewModelScope.launch {
            var lastSaveTime = 0L
            while (isActive) {
                mediaController?.let { controller ->
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    _currentPosition.value = pos
                    _duration.value = controller.duration.coerceAtLeast(0L)

                    // 更新当前歌词索引
                    val synced = _lyrics.value
                    val lines = synced?.lines
                    if (!lines.isNullOrEmpty()) {
                        val idx = lines.indexOfLast { it.start <= pos }
                        if (idx != _currentLyricIndex.value) {
                            _currentLyricIndex.value = idx
                        }
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastSaveTime > 5000) {
                        saveState()
                        lastSaveTime = now
                    }
                }
                delay(500)
            }
        }
    }

    private suspend fun restoreLastState(controller: MediaController) {
        if (hasRestoredState) return
        val state = playbackStateDao.getPlaybackState().first() ?: return
        hasRestoredState = true

        val songIds = state.queueJson.split(",").mapNotNull { it.toLongOrNull() }
        if (songIds.isEmpty()) return

        val songs = songIds.mapNotNull { songDao.getSongById(it) }
        if (songs.isEmpty()) return

        // 如果播放器/服务已经持有媒体项且当前歌曲一致，说明进程未重建，
        // 直接同步现有状态，避免用 DB 里的旧进度把播放拉回去。
        if (controller.mediaItemCount > 0 &&
            controller.currentMediaItem?.mediaId == state.currentSongId?.toString()
        ) {
            _queue.value = songs
            syncState(controller)
            loadLyrics()
            return
        }

        val currentIndex = if (state.currentSongId != null) {
            songs.indexOfFirst { it.id == state.currentSongId }.coerceAtLeast(0)
        } else 0

        _queue.value = songs

        val mediaItems = songs.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems)
        controller.seekTo(currentIndex, state.position)
        controller.repeatMode = state.repeatMode
        controller.shuffleModeEnabled = state.shuffleMode
        controller.prepare()

        // 同步 UI
        _currentIndex.value = currentIndex
        _currentSong.value = songs.getOrNull(currentIndex)
        _currentPosition.value = state.position
        _repeatMode.value = state.repeatMode
        _shuffleMode.value = state.shuffleMode

        loadLyrics()
    }

    private fun setupPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    playStartTime = System.currentTimeMillis()
                    currentPlaySongId = _currentSong.value?.id
                } else {
                    recordPlayTime()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                recordPlayTime()
                val index = controller.currentMediaItemIndex
                _currentIndex.value = index
                _currentSong.value = _queue.value.getOrNull(index)
                loadLyrics()
                saveState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _duration.value = controller.duration.coerceAtLeast(0L)
                if (playbackState == Player.STATE_ENDED && _endOfSongTimer.value) {
                    mediaController?.pause()
                    _endOfSongTimer.value = false
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
                saveState()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleMode.value = shuffleModeEnabled
                saveState()
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                _currentIndex.value = controller.currentMediaItemIndex
                _currentSong.value = _queue.value.getOrNull(controller.currentMediaItemIndex)
            }
        })
    }

    private fun syncState(controller: MediaController) {
        _isPlaying.value = controller.isPlaying
        _currentIndex.value = controller.currentMediaItemIndex
        _currentSong.value = _queue.value.getOrNull(controller.currentMediaItemIndex)
        _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
        _duration.value = controller.duration.coerceAtLeast(0L)
        _repeatMode.value = controller.repeatMode
        _shuffleMode.value = controller.shuffleModeEnabled
    }

    private fun ensureServiceStarted() {
        val context = getApplication<Application>()
        ContextCompat.startForegroundService(
            context,
            Intent(context, PlaybackService::class.java)
        )
    }

    private fun saveState() {
        viewModelScope.launch {
            val controller = mediaController ?: return@launch
            val state = PlaybackState(
                id = 1,
                currentSongId = _currentSong.value?.id,
                position = controller.currentPosition.coerceAtLeast(0L),
                queueJson = _queue.value.map { it.id }.joinToString(","),
                repeatMode = controller.repeatMode,
                shuffleMode = controller.shuffleModeEnabled
            )
            playbackStateDao.save(state)
        }
    }

    fun requestOpenPlayer() {
        _openPlayerEvent.tryEmit(Unit)
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val controller = mediaController ?: return
        ensureServiceStarted()
        _queue.value = songs
        val mediaItems = songs.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        controller.prepare()
        controller.play()
        hasRestoredState = true
        loadLyrics()
        saveState()
    }

    private fun loadLyrics() {
        val song = _currentSong.value ?: return
        viewModelScope.launch {
            val lyricText = LyricReader.readLyrics(getApplication(), song)
            _lyrics.value = lyricText?.let {
                try {
                    AutoParser().parse(it)
                } catch (e: Exception) {
                    android.util.Log.w("PlayerViewModel", "歌词解析失败: ${e.message}")
                    null
                }
            }
            _currentLyricIndex.value = -1
        }
    }

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            ensureServiceStarted()
            controller.play()
        }
    }

    fun next() {
        mediaController?.seekToNextMediaItem()
    }

    fun previous() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun seekTo(index: Int) {
        mediaController?.seekToDefaultPosition(index)
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        controller.removeMediaItem(index)
        _queue.value = _queue.value.toMutableList().apply { removeAt(index) }
        saveState()
    }

    fun setTimer(minutes: Int) {
        timerJob?.cancel()
        _endOfSongTimer.value = false
        val durationMs = minutes * 60 * 1000L
        _timerRemaining.value = durationMs
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive && _timerRemaining.value > 0) {
                val elapsed = System.currentTimeMillis() - startTime
                _timerRemaining.value = (durationMs - elapsed).coerceAtLeast(0)
                delay(1000)
            }
            if (_timerRemaining.value <= 0) {
                mediaController?.pause()
                _timerRemaining.value = 0
            }
        }
    }

    fun setEndOfSongTimer() {
        timerJob?.cancel()
        _timerRemaining.value = 0
        _endOfSongTimer.value = true
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _timerRemaining.value = 0
        _endOfSongTimer.value = false
    }

    private fun recordPlayTime() {
        val songId = currentPlaySongId ?: _currentSong.value?.id ?: return
        if (playStartTime > 0) {
            val duration = System.currentTimeMillis() - playStartTime
            viewModelScope.launch {
                statisticsRepository.recordPlay(songId, duration)
            }
        }
        playStartTime = 0
        currentPlaySongId = null
    }

    override fun onCleared() {
        recordPlayTime()
        saveState()
        timerJob?.cancel()
        mediaController?.release()
        super.onCleared()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(application) as T
        }
    }
}

fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(android.net.Uri.parse("content://media/external/audio/media/$id"))
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .setTag(this)
        .build()
}
