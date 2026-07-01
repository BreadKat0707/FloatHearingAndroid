package cn.lemondrop.fhreborn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.entity.ScanDirectory
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.data.repository.MediaLibraryRepository
import cn.lemondrop.fhreborn.data.repository.PlayStatisticsRepository
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import cn.lemondrop.fhreborn.scanner.MediaScanner
import cn.lemondrop.fhreborn.scanner.ScanProgress
import cn.lemondrop.fhreborn.util.ArtistSplitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortField {
    TITLE, ARTIST_ALBUM, ALBUM_DISC_TRACK, MODIFIED_TIME, ADDED_TIME,
    PLAY_COUNT, PATH_FILENAME, FILE_NAME, RELEASE_YEAR, DURATION
}

enum class SortOrder { ASC, DESC }

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaLibraryRepository(AppDatabase.getInstance(application))
    private val scanner = MediaScanner(application)
    private val statisticsRepository = PlayStatisticsRepository(
        AppDatabase.getInstance(application).playRecordDao()
    )
    private val settingsRepository = SettingsRepository(application)

    val songCount = repository.songCount
    val scanDirectories = repository.scanDirectories

    private val _sortField = MutableStateFlow(SortField.TITLE)
    val sortField: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // 播放次数查找表
    private val playCountMap = MutableStateFlow<Map<Long, Int>>(emptyMap())

    init {
        viewModelScope.launch {
            // 恢复排序设置
            val savedField = settingsRepository.sortField.first()
            val savedOrder = settingsRepository.sortOrder.first()
            savedField?.let { _sortField.value = SortField.valueOf(it) }
            savedOrder?.let { _sortOrder.value = SortOrder.valueOf(it) }
        }
        viewModelScope.launch {
            statisticsRepository.getMostPlayed(1000).collect { stats ->
                playCountMap.value = stats.associate { it.songId to it.count }
            }
        }
    }

    val songs: StateFlow<List<Song>> = combine(
        repository.allSongs,
        _sortField,
        _sortOrder,
        playCountMap
    ) { songList, field, order, counts ->
        sortSongs(songList, field, order, counts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun sortSongs(
        list: List<Song>,
        field: SortField,
        order: SortOrder,
        counts: Map<Long, Int>
    ): List<Song> {
        val comparator: Comparator<Song> = when (field) {
            SortField.TITLE -> compareBy { it.title.lowercase() }
            SortField.ARTIST_ALBUM -> compareBy<Song> { it.artist.lowercase() }
                .thenBy { it.album.lowercase() }
                .thenBy { it.title.lowercase() }
            SortField.ALBUM_DISC_TRACK -> compareBy<Song> { it.album.lowercase() }
                .thenBy { it.discNumber ?: 1 }          // 无碟号视为第 1 碟
                .thenBy { it.trackNumber ?: Int.MAX_VALUE } // 无音轨号排在最后
                .thenBy { it.title.lowercase() }
            SortField.MODIFIED_TIME -> compareBy { it.modifiedAt }
            SortField.ADDED_TIME -> compareBy { it.addedAt }
            SortField.PLAY_COUNT -> compareBy { counts[it.id] ?: 0 }
            SortField.PATH_FILENAME -> compareBy { it.path.lowercase() }
            SortField.FILE_NAME -> compareBy { it.path.substringAfterLast('/').lowercase() }
            SortField.RELEASE_YEAR -> compareBy { it.year ?: 0 }
            SortField.DURATION -> compareBy { it.duration }
        }

        val finalComparator = if (order == SortOrder.DESC) comparator.reversed() else comparator
        return list.sortedWith(finalComparator)
    }

    val albums: StateFlow<List<Album>> = songs.map { songList ->
        songList.groupBy { it.album to (it.albumArtist ?: it.artist) }
            .map { (key, albumSongs) ->
                Album(
                    name = key.first,
                    artist = key.second,
                    songs = albumSongs,
                    coverSongId = albumSongs.first().id
                )
            }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = combine(
        songs,
        settingsRepository.artistSeparators
    ) { songList, separators ->
        songList
            .flatMap { song -> ArtistSplitter.split(song.artist, separators).map { it to song } }
            .groupBy { it.first }
            .map { (name, pairs) ->
                val artistSongs = pairs.map { it.second }
                Artist(
                    name = name,
                    songCount = artistSongs.size,
                    albumCount = artistSongs.distinctBy { it.album }.size
                )
            }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _artistSeparators = MutableStateFlow<Set<String>>(emptySet())
    val artistSeparators: StateFlow<Set<String>> = _artistSeparators.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.artistSeparators.collect {
                _artistSeparators.value = it
            }
        }
    }

    /** 根据当前分隔符获取某艺术家的歌曲列表。 */
    fun getArtistSongs(artist: String): List<Song> {
        val separators = _artistSeparators.value
        return songs.value.filter { ArtistSplitter.containsArtist(it.artist, artist, separators) }
    }

    /** 根据当前分隔符获取某艺术家作为主艺术家的专辑列表。 */
    fun getArtistAlbums(artist: String): List<Album> {
        return albums.value.filter { it.artist.equals(artist, ignoreCase = true) }
    }

    /** 根据当前分隔符获取某艺术家参与但不是主艺术家的专辑列表。 */
    fun getGuestAlbumsForArtist(artist: String): List<Album> {
        val separators = _artistSeparators.value
        return albums.value.filter { album ->
            !album.artist.equals(artist, ignoreCase = true)
                    && album.songs.any { song ->
                        ArtistSplitter.containsArtist(song.artist, artist, separators)
                    }
        }
    }

    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private var hasAutoScanned = false

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults = MutableStateFlow<List<Song>>(emptyList())

    init {
        viewModelScope.launch {
            _searchQuery.collectLatest { query ->
                if (query.isBlank()) {
                    searchResults.value = emptyList()
                } else {
                    repository.searchSongs(query).collect {
                        searchResults.value = sortSongs(it, _sortField.value, _sortOrder.value, playCountMap.value)
                    }
                }
            }
        }
    }

    fun setSortField(field: SortField) {
        _sortField.value = field
        viewModelScope.launch {
            settingsRepository.setSortField(field.name)
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            settingsRepository.setSortOrder(order.name)
        }
    }

    fun toggleSortOrder() {
        val newOrder = if (_sortOrder.value == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
        _sortOrder.value = newOrder
        viewModelScope.launch {
            settingsRepository.setSortOrder(newOrder.name)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun scanLibrary() {
        viewModelScope.launch {
            _scanProgress.value = ScanProgress.Scanning
            scanner.scan().collect { progress ->
                _scanProgress.value = progress
                if (progress is ScanProgress.Completed && progress.songs.isNotEmpty()) {
                    repository.insertSongs(progress.songs)
                }
            }
        }
    }

    /**
     * App 启动时自动快速刷新：只扫 MediaStore，不重复 FFmpeg 全目录，避免拖慢启动。
     */
    fun autoScanIfNeeded(hasStoragePermission: Boolean) {
        if (hasAutoScanned || !hasStoragePermission) return
        hasAutoScanned = true
        viewModelScope.launch {
            refreshMediaStore()
        }
    }

    /**
     * 手动从 MediaStore 刷新：读取系统媒体库中的音频文件变动（新增/删除/元数据修改）。
     */
    fun refreshMediaStore() {
        viewModelScope.launch {
            _scanProgress.value = ScanProgress.Scanning
            scanner.scan(quickScan = true).collect { progress ->
                _scanProgress.value = progress
                if (progress is ScanProgress.Completed && progress.songs.isNotEmpty()) {
                    repository.insertSongs(progress.songs)
                }
            }
        }
    }

    fun addScanDirectory(path: String, name: String) {
        viewModelScope.launch {
            repository.addScanDirectory(path, name)
        }
    }

    fun removeScanDirectory(directory: ScanDirectory) {
        viewModelScope.launch {
            repository.removeScanDirectory(directory)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.setFavorite(song.id, !song.isFavorite)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    val hiddenFolders: StateFlow<Set<String>> = settingsRepository.hiddenFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun hideFolder(path: String) {
        viewModelScope.launch {
            settingsRepository.addHiddenFolder(path)
        }
    }

    fun unhideFolder(path: String) {
        viewModelScope.launch {
            settingsRepository.removeHiddenFolder(path)
        }
    }

    data class Album(
        val name: String,
        val artist: String,
        val songs: List<Song>,
        val coverSongId: Long
    )

    data class Artist(
        val name: String,
        val songCount: Int,
        val albumCount: Int
    )

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(application) as T
        }
    }
}
