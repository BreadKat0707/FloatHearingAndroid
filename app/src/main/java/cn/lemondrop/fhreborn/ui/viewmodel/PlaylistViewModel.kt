package cn.lemondrop.fhreborn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.dao.PlaylistWithCount
import cn.lemondrop.fhreborn.data.db.entity.Playlist
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository(AppDatabase.getInstance(application))
    private val songDao = AppDatabase.getInstance(application).songDao()
    private val playlistStates = HashMap<Long, StateFlow<Playlist?>>()
    private val sortedSongStates = HashMap<Pair<Long, Int>, StateFlow<List<Song>>>()

    fun getAllPlaylists(): Flow<List<PlaylistWithCount>> = repository.getAllPlaylists()

    fun getPlaylist(id: Long): StateFlow<Playlist?> = playlistStates.getOrPut(id) {
        repository.observePlaylist(id).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> = repository.getSongsInPlaylist(playlistId)

    /** 歌单歌曲 + 排序类型合并：按当前 sortType 排序后输出 */
    fun getSortedSongs(playlistId: Long, sortType: Int): StateFlow<List<Song>> =
        sortedSongStates.getOrPut(playlistId to sortType) {
            combine(repository.getSongsInPlaylist(playlistId), flowOf(sortType)) { songs, type ->
                repository.sortSongs(songs, type)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        }

    fun setSortType(playlistId: Long, sortType: Int) {
        viewModelScope.launch {
            repository.updateSortType(playlistId, sortType)
        }
    }

    /** 更新封面（coverSource: 0=自动 1=选歌曲 2=自选图片） */
    fun setCover(playlistId: Long, coverPath: String?, coverSource: Int) {
        viewModelScope.launch {
            repository.updateCover(playlistId, coverPath, coverSource)
        }
    }

    /** 歌单前 N 首歌曲 id（自动封面拼图用，回调形式） */
    private val firstSongIdsCache = HashMap<Long, List<Long>>()
    fun getFirstSongIds(playlistId: Long, limit: Int = 3, onResult: (List<Long>) -> Unit) {
        // 内存缓存：列表滚动重建 item 时避免重复查询 DB（歌单封面加载不掉帧）
        firstSongIdsCache[playlistId]?.let { onResult(it); return }
        viewModelScope.launch {
            val ids = repository.getFirstSongIds(playlistId, limit)
            firstSongIdsCache[playlistId] = ids
            onResult(ids)
        }
    }

    /** 歌单前 N 首完整歌曲（自动封面拼图用，回调形式） */
    private val firstSongsCache = HashMap<Long, List<Song>>()
    fun getFirstSongs(playlistId: Long, limit: Int = 3, onResult: (List<Song>) -> Unit) {
        firstSongsCache[playlistId]?.let { onResult(it); return }
        viewModelScope.launch {
            val songs = repository.getFirstSongs(playlistId, limit)
            firstSongsCache[playlistId] = songs
            onResult(songs)
        }
    }

    /** 歌单内歌曲快照（封面"从歌单选"用，回调形式） */
    fun getSongsSnapshot(playlistId: Long, onResult: (List<Song>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getSongsSnapshot(playlistId, 0))
        }
    }

    // ===== 导出 / 导入 =====

    /** 导出 M3U 内容（回调形式） */
    fun exportPlaylistM3U(playlistId: Long, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val playlist = repository.getPlaylist(playlistId) ?: return@launch
            val songs = repository.getSongsSnapshot(playlistId, playlist.sortType)
            onResult(cn.lemondrop.fhreborn.data.playlist.PlaylistExporter.exportM3U(songs))
        }
    }

    /** 导出 JSON 内容（回调形式） */
    fun exportPlaylistJson(playlistId: Long, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val playlist = repository.getPlaylist(playlistId) ?: return@launch
            val songs = repository.getSongsSnapshot(playlistId, playlist.sortType)
            onResult(cn.lemondrop.fhreborn.data.playlist.PlaylistExporter.exportJson(playlist, songs))
        }
    }

    /**
     * 导入歌单（JSON 或 M3U）。匹配到的歌曲加入新歌单。
     * @param suggestedName 建议歌单名（文件名）
     * @param content 文件内容
     * @param isJson JSON 或 M3U
     * @param onResult 导入完成（成功返回 true）
     */
    fun importPlaylist(
        suggestedName: String,
        content: String,
        isJson: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val librarySongs = songDao.getAllSongsSnapshot()
            val parsed: Pair<String?, List<Song>> = if (isJson) {
                cn.lemondrop.fhreborn.data.playlist.PlaylistExporter.parseJson(content, librarySongs)
            } else {
                null to cn.lemondrop.fhreborn.data.playlist.PlaylistExporter.parseM3U(content, librarySongs)
            }
            val name = parsed.first?.takeIf { it.isNotBlank() }
                ?: suggestedName.substringBeforeLast('.').trim().ifBlank { "导入的歌单" }
            val songs = parsed.second

            if (songs.isEmpty()) {
                onResult(false)
                return@launch
            }
            val playlistId = repository.createPlaylist(name, null)
            repository.addSongs(playlistId, songs.map { it.id })
            onResult(true)
        }
    }

    /** 播放歌单（快照 + 应用默认播放模式） */
    fun playPlaylist(
        playlistId: Long,
        playerViewModel: PlayerViewModel,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val playlist = repository.getPlaylist(playlistId) ?: return@launch
            val songs = repository.getSongsSnapshot(playlistId, playlist.sortType)
            if (songs.isNotEmpty()) {
                repository.incrementPlayCount(playlistId)
                playerViewModel.playPlaylistSongs(songs, 0, playlist.defaultPlayMode)
            }
            onDone?.invoke()
        }
    }

    /** 歌单内歌曲总时长（回调形式） */
    fun getPlaylistTotalDuration(playlistId: Long, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getTotalDuration(playlistId))
        }
    }

    /** 播放次数 +1（从歌单内点歌播放时调用） */
    fun recordPlay(playlistId: Long) {
        viewModelScope.launch {
            repository.incrementPlayCount(playlistId)
        }
    }

    /** 拖拽重排歌单内歌曲（新顺序的 songId 列表） */
    fun reorderSongs(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            repository.reorderSongs(playlistId, songIds)
        }
    }

    fun createPlaylist(name: String, description: String?, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name.trim(), description?.trim()?.takeIf { it.isNotEmpty() })
            onDone(id)
        }
    }

    fun updatePlaylist(id: Long, name: String, description: String?, defaultPlayMode: Int) {
        viewModelScope.launch {
            repository.updatePlaylist(id, name.trim(), description?.trim()?.takeIf { it.isNotEmpty() }, defaultPlayMode)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addSong(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSong(playlistId, songId)
        }
    }

    /** 批量添加歌曲到歌单（忽略已存在） */
    fun addSongs(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            repository.addSongs(playlistId, songIds)
        }
    }

    /** 包含指定歌曲的歌单 id 列表（用于"已加入"标记） */
    fun getPlaylistIdsContainingSong(songId: Long, onResult: (Set<Long>) -> Unit = {}) {
        viewModelScope.launch {
            onResult(repository.getPlaylistIdsContainingSong(songId).toSet())
        }
    }

    fun removeSong(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSong(playlistId, songId)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlaylistViewModel(application) as T
        }
    }
}
