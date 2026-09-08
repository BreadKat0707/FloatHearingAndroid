package cn.lemondrop.fhreborn.data.repository

import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.dao.PlaylistWithCount
import cn.lemondrop.fhreborn.data.db.entity.Playlist
import cn.lemondrop.fhreborn.data.db.entity.PlaylistSong
import cn.lemondrop.fhreborn.data.db.entity.PlaylistSortType
import cn.lemondrop.fhreborn.data.db.entity.Song
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val db: AppDatabase) {

    private val dao = db.playlistDao()

    fun getAllPlaylists(): Flow<List<PlaylistWithCount>> = dao.getAllPlaylists()

    suspend fun getPlaylist(id: Long): Playlist? = dao.getPlaylistById(id)

    fun observePlaylist(id: Long): Flow<Playlist?> = dao.observePlaylist(id)

    suspend fun createPlaylist(name: String, description: String?): Long {
        val now = System.currentTimeMillis()
        return dao.insertPlaylist(
            Playlist(
                name = name,
                description = description,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updatePlaylist(id: Long, name: String, description: String?, defaultPlayMode: Int) {
        dao.updatePlaylist(id, name, description, defaultPlayMode, System.currentTimeMillis())
    }

    suspend fun deletePlaylist(id: Long) {
        dao.deleteSongsOfPlaylist(id)
        dao.deletePlaylist(id)
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> = dao.getSongsInPlaylist(playlistId)

    /** 按歌单排序类型排序后的歌曲列表（内存排序，歌单歌曲量小） */
    fun sortSongs(songs: List<Song>, sortType: Int): List<Song> = when (sortType) {
        PlaylistSortType.TITLE -> songs.sortedBy { it.title.lowercase() }
        PlaylistSortType.ARTIST -> songs.sortedWith(
            compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
        )
        PlaylistSortType.ALBUM -> songs.sortedWith(
            compareBy({ it.album.lowercase() }, { it.title.lowercase() })
        )
        PlaylistSortType.DURATION -> songs.sortedBy { it.duration }
        else -> songs // 自定义：DAO 已按 sortOrder, addedAt 返回
    }

    suspend fun updateSortType(id: Long, sortType: Int) {
        dao.updateSortType(id, sortType, System.currentTimeMillis())
    }

    /** 更新歌单封面（coverPath + coverSource） */
    suspend fun updateCover(id: Long, coverPath: String?, coverSource: Int) {
        dao.updateCover(id, coverPath, coverSource, System.currentTimeMillis())
    }

    /** 歌单内前 N 首歌曲 id（自动封面用） */
    suspend fun getFirstSongIds(playlistId: Long, limit: Int = 3): List<Long> =
        dao.getFirstSongIds(playlistId, limit)

    /** 歌单内前 N 首完整歌曲（自动封面用，保留 source/path） */
    suspend fun getFirstSongs(playlistId: Long, limit: Int = 3): List<Song> =
        dao.getFirstSongs(playlistId, limit)

    /** 歌单内歌曲快照（一次性，用于播放/导出） */
    suspend fun getSongsSnapshot(playlistId: Long, sortType: Int): List<Song> =
        sortSongs(dao.getSongsInPlaylistSnapshot(playlistId), sortType)

    suspend fun addSong(playlistId: Long, songId: Long) {
        dao.addSong(playlistId, songId)
    }

    /** 批量添加歌曲到歌单（忽略已存在的） */
    suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        val existing = dao.getSongIdsInPlaylist(playlistId).toSet()
        var order = dao.getMaxSortOrder(playlistId) + 1
        val now = System.currentTimeMillis()
        val toInsert = songIds.filter { it !in existing }.map { songId ->
            PlaylistSong(playlistId, songId, order++, now)
        }
        if (toInsert.isNotEmpty()) {
            dao.insertSongs(toInsert)
        }
    }

    suspend fun removeSong(playlistId: Long, songId: Long) {
        dao.removeSong(playlistId, songId)
    }

    suspend fun removeSongFromAllPlaylists(songId: Long) {
        dao.removeSongFromAllPlaylists(songId)
    }

    /** 批量重排歌单内歌曲（sortOrder = 列表下标） */
    suspend fun reorderSongs(playlistId: Long, songIds: List<Long>) {
        dao.reorderSongs(playlistId, songIds)
    }

    /** 播放次数 +1 */
    suspend fun incrementPlayCount(playlistId: Long) {
        dao.incrementPlayCount(playlistId)
    }

    /** 歌单内歌曲总时长（毫秒） */
    suspend fun getTotalDuration(playlistId: Long): Long =
        dao.getTotalDuration(playlistId)

    /** 包含指定歌曲的歌单 id 列表（用于"已加入"标记） */
    suspend fun getPlaylistIdsContainingSong(songId: Long): List<Long> =
        dao.getPlaylistIdsContainingSong(songId)
}
