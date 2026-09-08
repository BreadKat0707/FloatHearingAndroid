package cn.lemondrop.fhreborn.ui.screens.settings

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.util.PathUtils
import cn.lemondrop.fhreborn.util.PermissionUtils
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

private const val ROOT_PATH = "/storage"

@Composable
fun FolderPickerContent(
    libraryViewModel: LibraryViewModel,
    paddingValues: PaddingValues,
    bottomOverlayHeight: Dp,
    onBack: () -> Unit,
    onScrolledChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var hasAllFiles by remember { mutableStateOf(PermissionUtils.hasAllFilesAccess(context)) }

    if (!hasAllFiles) {
        PermissionMissingContent(
            onGrant = { PermissionUtils.openAllFilesAccessSettings(context) },
            onRefresh = { hasAllFiles = PermissionUtils.hasAllFilesAccess(context) }
        )
        return
    }

    var currentPath by remember { mutableStateOf(ROOT_PATH) }
    var entries by remember { mutableStateOf<List<FolderEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val selectedPaths = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    observeSettingsScroll(listState, onScrolledChange)
    val parentPath = remember(currentPath) {
        if (currentPath == ROOT_PATH) null else File(currentPath).parent ?: ROOT_PATH
    }

    BackHandler(enabled = parentPath != null) {
        currentPath = parentPath ?: ROOT_PATH
    }

    LaunchedEffect(currentPath, hasAllFiles) {
        loading = true
        errorMessage = null
        entries = withContext(Dispatchers.IO) { loadEntries(currentPath) }
        if (entries.isEmpty()) {
            errorMessage = "无法读取该目录"
        }
        loading = false
    }

    val existingPaths = remember(libraryViewModel.allScanDirectories.value) {
        libraryViewModel.allScanDirectories.value.map { PathUtils.normalizePath(it.path) }.toSet()
    }

    val canAddCurrent = currentPath != ROOT_PATH
    val hasSelection = selectedPaths.isNotEmpty()

    fun addDirectory(path: String) {
        val normalized = PathUtils.normalizePath(path)
        if (normalized !in existingPaths) {
            libraryViewModel.addScanDirectory(normalized, displayName(normalized))
        }
    }

    fun addSelectedDirectories(paths: List<String>) {
        val normalized = paths
            .map { PathUtils.normalizePath(it) }
            .distinct()
            .sortedBy { it.length }
            .filter { path ->
                !paths.any { other ->
                    val normalizedOther = PathUtils.normalizePath(other)
                    normalizedOther != path &&
                        PathUtils.isPathUnderFolder(path, normalizedOther)
                }
            }
        normalized.forEach { addDirectory(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (parentPath != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currentPath = parentPath }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "返回上级",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "返回上级",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            Text(
                text = currentPath,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = bottomOverlayHeight + 96.dp
                )
            ) {
                when {
                    loading -> item { Text("正在读取目录...", modifier = Modifier.padding(16.dp)) }
                    errorMessage != null && entries.isEmpty() -> item {
                        Text(
                            errorMessage.orEmpty(),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> items(entries, key = { it.path }) { entry ->
                        FolderPickerRow(
                            entry = entry,
                            selected = entry.path in selectedPaths,
                            onOpen = {
                                currentPath = entry.path
                            },
                            onToggle = {
                                if (entry.path in selectedPaths) {
                                    selectedPaths.remove(entry.path)
                                } else {
                                    selectedPaths.add(entry.path)
                                }
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            if (selectedPaths.isNotEmpty()) {
                Button(
                    onClick = {
                        addSelectedDirectories(selectedPaths.toList())
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加所选目录 (${selectedPaths.size})")
                }
            }
            if (canAddCurrent) {
                TextButton(
                    text = "添加当前目录",
                    onClick = {
                        addDirectory(currentPath)
                        onBack()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            if (!hasSelection && !canAddCurrent) {
                Text(
                    text = "进入目录后勾选，可一次添加多个",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun PermissionMissingContent(
    onGrant: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "需要 All Files 权限才能浏览本地目录",
            color = MiuixTheme.colorScheme.onSurface
        )
        Button(onClick = onGrant, modifier = Modifier.padding(top = 16.dp)) {
            Text("前往授权")
        }
        TextButton(
            text = "重新检查",
            onClick = onRefresh
        )
    }
}

@Composable
private fun FolderPickerRow(
    entry: FolderEntry,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Lucide.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MiuixTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp)
        ) {
            Text(
                text = entry.name,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = entry.path,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1
            )
        }
        Checkbox(
            state = if (selected) ToggleableState.On else ToggleableState.Off,
            onClick = onToggle
        )
    }
}

private data class FolderEntry(
    val name: String,
    val path: String
)

private suspend fun loadEntries(path: String): List<FolderEntry> = withContext(Dispatchers.IO) {
    val files = if (path == ROOT_PATH) {
        rootStorageDirectories()
    } else {
        File(path).listFiles()?.filter { it.isDirectory && it.canRead() }.orEmpty()
    }
    files
        .map { FileEntry(it, displayName(it.absolutePath)) }
        .sortedBy { it.name.lowercase() }
        .map { FolderEntry(name = it.name, path = it.file.absolutePath) }
}

private data class FileEntry(
    val file: File,
    val name: String
)

private fun rootStorageDirectories(): List<File> {
    val external = File(Environment.getExternalStorageDirectory().absolutePath)
    val rootFiles = File(ROOT_PATH).listFiles()
        ?.filter { it.isDirectory && it.canRead() }
        ?.filterNot { root ->
            root.name == "self" ||
                (root.name == "emulated" &&
                    PathUtils.normalizePath(external.absolutePath)
                        .startsWith(PathUtils.normalizePath(root.absolutePath) + "/"))
        }
        .orEmpty()
    return buildList {
        addAll(rootFiles)
        if (rootFiles.none { PathUtils.normalizePath(it.absolutePath) == PathUtils.normalizePath(external.absolutePath) }) {
            add(external)
        }
    }.distinctBy { PathUtils.normalizePath(it.absolutePath) }
}

private fun displayName(path: String): String {
    val internal = Environment.getExternalStorageDirectory().absolutePath
    if (PathUtils.normalizePath(path) == PathUtils.normalizePath(internal)) return "内部存储"
    return File(path).name.ifBlank { path }
}
