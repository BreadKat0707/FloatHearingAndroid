package cn.lemondrop.fhreborn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.dao.TopAlbumStat
import cn.lemondrop.fhreborn.data.db.dao.TopArtistStat
import cn.lemondrop.fhreborn.data.db.dao.TopSongStat
import cn.lemondrop.fhreborn.data.repository.PlayStatisticsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val duration: Long = 0,
    val count: Int = 0,
    val uniqueSongs: Int = 0,
    val hourlyData: List<Pair<Int, Long>> = emptyList(),
    val topSongs: List<TopSongStat> = emptyList(),
    val topArtists: List<TopArtistStat> = emptyList(),
    val topAlbums: List<TopAlbumStat> = emptyList()
)

data class WeekUiState(
    val duration: Long = 0,
    val count: Int = 0,
    val uniqueSongs: Int = 0,
    val dailyData: List<Pair<String, Long>> = emptyList(),
    val topSongs: List<TopSongStat> = emptyList(),
    val topArtists: List<TopArtistStat> = emptyList(),
    val topAlbums: List<TopAlbumStat> = emptyList()
)

data class MonthUiState(
    val duration: Long = 0,
    val count: Int = 0,
    val uniqueSongs: Int = 0,
    val dailyData: List<Pair<String, Long>> = emptyList(),
    val topSongs: List<TopSongStat> = emptyList(),
    val topArtists: List<TopArtistStat> = emptyList(),
    val topAlbums: List<TopAlbumStat> = emptyList()
)

data class OverviewUiState(
    val totalDuration: Long = 0,
    val totalCount: Int = 0,
    val songCount: Int = 0,
    val songs: List<TopSongStat> = emptyList()
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val statisticsRepository = PlayStatisticsRepository(db.playRecordDao())
    private val songDao = db.songDao()

    val todayUiState: StateFlow<TodayUiState> = combine(
        listOf(
            statisticsRepository.getTodayPlayDuration(),
            statisticsRepository.getTodayPlayCount(),
            statisticsRepository.getTodayUniqueSongCount(),
            statisticsRepository.getTodayHourlyDuration(),
            statisticsRepository.getTodayTopSongs(30),
            statisticsRepository.getTodayTopArtists(10),
            statisticsRepository.getTodayTopAlbums(10)
        )
    ) { array ->
        TodayUiState(
            duration = array[0] as? Long ?: 0,
            count = array[1] as? Int ?: 0,
            uniqueSongs = array[2] as? Int ?: 0,
            hourlyData = array[3] as? List<Pair<Int, Long>> ?: emptyList(),
            topSongs = array[4] as? List<TopSongStat> ?: emptyList(),
            topArtists = array[5] as? List<TopArtistStat> ?: emptyList(),
            topAlbums = array[6] as? List<TopAlbumStat> ?: emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    val weekUiState: StateFlow<WeekUiState> = combine(
        listOf(
            statisticsRepository.getWeekPlayDuration(),
            statisticsRepository.getWeekPlayCount(),
            statisticsRepository.getWeekUniqueSongCount(),
            statisticsRepository.getWeekDailyDuration(),
            statisticsRepository.getWeekTopSongs(30),
            statisticsRepository.getWeekTopArtists(10),
            statisticsRepository.getWeekTopAlbums(10)
        )
    ) { array ->
        WeekUiState(
            duration = array[0] as? Long ?: 0,
            count = array[1] as? Int ?: 0,
            uniqueSongs = array[2] as? Int ?: 0,
            dailyData = array[3] as? List<Pair<String, Long>> ?: emptyList(),
            topSongs = array[4] as? List<TopSongStat> ?: emptyList(),
            topArtists = array[5] as? List<TopArtistStat> ?: emptyList(),
            topAlbums = array[6] as? List<TopAlbumStat> ?: emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeekUiState()
    )

    val monthUiState: StateFlow<MonthUiState> = combine(
        listOf(
            statisticsRepository.getMonthPlayDuration(),
            statisticsRepository.getMonthPlayCount(),
            statisticsRepository.getMonthUniqueSongCount(),
            statisticsRepository.getMonthDailyDuration(),
            statisticsRepository.getMonthTopSongs(30),
            statisticsRepository.getMonthTopArtists(10),
            statisticsRepository.getMonthTopAlbums(10)
        )
    ) { array ->
        MonthUiState(
            duration = array[0] as? Long ?: 0,
            count = array[1] as? Int ?: 0,
            uniqueSongs = array[2] as? Int ?: 0,
            dailyData = array[3] as? List<Pair<String, Long>> ?: emptyList(),
            topSongs = array[4] as? List<TopSongStat> ?: emptyList(),
            topArtists = array[5] as? List<TopArtistStat> ?: emptyList(),
            topAlbums = array[6] as? List<TopAlbumStat> ?: emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthUiState()
    )

    val overviewUiState: StateFlow<OverviewUiState> = combine(
        listOf(
            statisticsRepository.getTotalPlayDuration(),
            statisticsRepository.getTotalPlayCount(),
            songDao.getSongCount(),
            statisticsRepository.getLibrarySongsByPlayCount()
        )
    ) { array ->
        OverviewUiState(
            totalDuration = array[0] as? Long ?: 0,
            totalCount = array[1] as? Int ?: 0,
            songCount = array[2] as? Int ?: 0,
            songs = array[3] as? List<TopSongStat> ?: emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverviewUiState()
    )

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(application) as T
        }
    }
}
