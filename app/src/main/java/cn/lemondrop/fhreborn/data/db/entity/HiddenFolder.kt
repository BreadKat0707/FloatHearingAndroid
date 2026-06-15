package cn.lemondrop.fhreborn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_folders")
data class HiddenFolder(
    @PrimaryKey val path: String,
    val addedAt: Long = System.currentTimeMillis()
)
