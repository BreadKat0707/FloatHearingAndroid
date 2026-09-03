package cn.lemondrop.fhreborn.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.util.LruCache
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
import cn.lemondrop.fhreborn.data.lyrics.LyricSource
import cn.lemondrop.fhreborn.data.lyrics.LyricSourceType
import cn.lemondrop.fhreborn.data.lyrics.LyricFormatType
import cn.lemondrop.fhreborn.data.lyrics.LyricParser
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.data.repository.PlayStatisticsRepository
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import cn.lemondrop.fhreborn.player.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // 统计总开关（设置-数据管理）
    private val settingsRepository = AppSettingsRepository(application)
    private val _statsEnabled = MutableStateFlow(true)
    init {
        viewModelScope.launch {
            settingsRepository.statsEnabled.collect { _statsEnabled.value = it }
        }
    }

    // 歌词缓存：避免每次打开播放器 / 切歌都重新读文件 + 解析标签（重 IO 操作）
    private val lyricSourceCache = object : LruCache<Long, LyricSource>(32) {}
    private val parsedLyricCache = object : LruCache<Long, SyncedLyrics?>(32) {}

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

    // 计时到点后是否等当前曲目播完再暂停
    private val _pauseAfterCurrentSong = MutableStateFlow(false)
    val pauseAfterCurrentSong: StateFlow<Boolean> = _pauseAfterCurrentSong.asStateFlow()

    // 计划暂停对话框显示状态（全局托管）
    private val _showScheduledPauseDialog = MutableStateFlow(false)
    val showScheduledPauseDialog: StateFlow<Boolean> = _showScheduledPauseDialog.asStateFlow()

    // 原始歌词（含逐字数据）与逐字开关：发布值 = 开关开启时用原始，关闭时降级为整行高亮
    private val _rawLyrics = MutableStateFlow<SyncedLyrics?>(null)
    private val _wordLevelEnabled = MutableStateFlow(true)
    val lyrics: StateFlow<SyncedLyrics?> = combine(_rawLyrics, _wordLevelEnabled) { raw, wordLevel ->
        if (raw == null || wordLevel) raw else downgradeToPlainLyrics(raw)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    /** 开关逐字歌词：关闭后当前歌词即时降级为整行高亮 */
    fun setWordLevelEnabled(enabled: Boolean) {
        _wordLevelEnabled.value = enabled
    }

    private val _lyricSource = MutableStateFlow<LyricSource?>(null)
    val lyricSource: StateFlow<LyricSource?> = _lyricSource.asStateFlow()

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
                    val synced = _rawLyrics.value
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
                    _pauseAfterCurrentSong.value = false
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
        // 默认启用列表循环（点击列表/默认播放场景）；歌单播放等由调用方后续覆盖
        controller.repeatMode = Player.REPEAT_MODE_ALL
        controller.play()
        hasRestoredState = true
        loadLyrics()
        saveState()
    }

    /**
     * 将歌曲追加到播放队列末尾（不打断当前播放）。
     */
    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val controller = mediaController ?: return
        ensureServiceStarted()
        _queue.value = _queue.value + songs
        controller.addMediaItems(songs.map { it.toMediaItem() })
        saveState()
    }

    /**
     * 将歌曲插入到当前播放的下一首（不打断当前播放）。
     */
    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        val controller = mediaController ?: return
        ensureServiceStarted()
        val insertIndex = (_currentIndex.value + 1).coerceAtMost(_queue.value.size)
        val newQueue = _queue.value.toMutableList()
        newQueue.addAll(insertIndex, songs)
        _queue.value = newQueue
        controller.addMediaItems(insertIndex, songs.map { it.toMediaItem() })
        saveState()
    }

    /**
     * 播放歌单：按歌单默认播放模式设置 repeatMode / shuffle。
     * @param playMode 歌单的 defaultPlayMode（0=顺序 1=列表循环 2=单曲循环 3=随机）
     */
    fun playPlaylistSongs(songs: List<Song>, startIndex: Int = 0, playMode: Int = 0) {
        // playSongs 内部会先设为列表循环，这里按歌单默认模式覆盖
        playSongs(songs, startIndex)
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = playMode == 3
        controller.repeatMode = when (playMode) {
            2 -> Player.REPEAT_MODE_ONE
            1 -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setRepeatMode(mode: Int) {
        mediaController?.repeatMode = mode
    }

    fun setShuffle(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    private var lyricLoadJob: kotlinx.coroutines.Job? = null

    private fun loadLyrics() {
        val song = _currentSong.value ?: return
        // 切歌时不清空歌词，等待新歌词加载完成后再替换
        // 这样可以避免歌词跳动（先显示空内容再跳到当前行）
        // 取消上一次未完成的加载：快速连续切歌时避免旧协程后完成导致歌词错配
        lyricLoadJob?.cancel()
        lyricLoadJob = viewModelScope.launch {
            // 延迟加载，避开切歌高峰（封面/背景切换、播放器动画）
            delay(300)

            // 缓存命中：直接复用，不重新读文件/解析
            val cachedSource = lyricSourceCache.get(song.id)
            val cachedParsed = parsedLyricCache.get(song.id)
            if (cachedSource != null && cachedParsed != null) {
                android.util.Log.i(TAG, "lyric cache hit: ${song.id} (${System.currentTimeMillis()})")
                _lyricSource.value = cachedSource
                _rawLyrics.value = cachedParsed
                _currentLyricIndex.value = -1
                return@launch
            }

            android.util.Log.i(TAG, "lyric load start: ${song.id} (${System.currentTimeMillis()})")
            // 关键：readLyrics 内部有同步文件 IO + jaudiotagger 解析音频标签 +
            // MediaMetadataRetriever，必须在 IO 线程执行，否则切歌时阻塞主线程导致 ANR
            val source = withContext(Dispatchers.IO) {
                LyricReader.readLyrics(getApplication(), song)
            }
            android.util.Log.i(TAG, "lyric read done: ${song.id} (${System.currentTimeMillis()})")
            val parsed = source.rawText?.let {
                try {
                    withContext(Dispatchers.Default) {
                        dedupeDuplicateLyricLines(AutoParser().parse(it))
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "歌词解析失败: ${e.message}")
                    null
                }
            }
            android.util.Log.i(TAG, "lyric parse done: ${song.id} (${System.currentTimeMillis()})")

            // 提取罗马音数据并应用到解析后的歌词
            val romanjiMap = source.rawText?.let { LyricParser.extractRomanjiFromTTML(it) }
            val lyricsWithRomanji = if (romanjiMap != null && romanjiMap.isNotEmpty() && parsed != null) {
                applyRomanjiToLyrics(parsed, romanjiMap)
            } else {
                parsed
            }

            lyricSourceCache.put(song.id, source)
            // LruCache 不允许 null value：无歌词/解析失败时不缓存 parsed
            if (lyricsWithRomanji != null) {
                parsedLyricCache.put(song.id, lyricsWithRomanji)
            }

            _lyricSource.value = source
            _rawLyrics.value = lyricsWithRomanji
            _currentLyricIndex.value = -1
            android.util.Log.i(TAG, "lyric applied: ${song.id} (${System.currentTimeMillis()})")
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

    /**
     * 清空整个播放队列（停止当前播放）。
     */
    fun clearQueue() {
        val controller = mediaController
        controller?.clearMediaItems()
        _queue.value = emptyList()
        _currentSong.value = null
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
                if (_pauseAfterCurrentSong.value) {
                    // 到点改为“播完当前曲目再停”，由 STATE_ENDED 处理实际暂停
                    _endOfSongTimer.value = true
                } else {
                    mediaController?.pause()
                }
                _timerRemaining.value = 0
            }
        }
    }

    fun setEndOfSongTimer() {
        timerJob?.cancel()
        _timerRemaining.value = 0
        _endOfSongTimer.value = true
    }

    fun setPauseAfterCurrentSong(enabled: Boolean) {
        _pauseAfterCurrentSong.value = enabled
    }

    fun showScheduledPause() {
        _showScheduledPauseDialog.value = true
    }

    fun hideScheduledPause() {
        _showScheduledPauseDialog.value = false
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _timerRemaining.value = 0
        _endOfSongTimer.value = false
        _pauseAfterCurrentSong.value = false
    }

    private fun recordPlayTime() {
        val songId = currentPlaySongId ?: _currentSong.value?.id ?: return
        val start = playStartTime
        if (start > 0) {
            val end = System.currentTimeMillis()
            val songDuration = _currentSong.value?.duration
            viewModelScope.launch {
                // 设置-数据管理：统计关闭时不记录
                if (_statsEnabled.value) {
                    statisticsRepository.recordPlayRange(songId, start, end, songDuration)
                }
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

    companion object {
        private const val TAG = "SongSwitch"
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
        // 明确 mime 类型，帮助 renderer 精确匹配（系统解码器不支持的格式落到 FFmpeg）
        .setMimeType(mimeTypeForFormat(format))
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

/** 按歌曲格式映射 MIME 类型（用于 renderer 选择：系统优先，FFmpeg 兜底） */
private fun mimeTypeForFormat(format: String): String? = when (format.uppercase()) {
    "MP3" -> "audio/mpeg"
    "M4A" -> "audio/mp4"
    "ALAC" -> "audio/alac"
    "FLAC" -> "audio/flac"
    "OGG" -> "audio/ogg"
    "OPUS" -> "audio/opus"
    "WAV" -> "audio/wav"
    "AAC" -> "audio/aac"
    else -> null
}

/**
 * 合并相邻且内容相同的歌词行。
 *
 * Apple Music 的 TTML 常见结构：intro 段（begin=0）与第一句内容相同，
 * 解析后第一行会重复显示两遍。这里把内容相同的相邻行合并为一行。
 * 合并规则：
 * - 文本按去除空白后比较（中文歌词常带空格/全角空格差异）
 * - 不要求行类型一致（同一句可能一行带逐字时间戳、一行不带）
 * - 优先保留带逐字时间戳的主卡拉OK行，开始/结束时间取并集
 */
private fun dedupeDuplicateLyricLines(lyrics: SyncedLyrics): SyncedLyrics {
    fun lineText(line: ISyncedLine): String? = when (line) {
        is SyncedLine -> line.content
        is KaraokeLine -> line.syllables.joinToString("") { it.content }
        else -> null
    }
    fun normalized(text: String) = text.trim().filterNot { it.isWhitespace() }

    val merged = mutableListOf<ISyncedLine>()
    for (line in lyrics.lines) {
        val last = merged.lastOrNull()
        val lastText = last?.let { lineText(it) }?.let(::normalized)
        val lineTextValue = lineText(line)?.let(::normalized)
        if (last != null && lastText != null && lineTextValue != null && lastText == lineTextValue) {
            val newStart = minOf(last.start, line.start)
            val newEnd = maxOf(last.end, line.end)
            // 优先保留带逐字时间戳的主卡拉OK行
            val preferred = when {
                last is KaraokeLine.MainKaraokeLine && line is SyncedLine -> last
                last is SyncedLine && line is KaraokeLine.MainKaraokeLine -> line
                else -> last
            }
            merged[merged.size - 1] = when (preferred) {
                is SyncedLine -> preferred.copy(start = newStart, end = newEnd)
                is KaraokeLine.MainKaraokeLine -> preferred.copy(start = newStart, end = newEnd)
                else -> preferred
            }
        } else {
            merged.add(line)
        }
    }
    return if (merged.size == lyrics.lines.size) lyrics else lyrics.copy(lines = merged)
}

/**
 * 将罗马音数据应用到解析后的歌词
 * 根据时间戳匹配罗马音到对应的歌词行
 */
private fun applyRomanjiToLyrics(lyrics: SyncedLyrics, romanjiMap: Map<Int, String>): SyncedLyrics {
    if (romanjiMap.isEmpty()) return lyrics

    val updatedLines = lyrics.lines.map { line ->
        if (line is KaraokeLine.MainKaraokeLine) {
            // 尝试通过时间戳匹配罗马音
            val romanji = romanjiMap[line.start]
            if (romanji != null && line.phonetic.isNullOrBlank()) {
                KaraokeLine.MainKaraokeLine(
                    syllables = line.syllables,
                    translation = line.translation,
                    alignment = line.alignment,
                    start = line.start,
                    end = line.end,
                    accompanimentLines = line.accompanimentLines,
                    phonetic = romanji
                )
            } else {
                line
            }
        } else {
            line
        }
    }
    return lyrics.copy(lines = updatedLines)
}

/**
 * 逐字歌词降级为整行高亮：KaraokeLine 转 SyncedLine（丢弃音节级时间戳）。
 * 关闭"逐字歌词"开关时使用，渲染走整行高亮路径。
 */
private fun downgradeToPlainLyrics(lyrics: SyncedLyrics): SyncedLyrics {
    val plainLines = lyrics.lines.map { line ->
        if (line is KaraokeLine) {
            SyncedLine(
                content = line.syllables.joinToString("") { it.content },
                translation = line.translation,
                start = line.start,
                end = line.end
            )
        } else {
            line
        }
    }
    return lyrics.copy(lines = plainLines)
}
