package cn.lemondrop.fhreborn.ui.screens.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.lyrics.LyricFormatType
import cn.lemondrop.fhreborn.data.lyrics.LyricSource
import cn.lemondrop.fhreborn.data.lyrics.LyricSourceType
import top.yukonga.miuix.kmp.basic.Text
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet

@Composable
fun LyricInfoSheet(
    lyricSource: LyricSource?,
    onDismiss: () -> Unit
) {
    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = "歌词信息",
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (lyricSource == null || lyricSource.source == LyricSourceType.NONE) {
                InfoRow(label = "状态", value = "无歌词")
            } else {
                InfoRow(
                    label = "来源",
                    value = when (lyricSource.source) {
                        LyricSourceType.EMBEDDED -> "内嵌标签"
                        LyricSourceType.EXTERNAL_FILE -> "外部文件"
                        else -> "未知"
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(
                    label = "格式",
                    value = when (lyricSource.format) {
                        LyricFormatType.LRC -> "LRC"
                        LyricFormatType.TTML -> "TTML"
                        LyricFormatType.PLAIN -> "纯文本"
                        else -> "未知"
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(
                    label = "逐字歌词",
                    value = if (lyricSource.rawText != null && isWordLevelLyric(lyricSource)) "是" else "否"
                )
                lyricSource.filePath?.let { path ->
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "文件路径", value = path)
                }
                lyricSource.rawText?.let { text ->
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "字符数", value = "${text.length}")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun isWordLevelLyric(source: LyricSource): Boolean {
    val text = source.rawText ?: return false
    return when (source.format) {
        LyricFormatType.TTML -> text.contains("<span") && text.contains("begin=")
        LyricFormatType.LRC -> text.contains(Regex("<\\d{2}:\\d{2}"))
        else -> false
    }
}
