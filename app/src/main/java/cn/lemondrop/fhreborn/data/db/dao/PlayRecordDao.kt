package cn.lemondrop.fhreborn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.lemondrop.fhreborn.data.db.entity.PlayRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayRecordDao {

    @Insert
    suspend fun insert(record: PlayRecord)

    @Query("SELECT * FROM play_records ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PlayRecord>>

    @Query("SELECT SUM(playDuration) FROM play_records")
    fun getTotalPlayDuration(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM play_records")
    fun getTotalPlayCount(): Flow<Int?>

    /**
     * 获取最常播放的歌曲统计
     */
    @Query("""
        SELECT songId, COUNT(*) as count, SUM(playDuration) as totalDuration 
        FROM play_records 
        GROUP BY songId 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    fun getMostPlayed(limit: Int = 10): Flow<List<SongPlayStat>>

    @Query("SELECT * FROM play_records WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getRecordsSince(startTime: Long): Flow<List<PlayRecord>>

    @Query("DELETE FROM play_records WHERE timestamp < :beforeTime")
    suspend fun deleteOldRecords(beforeTime: Long)

    @Query("SELECT SUM(playDuration) FROM play_records WHERE timestamp >= :startTime")
    fun getPlayDurationSince(startTime: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM play_records WHERE timestamp >= :startTime")
    fun getPlayCountSince(startTime: Long): Flow<Int?>
}

/**
 * 歌曲播放统计（DAO 返回的投影类，非 Room Entity）
 */
data class SongPlayStat(
    val songId: Long,
    val count: Int,
    val totalDuration: Long
)
