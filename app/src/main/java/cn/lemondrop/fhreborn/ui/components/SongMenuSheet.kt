package cn.lemondrop.fhreborn.ui.components

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
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.clover.CloverBottomSheet
import cn.lemondrop.clover.CloverMenuItem
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import com.composables.icons.lucide.Album
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Tag
import com.composables.icons.lucide.Trash2
import io.github.composefluent.component.Text

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
    onStaticLyrics: () -> Unit = {},
    onEditLyrics: () -> Unit = {},
    onViewAlbum: () -> Unit = {},
    onViewArtist: () -> Unit = {},
    onGoToFolder: () -> Unit = {},
    onAudioTags: () -> Unit = {},
    onShare: () -> Unit = {},
    onProperties: () -> Unit = {},
    onSetRingtone: () -> Unit = {},
    onHide: () -> Unit = {},
    onDelete: () -> Unit = {},
    onImmersive: () -> Unit = {}
) {
    val menuItems = listOf(
        Triple("下一首播放", Lucide.SkipForward, onPlayNext),
        Triple("加入歌单", Lucide.Plus, onAddToPlaylist),
        Triple("想法", Lucide.Lightbulb, onThoughts),
        Triple("静态歌词", Lucide.FileText, onStaticLyrics),
        Triple("编辑歌词", Lucide.PenLine, onEditLyrics),
        Triple("查看专辑", Lucide.Album, onViewAlbum),
        Triple("查看艺术家", Lucide.Mic, onViewArtist),
        Triple("转至文件夹", Lucide.FolderOpen, onGoToFolder),
        Triple("音频标签", Lucide.Tag, onAudioTags),
        Triple("分享文件", Lucide.Share2, onShare),
        Triple("属性", Lucide.Info, onProperties),
        Triple("设为铃声", Lucide.Bell, onSetRingtone),
        Triple("隐藏音乐", Lucide.EyeOff, onHide),
        Triple("删除文件", Lucide.Trash2, onDelete),
        Triple("沉浸模式", Lucide.Maximize, onImmersive)
    )

    CloverBottomSheet(onDismiss = onDismiss) {
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // 菜单列表
        LazyColumn {
            items(menuItems, key = { it.first }) { (label, icon, onClick) ->
                CloverMenuItem(
                    label = label,
                    icon = icon,
                    isDestructive = label == "删除文件",
                    onClick = {
                        onClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}
