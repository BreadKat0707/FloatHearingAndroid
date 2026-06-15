package cn.lemondrop.fhreborn.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.lemondrop.fhreborn.data.db.entity.ScanDirectory
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDirectoryDao {
    @Query("SELECT * FROM scan_directories WHERE isActive = 1")
    fun getActiveDirectories(): Flow<List<ScanDirectory>>

    @Query("SELECT * FROM scan_directories")
    fun getAll(): Flow<List<ScanDirectory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(directory: ScanDirectory): Long

    @Update
    suspend fun update(directory: ScanDirectory)

    @Delete
    suspend fun delete(directory: ScanDirectory)

    @Query("SELECT COUNT(*) FROM scan_directories")
    suspend fun getCount(): Int
}
