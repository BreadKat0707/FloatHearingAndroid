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
import cn.lemondrop.fhreborn.scanner.MediaScanner
import cn.lemondrop.fhreborn.scanner.ScanProgress
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
                .thenBy { it.discNumber ?: 0 }
                .thenBy { it.trackNumber ?: 0 }
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

    val artists: StateFlow<List<Artist>> = songs.map { songList ->
        songList.groupBy { it.artist }
            .map { (name, artistSongs) ->
                Artist(
                    name = name,
                    songCount = artistSongs.size,
                    albumCount = artistSongs.distinctBy { it.album }.size
                )
            }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

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
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
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
