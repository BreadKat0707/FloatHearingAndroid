package cn.lemondrop.fhreborn.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.entity.PlayMode
import cn.lemondrop.fhreborn.data.db.entity.Song
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Repeat1
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.Trash2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

/**
 * 歌单创建/编辑弹窗（列表页与详情页共用）。
 *
 * 编辑态支持封面设置：自动（前 3 首拼图）/ 从歌单内选歌曲 / 自选图片。
 *
 * @param title 弹窗标题
 * @param initialName 初始名称
 * @param initialDescription 初始介绍
 * @param initialPlayMode 初始默认播放模式
 * @param initialCoverPath 初始封面路径（coverSource=1 时存 songId 字符串，=2 时存文件路径）
 * @param initialCoverSource 初始封面来源
 * @param coverSongs 歌单内歌曲（"从歌单选封面"用）
 * @param onDismiss 关闭
 * @param onSave 保存（名称、介绍、播放模式、封面路径、封面来源）
 * @param onDelete 删除入口（编辑态提供）
 */
@Composable
fun PlaylistEditSheet(
    title: String,
    initialName: String,
    initialDescription: String,
    initialPlayMode: Int = PlayMode.SEQUENTIAL,
    initialCoverPath: String? = null,
    initialCoverSource: Int = PlaylistCoverSource.AUTO,
    coverSongs: List<Song> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, String?, Int, String?, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var playMode by remember { mutableStateOf(initialPlayMode) }
    var coverSource by remember { mutableStateOf(initialCoverSource) }
    var coverPath by remember { mutableStateOf(initialCoverPath) }

    val playModeOptions = listOf(
        PlayMode.SEQUENTIAL to "顺序播放",
        PlayMode.LIST_LOOP to "列表循环",
        PlayMode.SINGLE_LOOP to "单曲循环",
        PlayMode.SHUFFLE to "随机播放"
    )

    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = title,
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "歌单名称（必填）",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "介绍（可选）",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    // 默认播放模式：下拉选择
                    OverlayDropdownMenu(
                        entries = listOf(
                            DropdownEntry(
                                items = playModeOptions.map { (mode, label) ->
                                    DropdownItem(
                                        text = label,
                                        selected = playMode == mode,
                                        icon = { mod ->
                                            Icon(
                                                imageVector = when (mode) {
                                                    PlayMode.SEQUENTIAL -> Lucide.ListMusic
                                                    PlayMode.LIST_LOOP -> Lucide.Repeat
                                                    PlayMode.SINGLE_LOOP -> Lucide.Repeat1
                                                    else -> Lucide.Shuffle
                                                },
                                                contentDescription = null,
                                                modifier = mod
                                            )
                                        },
                                        onClick = { playMode = mode }
                                    )
                                }
                            )
                        ),
                        title = "默认播放模式",
                        summary = playModeOptions.firstOrNull { it.first == playMode }?.second
                            ?: playModeOptions.first().second,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 封面设置（仅编辑态展示，创建时歌单为空无意义）
            if (onDelete != null) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "封面",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        CoverOptionRow(
                            label = "自动（前 3 首歌曲拼图）",
                            selected = coverSource == PlaylistCoverSource.AUTO,
                            onClick = { coverSource = PlaylistCoverSource.AUTO; coverPath = null }
                        )
                        CoverOptionRow(
                            label = "从歌单内选歌曲",
                            selected = coverSource == PlaylistCoverSource.SONG_COVER,
                            onClick = { coverSource = PlaylistCoverSource.SONG_COVER }
                        )
                        CoverOptionRow(
                            label = "自选图片",
                            selected = coverSource == PlaylistCoverSource.SELF_IMAGE,
                            onClick = { coverSource = PlaylistCoverSource.SELF_IMAGE }
                        )

                        // 从歌单选歌曲：展开歌曲列表
                        if (coverSource == PlaylistCoverSource.SONG_COVER) {
                            Spacer(modifier = Modifier.height(4.dp))
                            if (coverSongs.isEmpty()) {
                                Text(
                                    text = "歌单内暂无歌曲",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            } else {
                                LazyColumn(modifier = Modifier.height(160.dp)) {
                                    items(coverSongs, key = { it.id }) { song ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { coverPath = song.id.toString() }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Lucide.ListMusic,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = song.title,
                                                style = MiuixTheme.textStyles.body2,
                                                color = MiuixTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            if (coverPath == song.id.toString()) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = "已选",
                                                    style = MiuixTheme.textStyles.footnote1,
                                                    color = MiuixTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 自选图片：系统照片选择器 + 复制到应用目录
                        if (coverSource == PlaylistCoverSource.SELF_IMAGE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SelfImagePicker(
                                onPicked = { savedPath -> coverPath = savedPath }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Lucide.Trash2,
                                contentDescription = "删除歌单",
                                tint = MiuixTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = "保存",
                        enabled = name.isNotBlank(),
                        onClick = { onSave(name, description, playMode, coverPath, coverSource) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SelfImagePicker(
    onPicked: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val saved = copyImageToAppDir(context, uri)
                if (saved != null) onPicked(saved)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "选择图片…",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.primary
        )
    }
}

private suspend fun copyImageToAppDir(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val dir = File(context.filesDir, "covers").apply { mkdirs() }
        val target = File(dir, "cover_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target.absolutePath
    } catch (_: Exception) {
        null
    }
}
