package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.dao.PlaylistWithCount
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
 * 选择歌单弹窗：把歌曲加入目标歌单（支持单首与批量，可一次勾选多个歌单）。
 * - 点击歌单行切换"加入目标"勾选，可多选，底部统一提交
 * - 已包含该歌曲的歌单显示"已加入"标记
 * - 新建歌单：先创建并保存（自动勾选新歌单），可继续勾选其他歌单后统一加入
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
    // 勾选的歌单（可多选）。null 表示尚未从"已包含"集合初始化
    var selectedIds by remember { mutableStateOf<Set<Long>?>(null) }

    // 加载包含歌曲的歌单集合（批量时按第一首查询）；初始勾选 = 已包含的歌单
    LaunchedEffect(songId) {
        if (songId != 0L) {
            viewModel.getPlaylistIdsContainingSong(songId) { ids ->
                containingPlaylistIds = ids
                if (selectedIds == null) selectedIds = ids
            }
        } else {
            containingPlaylistIds = emptySet()
            if (selectedIds == null) selectedIds = emptySet()
        }
    }
    val currentSelected = selectedIds ?: emptySet()

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
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        selected = playlist.id in currentSelected,
                        onToggle = {
                            selectedIds = if (playlist.id in currentSelected) {
                                currentSelected - playlist.id
                            } else {
                                currentSelected + playlist.id
                            }
                        }
                    )
                }
            }

            // 新建歌单：先创建保存，自动勾选新歌单
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
                            text = "创建",
                            enabled = newName.isNotBlank(),
                            onClick = {
                                val name = newName.trim()
                                val desc = newDescription.trim()
                                newName = ""
                                newDescription = ""
                                showCreateField = false
                                // 先保存歌单，再勾选它，等待用户统一提交
                                viewModel.createPlaylist(name, desc) { id ->
                                    selectedIds = (selectedIds ?: emptySet()) + id
                                }
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

            // 底部统一提交栏：勾选 = 加入，取消勾选 = 从歌单移除
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentSelected.isEmpty()) "勾选歌单以加入；取消勾选即移除"
                    else "已选 ${currentSelected.size} 个歌单",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "完成",
                    onClick = {
                        // 新增勾选：加入
                        currentSelected.forEach { id ->
                            if (id !in containingPlaylistIds) {
                                if (songIds.size <= 1) {
                                    viewModel.addSong(id, songId)
                                } else {
                                    viewModel.addSongs(id, songIds)
                                }
                            }
                        }
                        // 取消勾选：从歌单移除
                        containingPlaylistIds.forEach { id ->
                            if (id !in currentSelected) {
                                viewModel.removeSong(id, songId)
                            }
                        }
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistWithCount,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
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
        // 勾选指示（勾选 = 加入，取消勾选 = 移除）
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = "已选择",
                    modifier = Modifier.size(13.dp),
                    tint = MiuixTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
