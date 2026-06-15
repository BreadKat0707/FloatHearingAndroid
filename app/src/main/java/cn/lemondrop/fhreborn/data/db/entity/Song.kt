package cn.lemondrop.fhreborn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val path: String,
    val duration: Long,
    val format: String,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val fileSize: Long,
    val isFavorite: Boolean = false,
    val coverPath: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val year: Int? = null,
    val discNumber: Int? = null,
    val trackNumber: Int? = null
)
