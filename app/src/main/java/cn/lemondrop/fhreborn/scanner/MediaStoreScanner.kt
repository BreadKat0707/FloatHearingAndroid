package cn.lemondrop.fhreborn.scanner

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import cn.lemondrop.fhreborn.data.db.entity.Song

class MediaStoreScanner(private val context: Context) {

    companion object {
        private const val TAG = "MediaStoreScanner"

        private val AUDIO_PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.BITRATE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.TRACK,
        )
    }

    suspend fun scan(
        filter: ScanFilterConfig,
        onFound: suspend (Song) -> Unit = {}
    ): List<Song> {
        val songs = mutableListOf<Song>()
        val rows = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            AUDIO_PROJECTION,
            "${MediaStore.Audio.Media.IS_MUSIC} = 1",
            null,
            MediaStore.Audio.Media.TITLE + " COLLATE NOCASE"
        )
        rows?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val bitrateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val discCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISC_NUMBER)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                if (shouldSkipPath(path)) continue
                val duration = cursor.getLong(durationCol)
                if (!filter.acceptsDuration(duration)) continue

                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: path.substringAfterLast('/')
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val albumArtist = cursor.getString(albumArtistCol)
                val size = cursor.getLong(sizeCol)
                val bitrate = cursor.getInt(bitrateCol)
                val mimeType = cursor.getString(mimeCol) ?: ""
                val modifiedAt = cursor.getLong(modifiedCol) * 1000L
                val year = cursor.getInt(yearCol).takeIf { it > 0 }

                val rawDisc = cursor.getInt(discCol).takeIf { it > 0 }
                val rawTrack = cursor.getInt(trackCol).takeIf { it > 0 }
                val parsedDisc = rawTrack?.div(1000)?.takeIf { it > 0 }
                val parsedTrack = rawTrack?.rem(1000)?.takeIf { it > 0 }
                val discNumber = rawDisc ?: parsedDisc
                val trackNumber = parsedTrack ?: rawTrack

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    path = path,
                    duration = duration,
                    format = extractFormat(mimeType, path),
                    source = Song.SOURCE_MEDIA_STORE,
                    bitrate = if (bitrate > 0) bitrate else null,
                    fileSize = size,
                    modifiedAt = modifiedAt,
                    year = year,
                    discNumber = discNumber,
                    trackNumber = trackNumber
                )
                songs.add(song)
                onFound(song)
            }
        }
        Log.d(TAG, "Scanned all MediaStore audio, found ${songs.size} songs")
        return songs
    }

    private fun shouldSkipPath(path: String): Boolean {
        return path.contains("/.nomedia") ||
            path.contains("/Android/data/") ||
            path.contains("/Android/obb/")
    }

    private fun extractFormat(mimeType: String, path: String): String {
        return when {
            mimeType.contains("flac") -> "FLAC"
            mimeType.contains("mp3") || mimeType.contains("mpeg") -> "MP3"
            mimeType.contains("mp4") || mimeType.contains("m4a") -> "M4A"
            mimeType.contains("ogg") || mimeType.contains("vorbis") -> "OGG"
            mimeType.contains("opus") -> "OPUS"
            mimeType.contains("wav") -> "WAV"
            mimeType.contains("aac") -> "AAC"
            else -> path.substringAfterLast('.', "").uppercase().ifEmpty { "UNKNOWN" }
        }
    }
}
