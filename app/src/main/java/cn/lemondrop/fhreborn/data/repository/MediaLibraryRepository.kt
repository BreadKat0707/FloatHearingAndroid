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

    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    suspend fun getSongById(id: Long): Song? = songDao.getSongById(id)

    suspend fun insertSongs(songs: List<Song>) = songDao.insertAll(songs)

    suspend fun deleteAllSongs() = songDao.deleteAll()

    suspend fun deleteSongByPath(path: String) = songDao.deleteByPath(path)

    suspend fun addScanDirectory(path: String, name: String): Long {
        return scanDirectoryDao.insert(ScanDirectory(path = path, name = name))
    }

    suspend fun removeScanDirectory(directory: ScanDirectory) = scanDirectoryDao.delete(directory)

    suspend fun isPathHidden(path: String): Boolean = hiddenFolderDao.isPathHidden(path)

    suspend fun setFavorite(songId: Long, favorite: Boolean) =
        songDao.setFavorite(songId, favorite)
}
