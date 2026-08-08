package cn.lemondrop.fhreborn.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cn.lemondrop.fhreborn.data.db.entity.Song
import java.io.File

object SongFileUtils {

    fun shareSong(context: Context, song: Song?, chooserTitle: String = "分享音频") {
        song ?: return
        val file = File(song.path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun openWithOtherApp(context: Context, song: Song?, chooserTitle: String = "用其他 app 打开") {
        song ?: return
        val file = File(song.path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    /** 批量分享多首歌曲（ACTION_SEND_MULTIPLE），跳过不存在的文件 */
    fun shareSongs(context: Context, songs: List<Song>, chooserTitle: String = "分享音频") {
        val uris = songs.mapNotNull { song ->
            val file = File(song.path)
            if (!file.exists()) null
            else FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
        if (uris.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
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
        val durationText = formatDuration(song.duration)
        val message = buildString {
            appendLine("标题：${song.title}")
            appendLine("艺术家：${song.artist}")
            appendLine("专辑：${song.album}")
            appendLine("时长：$durationText")
            appendLine("格式：${song.format}")
            appendLine("路径：${song.path}")
            appendLine("大小：${formatFileSize(song.fileSize)}")
            appendLine("修改时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(song.modifiedAt))}")
            if (song.bitrate != null) appendLine("比特率：${song.bitrate} kbps")
            if (song.sampleRate != null) appendLine("采样率：${song.sampleRate} Hz")
            if (song.channels != null) appendLine("声道：${song.channels}")
            if (song.year != null) appendLine("年份：${song.year}")
            if (song.discNumber != null) appendLine("碟号：${song.discNumber}")
            if (song.trackNumber != null) appendLine("音轨号：${song.trackNumber}")
            if (!file.exists()) appendLine("状态：文件不存在")
        }
        FhBottomSheet(
            show = true,
            title = "歌曲属性",
            onDismissRequest = onDismiss,
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
            content = {
                Text(text = message)
                TextButton(text = "确定", onClick = onDismiss)
            }
        )
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(minutes, secs)
    }

    @Composable
    fun ShowSongProperties(song: Song?, onDismiss: () -> Unit) {
        if (song != null) {
            SongPropertiesDialog(song = song, onDismiss = onDismiss)
        }
    }
}
