package cn.lemondrop.fhreborn.data.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import cn.lemondrop.fhreborn.data.db.entity.Song
import java.io.File

/**
 * 歌词读取器
 *
 * 读取优先级：
 * 1. 同名 .lrc 文件（与音乐文件同目录、同名）
 * 2. 音乐文件内嵌歌词（USLT / VorbisComment LYRICS）
 */
object LyricReader {

    fun readLyrics(context: Context, song: Song): String? {
        // 1. 尝试读取同名 .lrc 文件
        val fileLyrics = readFromFile(song)
        if (!fileLyrics.isNullOrBlank()) return fileLyrics

        // 2. 尝试从音乐标签读取内嵌歌词
        return readFromMetadata(context, song)
    }

    private fun readFromFile(song: Song): String? {
        return try {
            val musicFile = File(song.path)
            if (!musicFile.exists()) return null
            val baseName = musicFile.nameWithoutExtension
            val parent = musicFile.parentFile

            // 优先尝试 .ttml（逐字歌词），再尝试 .lrc
            val ttmlFile = File(parent, "$baseName.ttml")
            if (ttmlFile.exists()) {
                return ttmlFile.readText(Charsets.UTF_8)
            }

            val lrcFile = File(parent, "$baseName.lrc")
            if (lrcFile.exists()) {
                return lrcFile.readText(Charsets.UTF_8)
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun readFromMetadata(context: Context, song: Song): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(
                context,
                Uri.parse("content://media/external/audio/media/${song.id}")
            )
            retriever.extractMetadata(22) // METADATA_KEY_LYRICS = 22
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}
