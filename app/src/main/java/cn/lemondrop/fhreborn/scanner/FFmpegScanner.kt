package cn.lemondrop.fhreborn.scanner

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.util.PathUtils
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

/**
 * Directory scanner for custom folders.
 *
 * The class keeps the historical FFmpegScanner name for compatibility, but the
 * metadata probe uses Android MediaMetadataRetriever/MediaExtractor plus
 * JAudioTagger so it is available without shipping a separate FFmpeg binary.
 */
class FFmpegScanner {

    companion object {
        private const val TAG = "FFmpegScanner"
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "m4a", "m4b", "aac", "ogg", "oga", "opus",
            "wav", "wma", "alac", "ape", "dsf", "dff", "wv", "mpc", "tta"
        )
    }

    suspend fun scan(
        directoryPaths: List<String>,
        filter: ScanFilterConfig,
        onFound: suspend (Song) -> Unit = {}
    ): List<Song> {
        val songs = mutableListOf<Song>()
        val seenPaths = mutableSetOf<String>()

        for (dirPath in directoryPaths) {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                Log.w(TAG, "Directory does not exist: $dirPath")
                continue
            }

            dir.walkTopDown()
                .filter { it.isFile }
                .filter { it.extension.lowercase() in AUDIO_EXTENSIONS }
                .filter { !shouldSkip(it.absolutePath) }
                .forEach { file ->
                    val canonicalPath = runCatching { file.canonicalPath }
                        .getOrDefault(file.absolutePath)
                    if (!seenPaths.add(canonicalPath)) return@forEach
                    val song = parseFile(file, canonicalPath, filter) ?: return@forEach
                    songs.add(song)
                    onFound(song)
                }
        }

        Log.d(TAG, "Directory scan complete, found ${songs.size} songs")
        return songs
    }

    fun isAvailable(): Boolean {
        return try {
            Class.forName("android.media.MediaMetadataRetriever")
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun shouldSkip(path: String): Boolean {
        return path.contains("/Android/data/") ||
            path.contains("/Android/obb/") ||
            path.contains("/.nomedia")
    }

    private fun parseFile(file: File, canonicalPath: String, filter: ScanFilterConfig): Song? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
        } catch (_: Exception) {
            retriever.release()
            return parseWithTagsOnly(file, canonicalPath, filter)
        }

        return try {
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            if (!filter.acceptsDuration(durationMs)) return null

            val title = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: file.nameWithoutExtension
            val artist = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: "Unknown Artist"
            val album = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
                ?: "Unknown Album"
            val albumArtist = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?.takeIf { it.isNotBlank() }
            val year = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val bitrate = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
            val streamInfo = readStreamInfo(file.absolutePath)

            Song(
                id = PathUtils.stableDirectorySongId(canonicalPath),
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                path = canonicalPath,
                duration = durationMs,
                format = formatFromMime(mime, file),
                source = Song.SOURCE_DIRECTORY,
                bitrate = bitrate ?: streamInfo?.bitrate,
                sampleRate = streamInfo?.sampleRate,
                channels = streamInfo?.channels,
                fileSize = file.length(),
                modifiedAt = file.lastModified(),
                year = year
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ${file.absolutePath}", e)
            null
        } finally {
            retriever.release()
        }
    }

    private fun parseWithTagsOnly(file: File, canonicalPath: String, filter: ScanFilterConfig): Song? {
        return try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            val durationMs = audioFile.audioHeader?.trackLength?.let { it * 1000L } ?: 0L
            if (!filter.acceptsDuration(durationMs)) return null
            val title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() }
                ?: file.nameWithoutExtension
            val artist = tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() }
                ?: "Unknown Artist"
            val album = tag?.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() }
                ?: "Unknown Album"
            val year = tag?.getFirst(FieldKey.YEAR)?.toIntOrNull()?.takeIf { it > 0 }
            Song(
                id = PathUtils.stableDirectorySongId(canonicalPath),
                title = title,
                artist = artist,
                album = album,
                albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST)?.takeIf { it.isNotBlank() },
                path = canonicalPath,
                duration = durationMs,
                format = file.extension.uppercase(),
                source = Song.SOURCE_DIRECTORY,
                fileSize = file.length(),
                modifiedAt = file.lastModified(),
                year = year
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun readStreamInfo(path: String): StreamInfo? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(path)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("audio/")) continue
                val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else null
                val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else null
                val bitrate = if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    format.getInteger(MediaFormat.KEY_BIT_RATE)
                } else null
                return StreamInfo(sampleRate, channels, bitrate)
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    private fun formatFromMime(mime: String?, file: File): String {
        if (mime.isNullOrBlank()) return file.extension.uppercase().ifEmpty { "UNKNOWN" }
        return when {
            mime.contains("flac") -> "FLAC"
            mime.contains("mpeg") && !mime.contains("mp4") -> "MP3"
            mime.contains("mp4") || mime.contains("m4a") -> "M4A"
            mime.contains("alac") -> "ALAC"
            mime.contains("ogg") || mime.contains("opus") -> if (mime.contains("opus")) "OPUS" else "OGG"
            mime.contains("wav") || mime.contains("raw") -> "WAV"
            mime.contains("aac") -> "AAC"
            else -> file.extension.uppercase().ifEmpty { "UNKNOWN" }
        }
    }

    private data class StreamInfo(
        val sampleRate: Int?,
        val channels: Int?,
        val bitrate: Int?
    )
}
