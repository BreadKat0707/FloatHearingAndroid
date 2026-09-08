package cn.lemondrop.fhreborn.data.repository

import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.entity.ScanDirectory
import cn.lemondrop.fhreborn.data.db.entity.Song
import kotlinx.coroutines.flow.Flow

class MediaLibraryRepository(database: AppDatabase) {

    private val songDao = database.songDao()
    private val scanDirectoryDao = database.scanDirectoryDao()
    private val hiddenFolderDao = database.hiddenFolderDao()

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val songCount: Flow<Int> = songDao.getSongCount()
    val scanDirectories: Flow<List<ScanDirectory>> = scanDirectoryDao.getActiveDirectories()
    val allScanDirectories: Flow<List<ScanDirectory>> = scanDirectoryDao.getAll()

    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    fun songsBySource(source: Int): Flow<List<Song>> = songDao.getAllSongsBySource(source)

    fun searchSongsBySource(query: String, source: Int): Flow<List<Song>> =
        songDao.searchSongsBySource(query, source)

    suspend fun getSongById(id: Long): Song? = songDao.getSongById(id)

    suspend fun insertSongs(songs: List<Song>) = songDao.insertAll(songs)

    /**
     * 全量替换媒体库歌曲：
     * - 删除数据库中本次扫描未出现的歌曲（文件已被删除）
     * - 保留仍在的歌曲的收藏状态与首次添加时间
     * - 插入/替换新扫描结果
     *
     * @return 从媒体库移除的歌曲数量
     */
    suspend fun replaceAllSongs(songs: List<Song>): Int = songDao.replaceAll(songs)

    suspend fun replaceAllSongsForSource(songs: List<Song>, source: Int): Int =
        songDao.replaceSource(songs, source)

    suspend fun deleteAllSongs() = songDao.deleteAll()

    suspend fun deleteSongByPath(path: String) = songDao.deleteByPath(path)

    suspend fun getActiveScanDirectoriesSnapshot(): List<ScanDirectory> =
        scanDirectoryDao.getActiveDirectoriesSnapshot()

    suspend fun addScanDirectory(path: String, name: String): Long {
        return scanDirectoryDao.insert(ScanDirectory(path = path, name = name))
    }

    suspend fun removeScanDirectory(directory: ScanDirectory) = scanDirectoryDao.delete(directory)

    suspend fun setScanDirectoryActive(directoryId: Long, active: Boolean) =
        scanDirectoryDao.setActive(directoryId, active)

    suspend fun isPathHidden(path: String): Boolean = hiddenFolderDao.isPathHidden(path)

    suspend fun setFavorite(songId: Long, favorite: Boolean) =
        songDao.setFavorite(songId, favorite)
}
