package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.ui.theme.LocalAppDarkTheme
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 多选模式底部工具栏：已选计数 + 加入歌单 / 加入播放队列 / 分享 / 删除 / 退出。
 * 媒体库中直接替换底部导航栏（Scaffold bottomBar）；无底栏页面用 Box 悬浮显示。
 */
@Composable
fun MultiSelectToolbar(
    selectedCount: Int,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    showSongActions: Boolean = true
) {
    val enabled = selectedCount > 0
    val iconColor = if (enabled) {
        MiuixTheme.colorScheme.onSurface
    } else {
        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已选 $selectedCount",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        if (showSongActions) {
            IconButton(onClick = onAddToPlaylist, enabled = enabled) {
                Icon(
                    imageVector = Lucide.FolderPlus,
                    contentDescription = "加入歌单",
                    modifier = Modifier.size(22.dp),
                    tint = iconColor
                )
            }
            IconButton(onClick = onAddToQueue, enabled = enabled) {
                Icon(
                    imageVector = Lucide.ListMusic,
                    contentDescription = "加入播放队列",
                    modifier = Modifier.size(22.dp),
                    tint = iconColor
                )
            }
            IconButton(onClick = onShare, enabled = enabled) {
                Icon(
                    imageVector = Lucide.Share2,
                    contentDescription = "分享",
                    modifier = Modifier.size(22.dp),
                    tint = iconColor
                )
            }
        }
        IconButton(onClick = onDelete, enabled = enabled) {
            Icon(
                imageVector = Lucide.Trash2,
                contentDescription = "删除",
                modifier = Modifier.size(22.dp),
                tint = if (enabled) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        IconButton(onClick = onExit) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = "退出多选",
                modifier = Modifier.size(22.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}
