package cn.lemondrop.fhreborn.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.entity.ScanDirectory
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.scanner.ScanProgress
import cn.lemondrop.fhreborn.scanner.ScanSourceMode
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.util.PermissionUtils
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScanSettingsContent(
    libraryViewModel: LibraryViewModel,
    paddingValues: PaddingValues,
    bottomOverlayHeight: Dp,
    onOpenFolderPicker: () -> Unit,
    onScrolledChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settings = remember(context) { AppSettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val sourceMode by settings.scanSourceMode.collectAsState(initial = ScanSourceMode.MEDIA_STORE)
    val autoScan by settings.autoScanOnLaunch.collectAsState(initial = true)
    val minDurationEnabled by settings.minDurationEnabled.collectAsState(initial = false)
    val minDurationSeconds by settings.minDurationSeconds.collectAsState(initial = 30)
    val scanDirectories by libraryViewModel.allScanDirectories.collectAsState(initial = emptyList())
    val scanProgress by libraryViewModel.scanProgress.collectAsState()

    var allFilesAccess by remember {
        mutableStateOf(PermissionUtils.hasAllFilesAccess(context))
    }
    val directoryPermissionStatus =
        if (allFilesAccess) "All Files 已授权" else "需要 All Files 权限"

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    observeSettingsScroll(listState, onScrolledChange)
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 4.dp,
                bottom = bottomOverlayHeight + 16.dp
            )
        ) {
            item { SmallTitle("扫描来源") }
            item {
                RadioButtonPreference(
                    title = "MediaStore",
                    summary = "扫描 Android 媒体库",
                    selected = sourceMode == ScanSourceMode.MEDIA_STORE,
                    onClick = {
                        scope.launch { settings.setScanSourceMode(ScanSourceMode.MEDIA_STORE) }
                    }
                )
            }
            item {
                RadioButtonPreference(
                    title = "自定义文件夹",
                    summary = "递归扫描已启用的本地目录",
                    selected = sourceMode == ScanSourceMode.DIRECTORY,
                    onClick = {
                        scope.launch { settings.setScanSourceMode(ScanSourceMode.DIRECTORY) }
                    }
                )
            }

            if (sourceMode == ScanSourceMode.DIRECTORY) {
                item { SmallTitle("自定义文件夹") }
                item {
                    Button(
                        onClick = {
                            if (allFilesAccess) {
                                onOpenFolderPicker()
                            } else {
                                PermissionUtils.openAllFilesAccessSettings(context)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Lucide.FolderPlus,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                if (allFilesAccess) "添加目录" else "授权 All Files 后添加目录",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = directoryPermissionStatus,
                            color = if (allFilesAccess) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.error,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp, end = 8.dp)
                        )
                        TextButton(
                            text = "重新检查",
                            onClick = {
                                allFilesAccess = PermissionUtils.hasAllFilesAccess(context)
                            }
                        )
                    }
                }
                items(scanDirectories, key = { it.id }) { directory ->
                    ScanDirectoryRow(
                        directory = directory,
                        onActiveChange = { active ->
                            libraryViewModel.setScanDirectoryActive(directory.id, active)
                        },
                        onRemove = { libraryViewModel.removeScanDirectory(directory) }
                    )
                }
                if (scanDirectories.isEmpty()) {
                    item {
                        Text(
                            text = "尚未添加或启用任何目录",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            item { SmallTitle("扫描行为") }
            item {
                SwitchPreference(
                    title = "启动时自动扫描",
                    summary = "冷启动后对当前来源扫描一次",
                    checked = autoScan,
                    onCheckedChange = {
                        scope.launch { settings.setAutoScanOnLaunch(it) }
                    }
                )
            }
            item {
                SwitchPreference(
                    title = "忽略过短歌曲",
                    summary = if (minDurationEnabled) {
                        "排除时长小于 ${minDurationSeconds} 秒的曲目"
                    } else {
                        "默认关闭"
                    },
                    checked = minDurationEnabled,
                    onCheckedChange = {
                        scope.launch { settings.setMinDurationEnabled(it) }
                    }
                )
            }
            if (minDurationEnabled) {
                item {
                    SliderPreference(
                        title = "最短时长",
                        summary = "10 - 60 秒，步进 1 秒",
                        value = minDurationSeconds.toFloat(),
                        onValueChange = {
                            scope.launch { settings.setMinDurationSeconds(it.toInt()) }
                        },
                        valueText = "${minDurationSeconds} 秒",
                        valueRange = 10f..60f,
                        steps = 49
                    )
                }
                item {
                    Text(
                        text = "过滤在下次扫描当前来源时生效",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item { SmallTitle("手动扫描") }
            item {
                Button(
                    onClick = { libraryViewModel.scanLibrary() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("扫描当前来源")
                }
            }
            val currentProgress = scanProgress
            item {
                Text(
                    text = when (currentProgress) {
                        is ScanProgress.Idle -> "尚未扫描"
                        is ScanProgress.Scanning -> "准备扫描"
                        is ScanProgress.Progress -> "扫描中 ${currentProgress.current}"
                        is ScanProgress.Completed -> "完成：${currentProgress.songsFound} 首"
                        is ScanProgress.Error -> currentProgress.message
                    },
                    color = if (scanProgress is ScanProgress.Error) MiuixTheme.colorScheme.error
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
        LazyListScrollBar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd),
            trackPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 4.dp,
                bottom = bottomOverlayHeight
            )
        )
    }
}

@Composable
private fun ScanDirectoryRow(
    directory: ScanDirectory,
    onActiveChange: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = directory.name,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = directory.path,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1
            )
        }
        Switch(
            checked = directory.isActive,
            onCheckedChange = onActiveChange
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Lucide.Trash2,
                contentDescription = "删除目录",
                tint = MiuixTheme.colorScheme.error
            )
        }
    }
}
