package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.screens.library.SongCoverImage
import cn.lemondrop.fhreborn.ui.theme.FluentLargeCorner
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
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
        MenuItem("下一首播放", Lucide.SkipForward, onPlayNext),
        MenuItem("加入歌单", Lucide.Plus, onAddToPlaylist),
        MenuItem("想法", Lucide.Lightbulb, onThoughts),
        MenuItem("静态歌词", Lucide.FileText, onStaticLyrics),
        MenuItem("编辑歌词", Lucide.PenLine, onEditLyrics),
        MenuItem("查看专辑", Lucide.Album, onViewAlbum),
        MenuItem("查看艺术家", Lucide.Mic, onViewArtist),
        MenuItem("转至文件夹", Lucide.FolderOpen, onGoToFolder),
        MenuItem("音频标签", Lucide.Tag, onAudioTags),
        MenuItem("分享文件", Lucide.Share2, onShare),
        MenuItem("属性", Lucide.Info, onProperties),
        MenuItem("设为铃声", Lucide.Bell, onSetRingtone),
        MenuItem("隐藏音乐", Lucide.EyeOff, onHide),
        MenuItem("删除文件", Lucide.Trash2, onDelete),
        MenuItem("沉浸模式", Lucide.Maximize, onImmersive)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(FluentLargeCorner))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                .clickable(enabled = false) { }
        ) {
            // 歌曲信息头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 菜单列表
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(menuItems, key = { it.label }) { item ->
                    MenuRow(
                        icon = item.icon,
                        label = item.label,
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
}

private data class MenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (isDestructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
