package cn.lemondrop.fhreborn.scanner

import android.content.Context
import cn.lemondrop.fhreborn.data.db.entity.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class ScanProgress {
    data object Idle : ScanProgress()
    data object Scanning : ScanProgress()
    data class Progress(
        val current: Int,
        val total: Int,
        val path: String,
        /** 最近扫描到的若干条目，用于在对话框中展示实时扫描列表。 */
        val recentlyScanned: List<String> = emptyList()
    ) : ScanProgress()

    data class Completed(
        val songsFound: Int,
        val songs: List<Song> = emptyList(),
        /** 本次刷新从媒体库移除的歌曲数。 */
        val removed: Int = 0
    ) : ScanProgress()

    data class Error(val message: String) : ScanProgress()
}

class MediaScanner(context: Context) {

    private val mediaStoreScanner = MediaStoreScanner(context)

    fun scan(quickScan: Boolean = false): Flow<ScanProgress> = flow {
        emit(ScanProgress.Scanning)
        try {
            val allSongs = mutableListOf<Song>()

            // 仅读取系统 MediaStore 媒体库
            val mediaStoreSongs = mediaStoreScanner.scan(emptySet())
            mediaStoreSongs.forEachIndexed { index, song ->
                allSongs.add(song)
                emit(
                    ScanProgress.Progress(
                        current = index + 1,
                        total = mediaStoreSongs.size,
                        path = song.path,
                        recentlyScanned = allSongs.takeLast(6).map { it.title }
                    )
                )
            }

            emit(ScanProgress.Completed(allSongs.size, allSongs))
        } catch (e: Exception) {
            emit(ScanProgress.Error(e.message ?: "Unknown scan error"))
        }
    }
}
