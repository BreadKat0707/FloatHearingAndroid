package cn.lemondrop.fhreborn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cn.lemondrop.fhreborn.data.db.entity.Playlist
import cn.lemondrop.fhreborn.data.db.entity.PlaylistSong
import cn.lemondrop.fhreborn.data.db.entity.Song
import kotlinx.coroutines.flow.Flow

/** 歌单列表项：歌单 + 歌曲数 */
data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val description: String?,
    val coverPath: String?,
    val coverSource: Int,
    val defaultPlayMode: Int,
    val tags: String?,
    val sortType: Int,
    val isSystem: Boolean,
    val playCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val songCount: Int
)

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.*, COUNT(ps.songId) AS songCount
        FROM playlists p
        LEFT JOIN playlist_songs ps ON ps.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.createdAt ASC
        """
    )
    fun getAllPlaylists(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylist(id: Long): Flow<Playlist?>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name, description = :description, defaultPlayMode = :defaultPlayMode, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePlaylist(id: Long, name: String, description: String?, defaultPlayMode: Int, updatedAt: Long)

    @Query("UPDATE playlists SET sortType = :sortType, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortType(id: Long, sortType: Int, updatedAt: Long)

    @Query("UPDATE playlists SET coverPath = :coverPath, coverSource = :coverSource, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCover(id: Long, coverPath: String?, coverSource: Int, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteSongsOfPlaylist(playlistId: Long)

    /** 歌单内歌曲（按添加顺序） */
    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON ps.songId = s.id
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.sortOrder ASC, ps.addedAt ASC
        """
    )
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>

    /** 歌单内歌曲快照（一次性查询，用于播放/导出） */
    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON ps.songId = s.id
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.sortOrder ASC, ps.addedAt ASC
        """
    )
    suspend fun getSongsInPlaylistSnapshot(playlistId: Long): List<Song>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Int

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getSongIdsInPlaylist(playlistId: Long): List<Long>

    /** 歌单内前 N 首歌曲 id（自动封面拼图用） */
    @Query(
        "SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY sortOrder ASC, addedAt ASC LIMIT :limit"
    )
    suspend fun getFirstSongIds(playlistId: Long, limit: Int): List<Long>

    /** 歌单内前 N 首完整歌曲（自动封面拼图用，保留 source/path 供目录模式封面读取） */
    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON ps.songId = s.id
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.sortOrder ASC, ps.addedAt ASC
        LIMIT :limit
        """
    )
    suspend fun getFirstSongs(playlistId: Long, limit: Int): List<Song>

    /** 包含指定歌曲的所有歌单 id（用于"已加入"标记） */
    @Query("SELECT playlistId FROM playlist_songs WHERE songId = :songId")
    suspend fun getPlaylistIdsContainingSong(songId: Long): List<Long>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxSortOrder(playlistId: Long): Int

    @Query("UPDATE playlist_songs SET sortOrder = :sortOrder WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSortOrder(playlistId: Long, songId: Long, sortOrder: Int)

    /** 批量重排歌单内歌曲（sortOrder = 列表下标），事务内完成 */
    @Transaction
    suspend fun reorderSongs(playlistId: Long, songIds: List<Long>) {
        songIds.forEachIndexed { index, songId ->
            updateSortOrder(playlistId, songId, index)
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(playlistSong: PlaylistSong)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(playlistSongs: List<PlaylistSong>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE songId = :songId")
    suspend fun removeSongFromAllPlaylists(songId: Long)

    @Query("SELECT COUNT(*) FROM playlist_songs")
    fun getTotalCount(): Flow<Int>

    /** 播放次数 +1（进入播放时调用） */
    @Query("UPDATE playlists SET playCount = playCount + 1 WHERE id = :id")
    suspend fun incrementPlayCount(id: Long)

    /** 歌单内歌曲总时长（毫秒） */
    @Query(
        """
        SELECT COALESCE(SUM(s.duration), 0) FROM songs s
        INNER JOIN playlist_songs ps ON ps.songId = s.id
        WHERE ps.playlistId = :playlistId
        """
    )
    suspend fun getTotalDuration(playlistId: Long): Long

    /** 添加单首歌曲（自动分配 sortOrder），返回是否新增 */
    @Transaction
    suspend fun addSong(playlistId: Long, songId: Long): Boolean {
        if (isSongInPlaylist(playlistId, songId) > 0) return false
        val order = getMaxSortOrder(playlistId) + 1
        insertSong(PlaylistSong(playlistId, songId, order))
        return true
    }
}
