package cn.lemondrop.fhreborn.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 播放记录实体
 *
 * 每次播放时记录，用于统计和分析
 */
@Entity(
    tableName = "play_records",
    indices = [
        Index(value = ["timestamp"], name = "idx_play_records_timestamp"),
        Index(value = ["songId"], name = "idx_play_records_songId")
    ]
)
data class PlayRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playDuration: Long, // 播放时长（毫秒）
    val timestamp: Long = System.currentTimeMillis()
)
