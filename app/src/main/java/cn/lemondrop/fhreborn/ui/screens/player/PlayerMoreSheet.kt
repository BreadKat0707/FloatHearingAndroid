package cn.lemondrop.fhreborn.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverBottomSheet
import cn.lemondrop.clover.CloverMenuItem
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import com.composables.icons.lucide.Album
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Volume2
import io.github.composefluent.component.Text

private data class MoreMenuItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
fun PlayerMoreSheet(
    song: Song?,
    artistSeparators: Set<String>,
    onDismiss: () -> Unit,
    onAddToPlaylistClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onTimerClick: () -> Unit = {},
    onAudioOutputClick: () -> Unit = {},
    onThoughtsClick: () -> Unit = {},
    onLyricSettingsClick: () -> Unit = {},
    onViewAlbumClick: () -> Unit = {},
    onViewArtistClick: () -> Unit = {},
    onGoToFolderClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onPropertiesClick: () -> Unit = {},
    onOpenWithClick: () -> Unit = {},
    onHideClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val menuItems = listOf(
        MoreMenuItem("加入歌单", Lucide.Music, onAddToPlaylistClick),
        MoreMenuItem("倍速", Lucide.Gauge, onSpeedClick),
        MoreMenuItem("计划暂停", Lucide.Timer, onTimerClick),
        MoreMenuItem("输出与音效", Lucide.Volume2, onAudioOutputClick),
        MoreMenuItem("想法", Lucide.Lightbulb, onThoughtsClick),
        MoreMenuItem("歌词设置", Lucide.FileText, onLyricSettingsClick),
        MoreMenuItem("查看专辑", Lucide.Album, onViewAlbumClick),
        MoreMenuItem("查看艺术家", Lucide.Mic, onViewArtistClick),
        MoreMenuItem("转至文件夹", Lucide.FolderOpen, onGoToFolderClick),
        MoreMenuItem("分享文件", Lucide.Share2, onShareClick),
        MoreMenuItem("属性", Lucide.Info, onPropertiesClick),
        MoreMenuItem("用其他 app 打开", Lucide.ExternalLink, onOpenWithClick),
        MoreMenuItem("隐藏音乐", Lucide.EyeOff, onHideClick),
        MoreMenuItem("删除文件", Lucide.Trash2, onDeleteClick)
    )

    CloverBottomSheet(
        onDismiss = onDismiss
    ) {
        song?.let { currentSong ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongCoverImage(
                    songId = currentSong.id,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        LazyColumn {
            items(menuItems, key = { it.label }) { item ->
                CloverMenuItem(
                    label = item.label,
                    icon = item.icon,
                    isDestructive = item.label == "删除文件",
                    onClick = {
                        item.onClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}
