package cn.lemondrop.fhreborn.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ScrollText
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Volume2
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.SongMenuItems
import cn.lemondrop.fhreborn.ui.components.SongMenuItem

private data class MoreMenuItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
    val destructive: Boolean = false
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
    onLyricInfoClick: () -> Unit = {},
    onViewAlbumClick: () -> Unit = {},
    onViewArtistClick: () -> Unit = {},
    onGoToFolderClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onPropertiesClick: () -> Unit = {},
    onOpenWithClick: () -> Unit = {},
    onHideClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    // 公共项与曲目/歌单菜单共用 SongMenuItems 定义（图标与文案全局一致）
    val menuItems = listOf(
        MoreMenuItem(SongMenuItems.AddToPlaylist.label, SongMenuItems.AddToPlaylist.icon, onAddToPlaylistClick),
        MoreMenuItem("倍速", Lucide.Gauge, onSpeedClick),
        MoreMenuItem("计划暂停", Lucide.Timer, onTimerClick),
        MoreMenuItem("输出与音效", Lucide.Volume2, onAudioOutputClick),
        MoreMenuItem(SongMenuItems.Thoughts.label, SongMenuItems.Thoughts.icon, onThoughtsClick),
        MoreMenuItem("歌词设置", Lucide.FileText, onLyricSettingsClick),
        MoreMenuItem("歌词信息", Lucide.ScrollText, onLyricInfoClick),
        MoreMenuItem(SongMenuItems.ViewAlbum.label, SongMenuItems.ViewAlbum.icon, onViewAlbumClick),
        MoreMenuItem(SongMenuItems.ViewArtist.label, SongMenuItems.ViewArtist.icon, onViewArtistClick),
        MoreMenuItem(SongMenuItems.GoToFolder.label, SongMenuItems.GoToFolder.icon, onGoToFolderClick),
        MoreMenuItem(SongMenuItems.Share.label, SongMenuItems.Share.icon, onShareClick),
        MoreMenuItem(SongMenuItems.Properties.label, SongMenuItems.Properties.icon, onPropertiesClick),
        MoreMenuItem(SongMenuItems.OpenWith.label, SongMenuItems.OpenWith.icon, onOpenWithClick),
        MoreMenuItem(SongMenuItems.Hide.label, SongMenuItems.Hide.icon, onHideClick),
        MoreMenuItem(SongMenuItems.Delete.label, SongMenuItems.Delete.icon, onDeleteClick, destructive = true)
    )

    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
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
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.artist,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(1.dp)
                    .background(MiuixTheme.colorScheme.outline)
            )
        }

        LazyColumn {
            items(menuItems, key = { it.label }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            item.onClick()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (item.destructive) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.label,
                        style = MiuixTheme.textStyles.body1,
                        color = if (item.destructive) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
