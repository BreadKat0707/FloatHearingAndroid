package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.entity.Song
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 歌曲上下文菜单弹窗（曲目菜单 / 歌单详情菜单共用）。
 * 菜单项使用 [SongMenuItems] 共享定义，保证图标与文案全局一致。
 *
 * @param onRemoveFromPlaylist 非 null 时显示"从歌单移除"项（歌单详情上下文）
 */
@Composable
fun SongMenuSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onThoughts: () -> Unit = {},
    onViewAlbum: () -> Unit = {},
    onViewArtist: () -> Unit = {},
    onGoToFolder: () -> Unit = {},
    onShare: () -> Unit = {},
    onOpenWith: () -> Unit = {},
    onProperties: () -> Unit = {},
    onHide: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val menuItems = buildList {
        add(SongMenuItems.PlayNext to onPlayNext)
        add(SongMenuItems.AddToPlaylist to onAddToPlaylist)
        add(SongMenuItems.Thoughts to onThoughts)
        if (onRemoveFromPlaylist != null) {
            add(SongMenuItems.RemoveFromPlaylist to onRemoveFromPlaylist)
        }
        add(SongMenuItems.ViewAlbum to onViewAlbum)
        add(SongMenuItems.ViewArtist to onViewArtist)
        add(SongMenuItems.GoToFolder to onGoToFolder)
        add(SongMenuItems.Share to onShare)
        add(SongMenuItems.OpenWith to onOpenWith)
        add(SongMenuItems.Properties to onProperties)
        add(SongMenuItems.Hide to onHide)
        add(SongMenuItems.Delete to onDelete)
    }

    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        // 歌曲信息头部
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongCoverImage(
                song = song,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 分隔线
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(1.dp)
                .background(MiuixTheme.colorScheme.outline)
        )

        // 菜单列表
        LazyColumn {
            items(menuItems, key = { it.first.label }) { (item, onClick) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClick()
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
