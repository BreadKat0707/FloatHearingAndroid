package cn.lemondrop.fhreborn.util

import android.content.Context
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.InfoRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

object SongFileUtils {

    fun shareSong(context: Context, song: Song?, chooserTitle: String = "分享音频") {
        song ?: return
        val uri = song.toShareUri(context) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun openWithOtherApp(context: Context, song: Song?, chooserTitle: String = "用其他 app 打开") {
        song ?: return
        val uri = song.toShareUri(context) ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    /** 批量分享多首歌曲（ACTION_SEND_MULTIPLE），跳过不存在的文件 */
    fun shareSongs(context: Context, songs: List<Song>, chooserTitle: String = "分享音频") {
        val uris = songs.mapNotNull { song ->
            song.toShareUri(context)
        }
        if (uris.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    private fun Song.toShareUri(context: Context): Uri? {
        return if (source == Song.SOURCE_DIRECTORY) {
            val file = File(path)
            if (!file.exists()) return null
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            android.content.ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id
            )
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "${bytes} B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.2f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.2f MB".format(mb)
        val gb = mb / 1024.0
        return "%.2f GB".format(gb)
    }

    @Composable
    fun SongPropertiesDialog(song: Song?, onDismiss: () -> Unit) {
        song ?: return
        val file = File(song.path)
        val context = LocalContext.current
        val db = remember { AppDatabase.getInstance(context) }

        // 音频流信息（编码方式/比特率/采样率/位深），异步读取
        var streamInfo by remember { mutableStateOf<AudioStreamInfo?>(null) }
        LaunchedEffect(song.id, song.path) {
            streamInfo = withContext(Dispatchers.IO) {
                readAudioStreamInfo(song.path)
            }
        }
        // 播放次数
        var playCount by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(song.id) {
            playCount = withContext(Dispatchers.IO) {
                db.playRecordDao().getPlayCountBySong(song.id)
            }
        }

        val durationText = formatDurationMs(song.duration)
        val streamBitrate = streamInfo?.bitrate
        val streamSampleRate = streamInfo?.sampleRate
        val bitDepth = streamInfo?.bitDepth
        val codec = streamInfo?.mime?.let { codecName(it) } ?: song.format

        FhBottomSheet(
            show = true,
            title = "歌曲属性",
            onDismissRequest = onDismiss,
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    InfoRow(label = "标题", value = song.title, selectable = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "艺术家", value = song.artist, selectable = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "专辑", value = song.album, selectable = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "编码方式", value = codec ?: "未知", selectable = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "时长", value = durationText, selectable = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        label = "比特率",
                        value = streamBitrate?.let { formatBitrate(it) }
                            ?: song.bitrate?.let { formatBitrate(it) }
                            ?: "未知",
                        selectable = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        label = "采样率",
                        value = (streamSampleRate ?: song.sampleRate)?.let { "$it Hz" } ?: "未知",
                        selectable = true
                    )
                    if (bitDepth != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "位深", value = "$bitDepth bit", selectable = true)
                    }
                    song.channels?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "声道", value = "$it", selectable = true)
                    }
                    playCount?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "播放次数", value = "$it 次", selectable = true)
                    }
                    song.year?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "年份", value = "$it", selectable = true)
                    }
                    song.discNumber?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "碟号", value = "$it", selectable = true)
                    }
                    song.trackNumber?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "音轨号", value = "$it", selectable = true)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "路径", value = song.path, selectable = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        label = "大小",
                        value = formatFileSize(song.fileSize),
                        selectable = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        label = "修改时间",
                        value = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(song.modifiedAt)),
                        selectable = true
                    )
                    if (!file.exists()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "状态", value = "文件不存在", selectable = true)
                    }
                }
            }
        )
    }

    /** 音频流信息：编码方式 / 比特率(bps) / 采样率 / 位深 */
    private data class AudioStreamInfo(
        val mime: String?,
        val bitrate: Int?,
        val sampleRate: Int?,
        val bitDepth: Int?
    )

    /** 用 MediaExtractor 读取音频轨道的流信息（异步调用） */
    private fun readAudioStreamInfo(path: String): AudioStreamInfo? {
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
                val bitrate = if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    format.getInteger(MediaFormat.KEY_BIT_RATE)
                } else null
                // 位深：FLAC 等提供 "bit-depth"，PCM 系用 pcm-encoding 换算
                val bitDepth = format.getString("bit-depth")?.toIntOrNull()
                    ?: pcmEncodingToBits(
                        if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        } else 0
                    )
                return AudioStreamInfo(mime, bitrate, sampleRate, bitDepth)
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    private fun pcmEncodingToBits(encoding: Int): Int? = when (encoding) {
        android.media.AudioFormat.ENCODING_PCM_16BIT -> 16
        android.media.AudioFormat.ENCODING_PCM_8BIT -> 8
        android.media.AudioFormat.ENCODING_PCM_FLOAT -> 32
        android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
        android.media.AudioFormat.ENCODING_PCM_32BIT -> 32
        else -> null
    }

    /** MIME → 友好编码名 */
    private fun codecName(mime: String): String? = when (mime) {
        "audio/flac" -> "FLAC"
        "audio/mpeg" -> "MP3"
        "audio/mp4a-latm" -> "AAC"
        "audio/mp4" -> "M4A"
        "audio/alac", "audio/x-alac" -> "ALAC"
        "audio/ogg" -> "OGG"
        "audio/opus" -> "Opus"
        "audio/vorbis" -> "Vorbis"
        "audio/wav", "audio/raw" -> "WAV（PCM）"
        "audio/ape", "audio/x-ape" -> "APE"
        "audio/x-wavpack" -> "WavPack"
        "audio/amr" -> "AMR"
        "audio/mpeg-L1" -> "MP1"
        "audio/mpeg-L2" -> "MP2"
        else -> mime.substringAfter('/')
    }

    /** 时长（毫秒精度）：m:ss.mmm */
    private fun formatDurationMs(ms: Long): String {
        val minutes = ms / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return "%d:%02d.%03d".format(minutes, seconds, millis)
    }

    /**
     * 比特率格式化：单位是 bps（如 897446 bps ≈ 897 kbps）。
     */
    private fun formatBitrate(bps: Int): String = when {
        bps >= 1_000_000 -> String.format("%.1f Mbps", bps / 1_000_000f)
        else -> "${bps / 1000} kbps"
    }

    @Composable
    fun ShowSongProperties(song: Song?, onDismiss: () -> Unit) {
        if (song != null) {
            SongPropertiesDialog(song = song, onDismiss = onDismiss)
        }
    }
}
