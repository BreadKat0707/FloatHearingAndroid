package cn.lemondrop.fhreborn.data.repository

import cn.lemondrop.fhreborn.data.db.dao.PlayRecordDao
import cn.lemondrop.fhreborn.data.db.entity.PlayRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class PlayStatisticsRepository(private val playRecordDao: PlayRecordDao) {

    fun getTotalPlayDuration(): Flow<Long?> = playRecordDao.getTotalPlayDuration()

    fun getTotalPlayCount(): Flow<Int?> = playRecordDao.getTotalPlayCount()

    fun getMostPlayed(limit: Int = 10) = playRecordDao.getMostPlayed(limit)

    fun getTodayPlayDuration(): Flow<Long?> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return playRecordDao.getPlayDurationSince(startOfDay)
    }

    fun getTodayPlayCount(): Flow<Int?> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return playRecordDao.getPlayCountSince(startOfDay)
    }

    fun getWeekPlayDuration(): Flow<Long?> {
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return playRecordDao.getPlayDurationSince(startOfWeek)
    }

    suspend fun recordPlay(songId: Long, duration: Long) {
        if (duration < 3000) return // 低于3秒不记录
        playRecordDao.insert(PlayRecord(songId = songId, playDuration = duration))
    }

    suspend fun clearOldRecords(days: Int = 90) {
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        playRecordDao.deleteOldRecords(cutoff)
    }
}
