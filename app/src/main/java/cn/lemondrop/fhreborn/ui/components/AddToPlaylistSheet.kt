package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.dao.PlaylistWithCount
import cn.lemondrop.fhreborn.data.db.entity.Playlist
import cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 选择歌单弹窗：把歌曲加入目标歌单（支持单首与批量）。
 * 已包含的歌曲在歌单上显示 ✓（批量时部分包含显示部分标记）。
 *
 * @param songIds 目标歌曲 id 列表（单首传 listOf(id)）
 * @param viewModel 歌单 ViewModel
 * @param onDismiss 关闭回调
 */
@Composable
fun AddToPlaylistSheet(
    songIds: List<Long>,
    viewModel: PlaylistViewModel,
    onDismiss: () -> Unit
) {
    val songId = songIds.firstOrNull() ?: 0L
    val playlists by viewModel.getAllPlaylists().collectAsState(initial = emptyList())
    var showCreateField by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var containingPlaylistIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // 加载包含歌曲的歌单集合（"已加入"标记；批量时按第一首查询）
    androidx.compose.runtime.LaunchedEffect(songId) {
        if (songId != 0L) {
            viewModel.getPlaylistIdsContainingSong(songId) { ids ->
                containingPlaylistIds = ids
            }
        }
    }

    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = "加入歌单",
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (playlists.isEmpty() && !showCreateField) {
                Text(
                    text = "还没有歌单，先创建一个吧",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            playlists.forEach { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    songId = songId,
                    contains = songId != 0L && playlist.id in containingPlaylistIds,
                    onAdd = {
                        if (songIds.size <= 1) {
                            viewModel.addSong(playlist.id, songId)
                        } else {
                            viewModel.addSongs(playlist.id, songIds)
                        }
                    },
                    onDismiss = onDismiss
                )
            }

            if (showCreateField) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = "歌单名称",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = "介绍（可选）",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(text = "取消", onClick = { showCreateField = false })
                        TextButton(
                            text = "创建并加入",
                            enabled = newName.isNotBlank(),
                            onClick = {
                                viewModel.createPlaylist(newName, newDescription) { id ->
                                    if (songIds.size <= 1) {
                                        viewModel.addSong(id, songId)
                                    } else {
                                        viewModel.addSongs(id, songIds)
                                    }
                                }
                                onDismiss()
                            }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateField = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Plus,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "新建歌单",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistWithCount,
    songId: Long,
    contains: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onAdd()
                onDismiss()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Lucide.ListMusic,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.songCount} 首歌曲",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        if (contains) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = "已包含",
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.primary
            )
        }
    }
}
