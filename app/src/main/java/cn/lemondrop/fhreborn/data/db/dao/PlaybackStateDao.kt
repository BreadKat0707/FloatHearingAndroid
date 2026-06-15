package cn.lemondrop.fhreborn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.lemondrop.fhreborn.data.db.entity.PlaybackState
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackStateDao {
    @Query("SELECT * FROM playback_state WHERE id = 1")
    fun getPlaybackState(): Flow<PlaybackState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: PlaybackState)
}
