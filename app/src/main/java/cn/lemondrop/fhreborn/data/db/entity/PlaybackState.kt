package cn.lemondrop.fhreborn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackState(
    @PrimaryKey val id: Int = 1,
    val currentSongId: Long? = null,
    val position: Long = 0,
    val queueJson: String = "[]",
    val repeatMode: Int = 0, // 0=顺序, 1=列表循环, 2=单曲循环
    val shuffleMode: Boolean = false
)
