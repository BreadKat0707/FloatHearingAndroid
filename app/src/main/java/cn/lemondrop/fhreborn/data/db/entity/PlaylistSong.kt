package cn.lemondrop.fhreborn.data.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSong(
    val playlistId: Long,
    val songId: Long,
    val sortOrder: Int,
    val addedAt: Long = System.currentTimeMillis()
)
