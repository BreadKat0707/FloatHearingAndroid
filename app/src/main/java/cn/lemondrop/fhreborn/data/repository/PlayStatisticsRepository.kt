package cn.lemondrop.fhreborn.data.repository

import cn.lemondrop.fhreborn.data.db.dao.PlayRecordDao
import cn.lemondrop.fhreborn.data.db.dao.TopAlbumStat
import cn.lemondrop.fhreborn.data.db.dao.TopArtistStat
import cn.lemondrop.fhreborn.data.db.dao.TopSongStat
import cn.lemondrop.fhreborn.data.db.entity.PlayRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlayStatisticsRepository(private val playRecordDao: PlayRecordDao) {

    fun getTotalPlayDuration(): Flow<Long?> = playRecordDao.getTotalPlayDuration()

    fun getTotalPlayCount(): Flow<Int?> = playRecordDao.getTotalPlayCount()

    fun getMostPlayed(limit: Int = 10) = playRecordDao.getMostPlayed(limit)

    fun getTodayPlayDuration(): Flow<Long?> = playRecordDao.getPlayDurationSince(todayStart())

    fun getTodayPlayCount(): Flow<Int?> = playRecordDao.getPlayCountSince(todayStart())

    fun getWeekPlayDuration(): Flow<Long?> = playRecordDao.getPlayDurationSince(weekStart())

    fun getWeekPlayCount(): Flow<Int?> = playRecordDao.getPlayCountSince(weekStart())

    fun getMonthPlayDuration(): Flow<Long?> = playRecordDao.getPlayDurationSince(monthStart())

    fun getMonthPlayCount(): Flow<Int?> = playRecordDao.getPlayCountSince(monthStart())

    fun getTodayHourlyDuration(): Flow<List<Pair<Int, Long>>> {
        val start = todayStart()
        val end = todayEnd()
        return playRecordDao.getHourlyDurationToday(start, end).map { list ->
            list.associate { it.hour to it.duration }.toSortedMap().map { it.key to it.value }
        }
    }

    fun getWeekDailyDuration(): Flow<List<Pair<String, Long>>> {
        val start = weekStart()
        val end = now()
        val formatter = SimpleDateFormat("MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply { timeInMillis = start }
        return playRecordDao.getDailyDurationRange(start, end).map { list ->
            val map = list.associate { it.dayOffset to it.duration }
            val result = mutableListOf<Pair<String, Long>>()
            for (offset in 0 until 7) {
                calendar.timeInMillis = start
                calendar.add(Calendar.DAY_OF_MONTH, offset)
                result.add(formatter.format(calendar.time) to (map[offset] ?: 0L))
            }
            result
        }
    }

    fun getMonthDailyDuration(): Flow<List<Pair<String, Long>>> {
        val start = monthStart()
        val end = now()
        val formatter = SimpleDateFormat("dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply { timeInMillis = start }
        val daysInMonth = Calendar.getInstance().apply {
            timeInMillis = start
            getActualMaximum(Calendar.DAY_OF_MONTH)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        return playRecordDao.getDailyDurationRange(start, end).map { list ->
            val map = list.associate { it.dayOffset to it.duration }
            val result = mutableListOf<Pair<String, Long>>()
            for (offset in 0 until daysInMonth) {
                calendar.timeInMillis = start
                calendar.add(Calendar.DAY_OF_MONTH, offset)
                result.add(formatter.format(calendar.time) to (map[offset] ?: 0L))
            }
            result
        }
    }

    fun getTodayUniqueSongCount(): Flow<Int?> {
        val start = todayStart()
        val end = todayEnd()
        return playRecordDao.getUniqueSongCountRange(start, end)
    }

    fun getWeekUniqueSongCount(): Flow<Int?> {
        val start = weekStart()
        val end = now()
        return playRecordDao.getUniqueSongCountRange(start, end)
    }

    fun getMonthUniqueSongCount(): Flow<Int?> {
        val start = monthStart()
        val end = now()
        return playRecordDao.getUniqueSongCountRange(start, end)
    }

    fun getTodayTopSongs(limit: Int = 30): Flow<List<TopSongStat>> {
        val start = todayStart()
        val end = todayEnd()
        return playRecordDao.getTopSongsRange(start, end, limit)
    }

    fun getWeekTopSongs(limit: Int = 30): Flow<List<TopSongStat>> {
        val start = weekStart()
        val end = now()
        return playRecordDao.getTopSongsRange(start, end, limit)
    }

    fun getMonthTopSongs(limit: Int = 30): Flow<List<TopSongStat>> {
        val start = monthStart()
        val end = now()
        return playRecordDao.getTopSongsRange(start, end, limit)
    }

    fun getTodayTopArtists(limit: Int = 10): Flow<List<TopArtistStat>> {
        val start = todayStart()
        val end = todayEnd()
        return playRecordDao.getTopArtistsRange(start, end, limit)
    }

    fun getWeekTopArtists(limit: Int = 10): Flow<List<TopArtistStat>> {
        val start = weekStart()
        val end = now()
        return playRecordDao.getTopArtistsRange(start, end, limit)
    }

    fun getMonthTopArtists(limit: Int = 10): Flow<List<TopArtistStat>> {
        val start = monthStart()
        val end = now()
        return playRecordDao.getTopArtistsRange(start, end, limit)
    }

    fun getTodayTopAlbums(limit: Int = 10): Flow<List<TopAlbumStat>> {
        val start = todayStart()
        val end = todayEnd()
        return playRecordDao.getTopAlbumsRange(start, end, limit)
    }

    fun getWeekTopAlbums(limit: Int = 10): Flow<List<TopAlbumStat>> {
        val start = weekStart()
        val end = now()
        return playRecordDao.getTopAlbumsRange(start, end, limit)
    }

    fun getMonthTopAlbums(limit: Int = 10): Flow<List<TopAlbumStat>> {
        val start = monthStart()
        val end = now()
        return playRecordDao.getTopAlbumsRange(start, end, limit)
    }

    fun getLibrarySongsByPlayCount(): Flow<List<TopSongStat>> {
        return playRecordDao.getLibrarySongsPlayCount()
    }

    /**
     * 记录一次播放（可能跨天）：按自然日拆分成多条。
     *
     * 计入规则：文件时长 <30s 的歌曲播放即计入；≥30s 的需播放满 20s 才计入。
     * 跨天播放（如 23:50→00:10）按日期拆分，每段 timestamp 用段开始时刻，
     * 统计按日归属正确。
     */
    suspend fun recordPlayRange(songId: Long, startMs: Long, endMs: Long, songDuration: Long?) {
        val totalDuration = endMs - startMs
        if (totalDuration <= 0) return
        // 计入阈值
        val threshold = if (songDuration != null && songDuration < 30_000) 0L else 20_000L
        if (totalDuration < threshold) return
        // 跨天拆分
        val segments = splitByDay(startMs, endMs)
        segments.forEach { (segStart, segEnd) ->
            playRecordDao.insert(
                PlayRecord(songId = songId, playDuration = segEnd - segStart, timestamp = segStart)
            )
        }
    }

    /** 把 [startMs, endMs] 区间按自然日切分成若干段（每段在同一日内） */
    private fun splitByDay(startMs: Long, endMs: Long): List<Pair<Long, Long>> {
        val result = mutableListOf<Pair<Long, Long>>()
        var segStart = startMs
        while (segStart < endMs) {
            val segEnd = minOf(dayEndOf(segStart), endMs)
            result.add(segStart to segEnd)
            segStart = segEnd
        }
        return result
    }

    private fun dayEndOf(ts: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ts
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    suspend fun clearOldRecords(days: Int = 90) {
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        playRecordDao.deleteOldRecords(cutoff)
    }

    /** 清空全部播放记录（设置-数据管理：重置听歌统计） */
    suspend fun resetAll() = playRecordDao.deleteAll()

    private fun now(): Long = System.currentTimeMillis()

    private fun todayStart(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun todayEnd(): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun weekStart(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun monthStart(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
