package cn.lemondrop.fhreborn.scanner

import android.content.Context
import android.util.Log
import cn.lemondrop.fhreborn.data.db.entity.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * FFmpeg 补充扫描器 — 用于扫描 MediaStore 不支持的格式（APE、DSD 等）。
 *
 * TODO: 集成 ffmpeg-kit-min 后实现完整解析逻辑。
 * 当前版本为框架占位，使用文件遍历 + 基础元数据解析。
 */
class FFmpegScanner(private val context: Context) {

    companion object {
        private const val TAG = "FFmpegScanner"
        private val FFMPEG_FORMATS = setOf("ape", "dsf", "dff", "wv", "mpc", "tta")
    }

    /**
     * 扫描指定目录中 MediaStore 未覆盖的音频文件。
     */
    suspend fun scan(
        directoryPaths: List<String>,
        existingPaths: Set<String>
    ): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        for (dirPath in directoryPaths) {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                Log.w(TAG, "Directory does not exist: $dirPath")
                continue
            }

            dir.walkTopDown()
                .filter { it.isFile }
                .filter { isFFmpegFormat(it.name) }
                .filter { it.absolutePath !in existingPaths }
                .forEach { file ->
                    val song = parseFile(file)
                    if (song != null) {
                        songs.add(song)
                    }
                }
        }

        Log.d(TAG, "FFmpeg scan complete, found ${songs.size} songs")
        songs
    }

    private fun isFFmpegFormat(fileName: String): Boolean {
        return fileName.substringAfterLast('.', "").lowercase() in FFMPEG_FORMATS
    }

    /**
     * 基础文件元数据解析（待 FFmpeg 集成后替换为完整实现）。
     */
    private fun parseFile(file: File): Song? {
        return try {
            Song(
                id = file.absolutePath.hashCode().toLong(),
                title = file.nameWithoutExtension,
                artist = "Unknown Artist",
                album = "Unknown Album",
                path = file.absolutePath,
                duration = 0,
                format = file.extension.uppercase(),
                fileSize = file.length(),
                year = null,
                discNumber = null,
                trackNumber = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ${file.absolutePath}", e)
            null
        }
    }

    fun isAvailable(): Boolean {
        // TODO: 集成 ffmpeg-kit 后返回真实状态
        return false
    }
}
