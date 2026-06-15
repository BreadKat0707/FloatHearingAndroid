package cn.lemondrop.fhreborn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_directories")
data class ScanDirectory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val name: String,
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
