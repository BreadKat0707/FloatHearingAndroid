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

    /**
     * 统计今天每小时的播放时长
     */
    @Query("""
        SELECT ((timestamp - :startOfDay) / 3600000) AS hour, SUM(playDuration) AS duration
        FROM play_records
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay
        GROUP BY hour
        ORDER BY hour
    """)
    fun getHourlyDurationToday(startOfDay: Long, endOfDay: Long): Flow<List<HourlyDuration>>

    /**
     * 按天聚合指定时间范围的播放时长
     */
    @Query("""
        SELECT ((timestamp - :startTime) / 86400000) AS dayOffset, SUM(playDuration) AS duration
        FROM play_records
        WHERE timestamp >= :startTime AND timestamp < :endTime
        GROUP BY dayOffset
        ORDER BY dayOffset
    """)
    fun getDailyDurationRange(startTime: Long, endTime: Long): Flow<List<DailyDuration>>

    /**
     * 指定时间范围内的去重歌曲数
     */
    @Query("SELECT COUNT(DISTINCT songId) FROM play_records WHERE timestamp >= :startTime AND timestamp < :endTime")
    fun getUniqueSongCountRange(startTime: Long, endTime: Long): Flow<Int?>

    /**
     * 指定时间范围内播放最多的歌曲
     */
    @Query("""
        SELECT p.songId, s.title, s.artist, s.album, COUNT(*) AS count, SUM(p.playDuration) AS totalDuration
        FROM play_records p
        INNER JOIN songs s ON p.songId = s.id
        WHERE p.timestamp >= :startTime AND p.timestamp < :endTime
        GROUP BY p.songId
        ORDER BY count DESC, totalDuration DESC
        LIMIT :limit
    """)
    fun getTopSongsRange(startTime: Long, endTime: Long, limit: Int): Flow<List<TopSongStat>>

    /**
     * 指定时间范围内最常听的艺术家
     */
    @Query("""
        SELECT s.artist, COUNT(*) AS count, SUM(p.playDuration) AS totalDuration
        FROM play_records p
        INNER JOIN songs s ON p.songId = s.id
        WHERE p.timestamp >= :startTime AND p.timestamp < :endTime
        GROUP BY s.artist
        ORDER BY count DESC, totalDuration DESC
        LIMIT :limit
    """)
    fun getTopArtistsRange(startTime: Long, endTime: Long, limit: Int): Flow<List<TopArtistStat>>

    /**
     * 指定时间范围内最常听的专辑
     */
    @Query("""
        SELECT s.album, s.artist AS albumArtist, COUNT(*) AS count, SUM(p.playDuration) AS totalDuration
        FROM play_records p
        INNER JOIN songs s ON p.songId = s.id
        WHERE p.timestamp >= :startTime AND p.timestamp < :endTime
        GROUP BY s.album, s.artist
        ORDER BY count DESC, totalDuration DESC
        LIMIT :limit
    """)
    fun getTopAlbumsRange(startTime: Long, endTime: Long, limit: Int): Flow<List<TopAlbumStat>>

    /**
     * 媒体库全部歌曲及其累计播放次数/时长（含未播放的歌曲）
     */
    @Query("""
        SELECT s.id AS songId, s.title, s.artist, s.album, COUNT(p.id) AS count, COALESCE(SUM(p.playDuration), 0) AS totalDuration
        FROM songs s
        LEFT JOIN play_records p ON s.id = p.songId
        GROUP BY s.id
        ORDER BY count DESC, totalDuration DESC
    """)
    fun getLibrarySongsPlayCount(): Flow<List<TopSongStat>>
}

/**
 * 歌曲播放统计（DAO 返回的投影类，非 Room Entity）
 */
data class SongPlayStat(
    val songId: Long,
    val count: Int,
    val totalDuration: Long
)

data class HourlyDuration(
    val hour: Int,
    val duration: Long
)

data class DailyDuration(
    val dayOffset: Int,
    val duration: Long
)

data class TopSongStat(
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val count: Int,
    val totalDuration: Long
)

data class TopArtistStat(
    val artist: String,
    val count: Int,
    val totalDuration: Long
)

data class TopAlbumStat(
    val album: String,
    val albumArtist: String,
    val count: Int,
    val totalDuration: Long
)
