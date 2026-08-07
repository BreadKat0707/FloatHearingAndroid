package cn.lemondrop.fhreborn.ui.components

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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import com.composables.icons.lucide.Album
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Trash2
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet

/**
 * 歌曲上下文菜单弹窗
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
    onDelete: () -> Unit = {}
) {
    val menuItems = listOf(
        Triple("下一首播放", Lucide.SkipForward, onPlayNext),
        Triple("加入歌单", Lucide.Plus, onAddToPlaylist),
        Triple("想法", Lucide.Lightbulb, onThoughts),
        Triple("查看专辑", Lucide.Album, onViewAlbum),
        Triple("查看艺术家", Lucide.Mic, onViewArtist),
        Triple("转至文件夹", Lucide.FolderOpen, onGoToFolder),
        Triple("分享文件", Lucide.Share2, onShare),
        Triple("用其他 app 打开", Lucide.ExternalLink, onOpenWith),
        Triple("属性", Lucide.Info, onProperties),
        Triple("隐藏音乐", Lucide.EyeOff, onHide),
        Triple("删除文件", Lucide.Trash2, onDelete)
    )

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
                songId = song.id,
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
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(1.dp)
                .background(MiuixTheme.colorScheme.outline)
        )

        // 菜单列表
        LazyColumn {
            items(menuItems, key = { it.first }) { (label, icon, onClick) ->
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
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (label == "删除文件") MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MiuixTheme.textStyles.body1,
                        color = if (label == "删除文件") MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
