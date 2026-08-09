package cn.lemondrop.fhreborn.ui.screens.playlists

import android.app.Application
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.LocalDrawerToggle
import cn.lemondrop.fhreborn.LocalDrawerVisible
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.LocalPlayBarOverride
import cn.lemondrop.fhreborn.data.db.dao.PlaylistWithCount
import cn.lemondrop.fhreborn.data.db.entity.Playlist
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.components.AppShell
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.LazyGridScrollBar
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import cn.lemondrop.fhreborn.ui.components.MultiSelectToolbar
import cn.lemondrop.fhreborn.ui.components.PlaylistCover
import cn.lemondrop.fhreborn.ui.components.PlaylistEditSheet
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Grid2x2
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.LayoutList
import com.composables.icons.lucide.LayoutPanelTop
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PlaylistsScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    playerViewModel: PlayerViewModel,
    onOpenPlaylist: (Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModel.Factory(context.applicationContext as Application)
    )
    val playlists by viewModel.getAllPlaylists().collectAsState(initial = emptyList())
    val drawerVisible = LocalDrawerVisible.current
    val drawerToggle = LocalDrawerToggle.current

    // 歌单视图样式（list / grid / card / square）
    val appSettingsRepository = remember { cn.lemondrop.fhreborn.data.repository.AppSettingsRepository(context) }
    val viewStyle by appSettingsRepository.playlistViewStyle.collectAsState(initial = "list")
    val settingsScope = rememberCoroutineScope()

    // 列表/网格滚动状态（滚动条用）
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    var showCreateSheet by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var deletingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }

    // 多选模式（批量删除歌单）
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedPlaylistIds = remember { mutableStateSetOf<Long>() }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    // 多选时隐藏全局播放条（底部由多选工具栏接管）；离开页面时复位
    val playBarOverride = LocalPlayBarOverride.current
    LaunchedEffect(multiSelectMode) {
        playBarOverride.value = multiSelectMode
    }
    DisposableEffect(Unit) {
        onDispose { playBarOverride.value = false }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    // 导入：OpenDocument 读取 JSON/M3U 文件
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            snackbarScope.launch {
                val fileName = withContext(Dispatchers.IO) {
                    context.contentResolver.query(
                        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: uri.lastPathSegment
                }
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }
                }
                if (content.isNullOrBlank()) {
                    snackbarHostState.showSnackbar("无法读取文件")
                    return@launch
                }
                val isJson = fileName?.endsWith(".json", ignoreCase = true) == true
                viewModel.importPlaylist(
                    suggestedName = fileName ?: "导入的歌单",
                    content = content,
                    isJson = isJson,
                    onResult = { success ->
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) "导入成功" else "未匹配到任何歌曲"
                            )
                        }
                    }
                )
            }
        }
    }

    AppShell(
        drawerVisible = drawerVisible.value,
        onDismissDrawer = { drawerVisible.value = false },
        currentRoute = currentRoute,
        onNavigate = { route ->
            onNavigate(route)
        },
        onScheduledPauseClick = { playerViewModel.showScheduledPause() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppBackgroundLayer()
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    BlurTopBar(
                        title = "歌单",
                        navigationIcon = {
                            IconButton(onClick = { drawerToggle() }) {
                                Icon(
                                    imageVector = Lucide.Menu,
                                    contentDescription = "菜单"
                                )
                            }
                        },
                        actions = {
                            // 视图样式切换
                            top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu(
                                entries = listOf(
                                    DropdownEntry(
                                        items = listOf(
                                            DropdownItem(
                                                "列表",
                                                icon = { mod -> Icon(Lucide.LayoutList, null, modifier = mod) },
                                                onClick = { settingsScope.launch { appSettingsRepository.setPlaylistViewStyle("list") } }
                                            ),
                                            DropdownItem(
                                                "双栏列表",
                                                icon = { mod -> Icon(Lucide.LayoutGrid, null, modifier = mod) },
                                                onClick = { settingsScope.launch { appSettingsRepository.setPlaylistViewStyle("grid") } }
                                            ),
                                            DropdownItem(
                                                "卡片",
                                                icon = { mod -> Icon(Lucide.LayoutPanelTop, null, modifier = mod) },
                                                onClick = { settingsScope.launch { appSettingsRepository.setPlaylistViewStyle("card") } }
                                            ),
                                            DropdownItem(
                                                "方形",
                                                icon = { mod -> Icon(Lucide.Grid2x2, null, modifier = mod) },
                                                onClick = { settingsScope.launch { appSettingsRepository.setPlaylistViewStyle("square") } }
                                            )
                                        )
                                    )
                                ),
                                minHeight = 40.dp,
                                minWidth = 40.dp,
                            ) {
                                Icon(
                                    imageVector = Lucide.LayoutGrid,
                                    contentDescription = "视图样式"
                                )
                            }
                            IconButton(onClick = { showCreateSheet = true }) {
                                Icon(
                                    imageVector = Lucide.Plus,
                                    contentDescription = "新建歌单"
                                )
                            }
                            IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "audio/x-mpegurl", "*/*")) }) {
                                Icon(
                                    imageVector = Lucide.Upload,
                                    contentDescription = "导入歌单"
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                val playBarHeight = LocalGlobalPlayBarHeight.current
                // 多选点击/长按统一处理（所有视图共用）
                fun onPlaylistClick(playlist: PlaylistWithCount) {
                    if (multiSelectMode) {
                        if (playlist.id in selectedPlaylistIds) {
                            selectedPlaylistIds.remove(playlist.id)
                        } else {
                            selectedPlaylistIds.add(playlist.id)
                        }
                    } else {
                        onOpenPlaylist(playlist.id)
                    }
                }
                fun onPlaylistLongClick(playlist: PlaylistWithCount) {
                    if (multiSelectMode) {
                        // 多选中长按：退出多选
                        multiSelectMode = false
                        selectedPlaylistIds.clear()
                    } else {
                        // 长按进入多选并选中当前项
                        multiSelectMode = true
                        selectedPlaylistIds.add(playlist.id)
                    }
                }

                when (viewStyle) {
                    "grid" -> Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = playBarHeight + 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (playlists.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { PlaylistEmptyHint() }
                            }
                            gridItems(playlists, key = { it.id }) { playlist ->
                                PlaylistGridItem(
                                    playlist = playlist,
                                    viewModel = viewModel,
                                    onClick = { onPlaylistClick(playlist) },
                                    onLongClick = { onPlaylistLongClick(playlist) },
                                    selectionMode = multiSelectMode,
                                    selected = playlist.id in selectedPlaylistIds
                                )
                            }
                        }
                        LazyGridScrollBar(
                            gridState = gridState,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                    "card" -> Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = playBarHeight + 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (playlists.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { PlaylistEmptyHint() }
                            }
                            gridItems(playlists, key = { it.id }) { playlist ->
                                PlaylistCardItem(
                                    playlist = playlist,
                                    viewModel = viewModel,
                                    onClick = { onPlaylistClick(playlist) },
                                    onLongClick = { onPlaylistLongClick(playlist) },
                                    selectionMode = multiSelectMode,
                                    selected = playlist.id in selectedPlaylistIds
                                )
                            }
                        }
                        LazyGridScrollBar(
                            gridState = gridState,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                    "square" -> Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = playBarHeight + 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (playlists.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { PlaylistEmptyHint() }
                            }
                            gridItems(playlists, key = { it.id }) { playlist ->
                                PlaylistSquareItem(
                                    playlist = playlist,
                                    viewModel = viewModel,
                                    onClick = { onPlaylistClick(playlist) },
                                    onLongClick = { onPlaylistLongClick(playlist) },
                                    selectionMode = multiSelectMode,
                                    selected = playlist.id in selectedPlaylistIds
                                )
                            }
                        }
                        LazyGridScrollBar(
                            gridState = gridState,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                    else -> Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = playBarHeight + 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (playlists.isEmpty()) {
                                item { PlaylistEmptyHint() }
                            }
                            items(playlists, key = { it.id }) { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    viewModel = viewModel,
                                    onClick = { onPlaylistClick(playlist) },
                                    onPlayClick = { viewModel.playPlaylist(playlist.id, playerViewModel) },
                                    onLongClick = { onPlaylistLongClick(playlist) },
                                    onMenuClick = { editingPlaylist = playlist },
                                    selectionMode = multiSelectMode,
                                    selected = playlist.id in selectedPlaylistIds
                                )
                            }
                        }
                        LazyListScrollBar(
                            listState = listState,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }

                // 多选底部工具栏（悬浮）
                if (multiSelectMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        MultiSelectToolbar(
                            selectedCount = selectedPlaylistIds.size,
                            onAddToPlaylist = {},
                            onAddToQueue = {},
                            onShare = {},
                            onDelete = {
                                if (selectedPlaylistIds.isNotEmpty()) showBatchDeleteConfirm = true
                            },
                            onExit = {
                                multiSelectMode = false
                                selectedPlaylistIds.clear()
                            },
                            showSongActions = false
                        )
                    }
                }
            }
        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (multiSelectMode) 0.dp else LocalGlobalPlayBarHeight.current + 8.dp)
        )
        }
    }

    // 新建歌单
    if (showCreateSheet) {
        PlaylistEditSheet(
            title = "新建歌单",
            initialName = "",
            initialDescription = "",
            onDismiss = { showCreateSheet = false },
            onSave = { name, desc, _, _, _ ->
                viewModel.createPlaylist(name, desc)
                showCreateSheet = false
            }
        )
    }

    // 编辑歌单
    editingPlaylist?.let { playlist ->
        var editCoverSongs by remember(playlist.id) { mutableStateOf<List<Song>>(emptyList()) }
        LaunchedEffect(playlist.id) {
            viewModel.getSongsSnapshot(playlist.id) { songs -> editCoverSongs = songs }
        }
        PlaylistEditSheet(
            title = "编辑歌单",
            initialName = playlist.name,
            initialDescription = playlist.description.orEmpty(),
            initialPlayMode = playlist.defaultPlayMode,
            initialCoverPath = playlist.coverPath,
            initialCoverSource = playlist.coverSource,
            coverSongs = editCoverSongs,
            onDismiss = { editingPlaylist = null },
            onSave = { name, desc, playMode, coverPath, coverSource ->
                viewModel.updatePlaylist(playlist.id, name, desc, playMode)
                viewModel.setCover(playlist.id, coverPath, coverSource)
                editingPlaylist = null
            },
            onDelete = {
                editingPlaylist = null
                deletingPlaylist = playlist
            }
        )
    }

    // 删除确认
    deletingPlaylist?.let { playlist ->
        FhBottomSheet(
            show = true,
            onDismissRequest = { deletingPlaylist = null },
            title = "删除歌单",
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    text = "确定删除「${playlist.name}」吗？歌单中的歌曲不会从媒体库删除。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(text = "取消", onClick = { deletingPlaylist = null })
                    TextButton(
                        text = "删除",
                        onClick = {
                            viewModel.deletePlaylist(playlist.id)
                            deletingPlaylist = null
                        }
                    )
                }
            }
        }
    }

    // 批量删除歌单确认
    if (showBatchDeleteConfirm) {
        BackHandler { showBatchDeleteConfirm = false }
        FhBottomSheet(
            show = true,
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = "删除歌单",
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    text = "确定删除选中的 ${selectedPlaylistIds.size} 个歌单吗？歌单中的歌曲不会从媒体库删除。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(text = "取消", onClick = { showBatchDeleteConfirm = false })
                    TextButton(
                        text = "删除",
                        onClick = {
                            selectedPlaylistIds.forEach { viewModel.deletePlaylist(it) }
                            showBatchDeleteConfirm = false
                            multiSelectMode = false
                            selectedPlaylistIds.clear()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistEmptyHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "还没有歌单\n点击右上角 + 创建",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2
        )
    }
}

/** 双栏列表视图：横向紧凑卡片（封面 + 名称/数量） */
@Composable
private fun PlaylistGridItem(
    playlist: PlaylistWithCount,
    viewModel: PlaylistViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    var coverSongIds by remember(playlist.id) { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(playlist.id, playlist.songCount) {
        viewModel.getFirstSongIds(playlist.id) { ids -> coverSongIds = ids }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlaylistCover(
                songIds = coverSongIds,
                coverPath = playlist.coverPath,
                coverSource = playlist.coverSource,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${playlist.songCount} 首歌曲",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
        if (selectionMode) {
            SelectionBadge(selected = selected, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/** 卡片视图：大封面卡片（PressFeedback Tilt） */
@Composable
private fun PlaylistCardItem(
    playlist: PlaylistWithCount,
    viewModel: PlaylistViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    var coverSongIds by remember(playlist.id) { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(playlist.id, playlist.songCount) {
        viewModel.getFirstSongIds(playlist.id) { ids -> coverSongIds = ids }
    }
    top.yukonga.miuix.kmp.basic.Card(
        modifier = Modifier.fillMaxWidth(),
        pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Tilt,
        onClick = onClick,
        onLongPress = onLongClick,
    ) {
        Box {
            Column(modifier = Modifier.fillMaxWidth()) {
                PlaylistCover(
                    songIds = coverSongIds,
                    coverPath = playlist.coverPath,
                    coverSource = playlist.coverSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = playlist.name,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlist.songCount} 首歌曲",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
            if (selectionMode) {
                SelectionBadge(selected = selected, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }
    }
}

/** 方形视图：方形封面 + 名称 */
@Composable
private fun PlaylistSquareItem(
    playlist: PlaylistWithCount,
    viewModel: PlaylistViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    var coverSongIds by remember(playlist.id) { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(playlist.id, playlist.songCount) {
        viewModel.getFirstSongIds(playlist.id) { ids -> coverSongIds = ids }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column {
            Box {
                PlaylistCover(
                    songIds = coverSongIds,
                    coverPath = playlist.coverPath,
                    coverSource = playlist.coverSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                if (selectionMode) {
                    SelectionBadge(selected = selected, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = playlist.name,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 多选勾选指示（圆形，选中 primary 填充） */
@Composable
private fun SelectionBadge(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (selected) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = "已选择",
                modifier = Modifier.size(14.dp),
                tint = MiuixTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistWithCount,
    viewModel: PlaylistViewModel,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    var coverSongIds by remember(playlist.id) { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(playlist.id, playlist.songCount) {
        viewModel.getFirstSongIds(playlist.id) { ids -> coverSongIds = ids }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            // 多选勾选指示
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = "已选择",
                        modifier = Modifier.size(14.dp),
                        tint = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        PlaylistCover(
            songIds = coverSongIds,
            coverPath = playlist.coverPath,
            coverSource = playlist.coverSource,
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onClick)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.songCount} 首歌曲",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        if (!selectionMode) {
            IconButton(onClick = onPlayClick) {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = "播放歌单",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}
