package cn.lemondrop.fhreborn.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.lemondrop.fhreborn.data.db.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE")
    fun searchSongs(query: String): Flow<List<Song>>

    /**
     * 专辑详情页数据源：按专辑名匹配，[albumArtist] 非空时再收窄到该专辑艺术家
     * （很多歌曲 albumArtist 为空，故放宽为“为空也算命中”）。
     * 排序按碟号 → 音轨号 → 标题，空值视为 0。
     */
    @Query(
        """
        SELECT * FROM songs
        WHERE album = :album
          AND (:albumArtist IS NULL OR albumArtist = :albumArtist OR albumArtist IS NULL)
        ORDER BY COALESCE(discNumber, 0) ASC, COALESCE(trackNumber, 0) ASC, title COLLATE NOCASE
        """
    )
    fun getSongsByAlbum(album: String, albumArtist: String?): Flow<List<Song>>

    // TODO(第二阶段): 专辑列表页需要聚合查询 getAllAlbums(): Flow<List<AlbumStat>>。
    // Room 将聚合结果映射到非 Entity 的 AlbumStat 需列名与字段名严格匹配（或用 @DatabaseView），
    // 本期不接入，留待专辑网格页一并实现。

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): Song?

    @Query("SELECT * FROM songs WHERE path = :path")
    suspend fun getSongByPath(path: String): Song?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Update
    suspend fun update(song: Song)

    @Delete
    suspend fun delete(song: Song)

    @Query("DELETE FROM songs WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsSnapshot(): List<Song>

    @Query("DELETE FROM songs WHERE path NOT IN (:paths)")
    suspend fun deleteAllNotInPaths(paths: Set<String>)

    /**
     * 全量替换歌曲：删除已不存在的歌曲，保留仍在的歌曲的收藏状态与首次添加时间。
     *
     * @return 从媒体库移除的歌曲数量
     */
    @androidx.room.Transaction
    suspend fun replaceAll(songs: List<Song>): Int {
        val newPaths = songs.map { it.path }.toSet()
        val existing = getAllSongsSnapshot().associateBy { it.path }
        val removed = existing.keys.count { it !in newPaths }

        val merged = songs.map { song ->
            existing[song.path]?.let { old ->
                song.copy(addedAt = old.addedAt, isFavorite = old.isFavorite)
            } ?: song
        }

        deleteAllNotInPaths(newPaths)
        insertAll(merged)
        return removed
    }

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>

    @Query("UPDATE songs SET isFavorite = :favorite WHERE id = :songId")
    suspend fun setFavorite(songId: Long, favorite: Boolean)
}
