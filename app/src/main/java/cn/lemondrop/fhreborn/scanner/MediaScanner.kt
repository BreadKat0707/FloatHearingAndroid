package cn.lemondrop.fhreborn.scanner

import android.content.Context
import cn.lemondrop.fhreborn.data.db.entity.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class ScanProgress {
    data object Idle : ScanProgress()
    data object Scanning : ScanProgress()
    data class Progress(val current: Int, val total: Int, val path: String) : ScanProgress()
    data class Completed(val songsFound: Int, val songs: List<Song> = emptyList()) : ScanProgress()
    data class Error(val message: String) : ScanProgress()
}

class MediaScanner(context: Context) {

    private val mediaStoreScanner = MediaStoreScanner(context)
    private val ffmpegScanner = FFmpegScanner(context)

    fun scan(): Flow<ScanProgress> = flow {
        emit(ScanProgress.Scanning)
        try {
            val allSongs = mutableListOf<Song>()
            val scannedPaths = mutableSetOf<String>()

            // Step 1: MediaStore 快速索引
            emit(ScanProgress.Progress(1, 2, "MediaStore"))
            val mediaStoreSongs = mediaStoreScanner.scan(scannedPaths)
            allSongs.addAll(mediaStoreSongs)
            scannedPaths.addAll(mediaStoreSongs.map { it.path })

            // Step 2: FFmpeg 补充扫描（扫描外部存储常见音乐目录）
            emit(ScanProgress.Progress(2, 2, "FFmpeg"))
            val commonMusicDirs = listOf(
                "/storage/emulated/0/Music",
                "/storage/emulated/0/Download",
                "/storage/emulated/0/"
            ).filter { java.io.File(it).exists() }
            val ffmpegSongs = ffmpegScanner.scan(commonMusicDirs, scannedPaths)
            allSongs.addAll(ffmpegSongs)

            emit(ScanProgress.Completed(allSongs.size, allSongs))
        } catch (e: Exception) {
            emit(ScanProgress.Error(e.message ?: "Unknown scan error"))
        }
    }
}
