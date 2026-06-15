package cn.lemondrop.fhreborn.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.lemondrop.fhreborn.data.db.entity.HiddenFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenFolderDao {
    @Query("SELECT * FROM hidden_folders")
    fun getAll(): Flow<List<HiddenFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: HiddenFolder)

    @Delete
    suspend fun delete(folder: HiddenFolder)

    @Query("DELETE FROM hidden_folders WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_folders WHERE :path LIKE path || '%')")
    suspend fun isPathHidden(path: String): Boolean
}
