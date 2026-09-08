package cn.lemondrop.fhreborn.data.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import cn.lemondrop.fhreborn.data.db.entity.Song
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

/**
 * 歌词读取器
 *
 * 读取优先级：
 * 1. 同名 .lrc 文件（与音乐文件同目录、同名）
 * 2. 音乐文件内嵌歌词（USLT / VorbisComment LYRICS）
 */
object LyricReader {

    fun readLyrics(context: Context, song: Song): LyricSource {
        // 1. 尝试读取同名 .ttml 文件
        val ttmlFileLyrics = readFromTtmlFile(song)
        if (ttmlFileLyrics != null) {
            return LyricSource(
                rawText = ttmlFileLyrics,
                source = LyricSourceType.EXTERNAL_FILE,
                format = LyricFormatType.TTML,
                filePath = song.path.let { File(it).parentFile?.let { dir ->
                    File(dir, File(it).nameWithoutExtension + ".ttml").absolutePath
                }}
            )
        }

        // 2. 尝试读取同名 .lrc 文件
        val lrcFileLyrics = readFromLrcFile(song)
        if (lrcFileLyrics != null) {
            return LyricSource(
                rawText = lrcFileLyrics,
                source = LyricSourceType.EXTERNAL_FILE,
                format = LyricFormatType.LRC,
                filePath = song.path.let { File(it).parentFile?.let { dir ->
                    File(dir, File(it).nameWithoutExtension + ".lrc").absolutePath
                }}
            )
        }

        // 3. 尝试从音乐标签读取内嵌歌词
        val embeddedLyrics = readFromMetadata(context, song)
        if (!embeddedLyrics.isNullOrBlank()) {
            val format = detectFormat(embeddedLyrics)
            return LyricSource(
                rawText = embeddedLyrics,
                source = LyricSourceType.EMBEDDED,
                format = format,
                filePath = null
            )
        }

        return LyricSource(
            rawText = null,
            source = LyricSourceType.NONE,
            format = LyricFormatType.NONE,
            filePath = null
        )
    }

    private fun readFromTtmlFile(song: Song): String? {
        return try {
            val musicFile = File(song.path)
            if (!musicFile.exists()) return null
            val ttmlFile = File(musicFile.parentFile, musicFile.nameWithoutExtension + ".ttml")
            if (ttmlFile.exists()) ttmlFile.readText(Charsets.UTF_8) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun readFromLrcFile(song: Song): String? {
        return try {
            val musicFile = File(song.path)
            if (!musicFile.exists()) return null
            val lrcFile = File(musicFile.parentFile, musicFile.nameWithoutExtension + ".lrc")
            if (lrcFile.exists()) lrcFile.readText(Charsets.UTF_8) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun readFromMetadata(context: Context, song: Song): String? {
        // 优先使用 jaudiotagger 读取 USLT / VorbisComment LYRICS / APE 等内嵌歌词
        try {
            val file = File(song.path)
            if (file.exists()) {
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tagOrCreateDefault
                val lyrics = tag.getFirst(FieldKey.LYRICS)
                if (!lyrics.isNullOrBlank()) return lyrics
            }
        } catch (_: Exception) {
            // 回退到 MediaMetadataRetriever
        }

        val retriever = MediaMetadataRetriever()
        return try {
            try {
                retriever.setDataSource(song.path)
            } catch (_: Exception) {
                val fallbackUri = if (song.source == cn.lemondrop.fhreborn.data.db.entity.Song.SOURCE_DIRECTORY) {
                    Uri.fromFile(File(song.path))
                } else {
                    Uri.parse("content://media/external/audio/media/${song.id}")
                }
                retriever.setDataSource(context, fallbackUri)
            }
            retriever.extractMetadata(22) // MediaMetadataRetriever.METADATA_KEY_LYRICS
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun detectFormat(content: String): LyricFormatType {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("<?xml") || trimmed.startsWith("<tt") -> LyricFormatType.TTML
            trimmed.contains("[") && trimmed.contains("]") -> LyricFormatType.LRC
            else -> LyricFormatType.PLAIN
        }
    }
}

enum class LyricSourceType {
    EMBEDDED,
    EXTERNAL_FILE,
    NONE
}

enum class LyricFormatType {
    LRC,
    TTML,
    PLAIN,
    NONE
}

data class LyricSource(
    val rawText: String?,
    val source: LyricSourceType,
    val format: LyricFormatType,
    val filePath: String?
)
