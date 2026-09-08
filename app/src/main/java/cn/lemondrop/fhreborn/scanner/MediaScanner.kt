package cn.lemondrop.fhreborn.scanner

import android.content.Context
import cn.lemondrop.fhreborn.data.db.entity.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

sealed class ScanProgress {
    data object Idle : ScanProgress()
    data object Scanning : ScanProgress()
    data class Progress(
        val current: Int,
        val total: Int,
        val path: String,
        val recentlyScanned: List<String> = emptyList()
    ) : ScanProgress()

    data class Completed(
        val songsFound: Int,
        val songs: List<Song> = emptyList(),
        val removed: Int = 0
    ) : ScanProgress()

    data class Error(val message: String) : ScanProgress()
}

object ScanSourceMode {
    const val MEDIA_STORE = "media_store"
    const val DIRECTORY = "directory"

    fun toSongSource(mode: String): Int =
        if (mode == DIRECTORY) Song.SOURCE_DIRECTORY else Song.SOURCE_MEDIA_STORE
}

data class ScanFilterConfig(
    val durationFilterEnabled: Boolean = false,
    val durationSeconds: Int = 30
) {
    val minimumDurationMs: Long
        get() = durationSeconds.coerceIn(10, 60) * 1000L

    fun acceptsDuration(durationMs: Long?): Boolean {
        if (!durationFilterEnabled) return true
        val duration = durationMs ?: return false
        return duration > 0L && duration >= minimumDurationMs
    }
}

class MediaScanner(context: Context) {

    private val mediaStoreScanner = MediaStoreScanner(context)
    private val ffmpegScanner = FFmpegScanner()

    fun scan(
        mode: String = ScanSourceMode.MEDIA_STORE,
        filter: ScanFilterConfig = ScanFilterConfig(),
        directoryPaths: List<String> = emptyList(),
        quickScan: Boolean = false
    ): Flow<ScanProgress> = flow {
        emit(ScanProgress.Scanning)
        try {
            val source = ScanSourceMode.toSongSource(mode)
            val songs = when (mode) {
                ScanSourceMode.DIRECTORY -> {
                    if (directoryPaths.isEmpty()) {
                        emit(ScanProgress.Error("No active scan directories"))
                        return@flow
                    }
                    scanDirectories(directoryPaths, filter)
                }
                else -> scanMediaStore(filter)
            }
            emit(
                ScanProgress.Completed(
                    songsFound = songs.size,
                    songs = songs.map { it.copy(source = source) }
                )
            )
        } catch (e: Exception) {
            emit(ScanProgress.Error(e.message ?: "Unknown scan error"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<ScanProgress>.scanMediaStore(
        filter: ScanFilterConfig
    ): List<Song> {
        val found = mutableListOf<Song>()
        val songs = mediaStoreScanner.scan(filter) { song ->
            found.add(song)
            emit(
                ScanProgress.Progress(
                    current = found.size,
                    total = 0,
                    path = song.path,
                    recentlyScanned = found.takeLast(6).map { it.title }
                )
            )
        }
        return songs
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<ScanProgress>.scanDirectories(
        directoryPaths: List<String>,
        filter: ScanFilterConfig
    ): List<Song> {
        val found = mutableListOf<Song>()
        val songs = ffmpegScanner.scan(directoryPaths, filter) { song ->
            found.add(song)
            emit(
                ScanProgress.Progress(
                    current = found.size,
                    total = 0,
                    path = song.path,
                    recentlyScanned = found.takeLast(6).map { it.title }
                )
            )
        }
        return songs
    }
}
