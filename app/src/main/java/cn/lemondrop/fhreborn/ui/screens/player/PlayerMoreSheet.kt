package cn.lemondrop.fhreborn.ui.screens.player

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cn.lemondrop.clover.CloverBottomSheet
import cn.lemondrop.clover.CloverMenuItem
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Volume2

private data class MoreMenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun PlayerMoreSheet(
    onDismiss: () -> Unit,
    onTimerClick: () -> Unit = {},
    onAudioOutputClick: () -> Unit = {},
    onAddToPlaylistClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    val menuItems = listOf(
        MoreMenuItem("计划暂停", Lucide.Timer, onTimerClick),
        MoreMenuItem("音频输出", Lucide.Volume2, onAudioOutputClick),
        MoreMenuItem("添加到歌单", Lucide.FolderPlus, onAddToPlaylistClick),
        MoreMenuItem("倍速", Lucide.Gauge, onSpeedClick),
        MoreMenuItem("分享", Lucide.Share2, onShareClick)
    )

    CloverBottomSheet(
        onDismiss = onDismiss,
        title = "更多"
    ) {
        LazyColumn {
            items(menuItems, key = { it.label }) { item ->
                CloverMenuItem(
                    label = item.label,
                    icon = item.icon,
                    onClick = {
                        item.onClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}
