package cn.lemondrop.fhreborn.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.InfoRow
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
                    InfoRow(label = "标题", value = song.title)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "艺术家", value = song.artist)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "专辑", value = song.album)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "时长", value = durationText)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "格式", value = song.format)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "路径", value = song.path)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        label = "大小",
                        value = formatFileSize(song.fileSize)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        label = "修改时间",
                        value = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(song.modifiedAt))
                    )
                    song.bitrate?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "比特率", value = "$it kbps")
                    }
                    song.sampleRate?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "采样率", value = "$it Hz")
                    }
                    song.channels?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "声道", value = "$it")
                    }
                    song.year?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "年份", value = "$it")
                    }
                    song.discNumber?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "碟号", value = "$it")
                    }
                    song.trackNumber?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "音轨号", value = "$it")
                    }
                    if (!file.exists()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "状态", value = "文件不存在")
                    }
                }
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
