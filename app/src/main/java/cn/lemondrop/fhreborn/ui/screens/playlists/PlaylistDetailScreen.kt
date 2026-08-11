package cn.lemondrop.fhreborn.ui.screens.playlists

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.LocalPlayBarOverride
import cn.lemondrop.fhreborn.data.db.entity.PlaylistSortType
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.AddToPlaylistSheet
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.FhListItem
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import cn.lemondrop.fhreborn.ui.components.MultiSelectToolbar
import cn.lemondrop.fhreborn.ui.components.PlaylistCover
import cn.lemondrop.fhreborn.ui.components.PlaylistEditSheet
import cn.lemondrop.fhreborn.ui.components.SelectionStateButton
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import cn.lemondrop.fhreborn.ui.components.SongMenuSheet
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.ListChecks
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel,
    onNavigateToAlbum: (String, String?) -> Unit = { _, _ -> },
    onNavigateToArtist: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModel.Factory(context.applicationContext as Application)
    )
    // 批量删除文件用（MediaStore 删除 + 刷新媒体库）
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(context.applicationContext as Application)
    )
    val playlist by viewModel.getPlaylist(playlistId).collectAsState(initial = null)
    val songs by viewModel.getSortedSongs(playlistId, playlist?.sortType ?: 0).collectAsState(initial = emptyList())
    var totalDuration by remember { mutableStateOf(0L) }
    LaunchedEffect(playlistId) {
        viewModel.getPlaylistTotalDuration(playlistId) { totalDuration = it }
    }

    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var addTargetSong by remember { mutableStateOf<Song?>(null) }
    var showSongProperties by remember { mutableStateOf(false) }
    var propertiesSong by remember { mutableStateOf<Song?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    // 多选模式（批量操作）
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateSetOf<Long>() }
    var showBatchAddSheet by remember { mutableStateOf(false) }
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

    // 自定义排序：长按拖拽重排
    val isCustomSort = playlist?.sortType == PlaylistSortType.CUSTOM
    val listState = rememberLazyListState()
    // 顶栏滚动感知：列表滚离顶部时显示背景/模糊，回顶隐藏
    val topBarScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }.value
    val dragSpacingPx = with(LocalDensity.current) { 4.dp.toPx() }
    var displaySongs by remember(songs) { mutableStateOf(songs) }
    var draggingSongId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val latestDisplaySongs by rememberUpdatedState(displaySongs)
    // Flow 刷新（重排落库/增删）时同步本地顺序；拖拽中不打断
    LaunchedEffect(songs) {
        if (draggingSongId == null) displaySongs = songs
    }

    // 导出：CreateDocument 保存 M3U/JSON
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingExportIsJson by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val isJson = pendingExportIsJson
            pendingExportIsJson = false
            val exportContent = pendingExportContent
            pendingExportContent = null
            if (exportContent != null) {
                snackbarScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(exportContent.toByteArray())
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("导出失败: ${e.message}")
                            return@withContext
                        }
                    }
                    snackbarHostState.showSnackbar("已导出 ${if (isJson) "JSON" else "M3U"}")
                }
            }
        }
    }

    BackHandler {
        if (multiSelectMode) {
            // 多选时返回键先退出多选
            multiSelectMode = false
            selectedSongIds.clear()
        } else {
            onBack()
        }
    }

    // 层背景：顶栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                BlurTopBar(
                    backdrop = backdrop,
                    // 列表滚动后（或多选时）才显示顶栏背景/模糊；回顶隐藏
                    scrolled = topBarScrolled || multiSelectMode,
                    title = if (multiSelectMode) "已选 ${selectedSongIds.size} 首" else playlist?.name ?: "歌单",
                    navigationIcon = {
                        if (multiSelectMode) {
                            IconButton(onClick = {
                                multiSelectMode = false
                                selectedSongIds.clear()
                            }) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "退出多选"
                                )
                            }
                        } else {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Lucide.ArrowLeft,
                                    contentDescription = "返回"
                                )
                            }
                        }
                    },
                    actions = {
                        if (multiSelectMode) {
                            // 选择状态：全选 / 选中多个 / 全未选（退出在标题栏左侧）
                            val allSongIds = displaySongs.map { it.id }
                            SelectionStateButton(
                                selectedCount = selectedSongIds.size,
                                totalCount = allSongIds.size,
                                onSelectAll = {
                                    selectedSongIds.clear()
                                    selectedSongIds.addAll(allSongIds)
                                },
                                onDeselectAll = { selectedSongIds.clear() }
                            )
                        } else {
                            // 多选 / 排序 / 导出 / 编辑 收纳进三点菜单
                            top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu(
                                entries = listOf(
                                    DropdownEntry(
                                        items = listOf(
                                            DropdownItem(
                                                "多选",
                                                icon = { mod -> Icon(Lucide.ListChecks, null, modifier = mod) },
                                                onClick = { multiSelectMode = true }
                                            ),
                                            DropdownItem(
                                                "排序",
                                                icon = { mod -> Icon(Lucide.ArrowUpDown, null, modifier = mod) },
                                                onClick = { showSortSheet = true }
                                            ),
                                            DropdownItem(
                                                "导出",
                                                icon = { mod -> Icon(Lucide.Download, null, modifier = mod) },
                                                onClick = {
                                                    pendingExportIsJson = false
                                                    pendingExportContent = null
                                                    showExportSheet = true
                                                }
                                            ),
                                            DropdownItem(
                                                "编辑",
                                                icon = { mod -> Icon(Lucide.Pencil, null, modifier = mod) },
                                                onClick = { showEditSheet = true }
                                            )
                                        )
                                    )
                                ),
                                minHeight = 40.dp,
                                minWidth = 40.dp,
                            ) {
                                Icon(
                                    imageVector = Lucide.EllipsisVertical,
                                    contentDescription = "更多"
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            val playBarHeight = LocalGlobalPlayBarHeight.current
            Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + playBarHeight + 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
                        // 封面 + 右侧：名称 / 介绍 / 创建时间
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlaylistCover(
                                songIds = songs.take(3).map { it.id },
                                coverPath = playlist?.coverPath,
                                coverSource = playlist?.coverSource ?: 0,
                                modifier = Modifier.size(96.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist?.name ?: "歌单",
                                    style = MiuixTheme.textStyles.title3,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                playlist?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = desc,
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "创建于 ${formatPlaylistDate(playlist?.createdAt ?: 0L)}",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 封面下方统计：数量 / 总时长 / 播放次数
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PlaylistStatItem(
                                value = "${songs.size}",
                                label = "首歌曲",
                                modifier = Modifier.weight(1f)
                            )
                            PlaylistStatItem(
                                value = formatPlaylistDuration(totalDuration),
                                label = "总时长",
                                modifier = Modifier.weight(1f)
                            )
                            PlaylistStatItem(
                                value = "${playlist?.playCount ?: 0}",
                                label = "播放次数",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                if (songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "歌单是空的\n去歌曲菜单里选择「加入歌单」",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                style = MiuixTheme.textStyles.body2
                            )
                        }
                    }
                }
                itemsIndexed(displaySongs, key = { _, song -> song.id }) { index, song ->
                    val isDragging = song.id == draggingSongId
                    FhListItem(
                        title = song.title,
                        summary = "${song.artist} - ${song.album}",
                        modifier = if (isCustomSort && !multiSelectMode) {
                            Modifier
                                .animateItem()
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                                .shadow(if (isDragging) 10.dp else 0.dp, RoundedCornerShape(12.dp))
                                .pointerInput(song.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingSongId = song.id
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val info = listState.layoutInfo
                                            val draggingItem = info.visibleItemsInfo
                                                .firstOrNull { it.key == song.id }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val step = draggingItem.size + dragSpacingPx
                                            val currentIdx = latestDisplaySongs
                                                .indexOfFirst { it.id == song.id }
                                            val target = ((draggingItem.offset + dragOffset) / step)
                                                .roundToInt()
                                                .coerceIn(0, (latestDisplaySongs.size - 1).coerceAtLeast(0))
                                            if (target != currentIdx) {
                                                displaySongs = latestDisplaySongs.toMutableList().apply {
                                                    add(target, removeAt(currentIdx))
                                                }
                                                dragOffset -= (target - currentIdx) * step
                                            }
                                        },
                                        onDragEnd = {
                                            viewModel.reorderSongs(
                                                playlistId,
                                                displaySongs.map { it.id }
                                            )
                                            draggingSongId = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggingSongId = null
                                            dragOffset = 0f
                                        }
                                    )
                                }
                        } else Modifier,
                        onClick = {
                            if (multiSelectMode) {
                                if (song.id in selectedSongIds) {
                                    selectedSongIds.remove(song.id)
                                } else {
                                    selectedSongIds.add(song.id)
                                }
                            } else {
                                viewModel.recordPlay(playlistId)
                                playerViewModel.playPlaylistSongs(
                                    songs,
                                    songs.indexOf(song).coerceAtLeast(0),
                                    playlist?.defaultPlayMode ?: 0
                                )
                            }
                        },
                        leading = {
                            SongCoverImage(
                                songId = song.id,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        trailing = {
                            if (multiSelectMode) {
                                // 多选勾选指示
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (song.id in selectedSongIds) MiuixTheme.colorScheme.primary
                                            else MiuixTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (song.id in selectedSongIds) {
                                        Icon(
                                            imageVector = Lucide.Check,
                                            contentDescription = "已选择",
                                            modifier = Modifier.size(14.dp),
                                            tint = MiuixTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = { menuSong = song }) {
                                    Icon(
                                        imageVector = Lucide.EllipsisVertical,
                                        contentDescription = "更多",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
            // 滚动条：自动淡入淡出，可拖动定位
            LazyListScrollBar(
                listState = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
            }

            // 多选底部工具栏（悬浮）
            if (multiSelectMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    MultiSelectToolbar(
                        selectedCount = selectedSongIds.size,
                        backdrop = backdrop,
                        onAddToPlaylist = {
                            if (selectedSongIds.isNotEmpty()) showBatchAddSheet = true
                        },
                        onAddToQueue = {
                            val songs = displaySongs.filter { it.id in selectedSongIds }
                            if (songs.isNotEmpty()) {
                                playerViewModel.addToQueue(songs)
                                multiSelectMode = false
                                selectedSongIds.clear()
                            }
                        },
                        onShare = {
                            val songs = displaySongs.filter { it.id in selectedSongIds }
                            if (songs.isNotEmpty()) {
                                cn.lemondrop.fhreborn.util.SongFileUtils.shareSongs(context, songs)
                                multiSelectMode = false
                                selectedSongIds.clear()
                            }
                        },
                        onDelete = {
                            if (selectedSongIds.isNotEmpty()) showBatchDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }

    // 歌曲更多菜单：完整曲目菜单 + 歌单上下文"从歌单移除"
    menuSong?.let { song ->
        SongMenuSheet(
            song = song,
            onDismiss = { menuSong = null },
            onPlayNext = { /* TODO: 加入播放队列下一首 */ },
            onAddToPlaylist = {
                addTargetSong = song
                menuSong = null
                showAddToPlaylistSheet = true
            },
            onThoughts = { /* TODO: 想法 */ },
            onViewAlbum = { onNavigateToAlbum(song.album, song.albumArtist ?: song.artist) },
            onViewArtist = { onNavigateToArtist(song.artist) },
            onGoToFolder = { /* TODO: 转至文件夹 */ },
            onShare = { cn.lemondrop.fhreborn.util.SongFileUtils.shareSong(context, song) },
            onOpenWith = { cn.lemondrop.fhreborn.util.SongFileUtils.openWithOtherApp(context, song) },
            onProperties = {
                propertiesSong = song
                menuSong = null
                showSongProperties = true
            },
            onHide = { /* TODO: 隐藏音乐 */ },
            onDelete = { /* TODO: 删除文件 */ },
            onRemoveFromPlaylist = {
                viewModel.removeSong(playlistId, song.id)
                menuSong = null
            }
        )
    }

    // 歌曲属性弹窗（菜单"属性"）
    if (showSongProperties) {
        BackHandler { showSongProperties = false }
        propertiesSong?.let { song ->
            cn.lemondrop.fhreborn.util.SongFileUtils.SongPropertiesDialog(
                song = song,
                onDismiss = { showSongProperties = false }
            )
        }
    }

    // 加入歌单弹窗（菜单"加入歌单"）
    if (showAddToPlaylistSheet) {
        BackHandler { showAddToPlaylistSheet = false }
        addTargetSong?.let { song ->
            AddToPlaylistSheet(
                songIds = listOf(song.id),
                viewModel = viewModel,
                onDismiss = { showAddToPlaylistSheet = false }
            )
        }
    }

    // 批量加入歌单弹窗（多选工具栏）
    if (showBatchAddSheet) {
        BackHandler { showBatchAddSheet = false }
        AddToPlaylistSheet(
            songIds = selectedSongIds.toList(),
            viewModel = viewModel,
            onDismiss = {
                showBatchAddSheet = false
                multiSelectMode = false
                selectedSongIds.clear()
            }
        )
    }

    // 批量删除确认（多选工具栏）
    if (showBatchDeleteConfirm) {
        BackHandler { showBatchDeleteConfirm = false }
        FhBottomSheet(
            show = true,
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = "删除歌曲",
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    text = "确定删除选中的 ${selectedSongIds.size} 首歌曲吗？文件将从设备中移除，此操作不可恢复。",
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
                            val songs = displaySongs.filter { it.id in selectedSongIds }
                            showBatchDeleteConfirm = false
                            libraryViewModel.deleteSongs(context, songs) { deleted ->
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar("已删除 $deleted 首歌曲")
                                }
                            }
                            multiSelectMode = false
                            selectedSongIds.clear()
                        }
                    )
                }
            }
        }
    }

    // 排序弹窗
    if (showSortSheet) {
        PlaylistSortSheet(
            currentSortType = playlist?.sortType ?: 0,
            onDismiss = { showSortSheet = false },
            onSelect = { type ->
                viewModel.setSortType(playlistId, type)
                showSortSheet = false
            }
        )
    }

    // 导出弹窗
    if (showExportSheet) {
        FhBottomSheet(
            show = true,
            onDismissRequest = { showExportSheet = false },
            title = "导出歌单",
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer
        ) {
            FhListItem(
                title = "导出为 M3U",
                summary = "路径列表，适合同设备恢复",
                onClick = {
                    showExportSheet = false
                    viewModel.exportPlaylistM3U(playlistId) { content ->
                        pendingExportContent = content
                        pendingExportIsJson = false
                        exportLauncher.launch("${playlist?.name ?: "playlist"}.m3u8")
                    }
                }
            )
            FhListItem(
                title = "导出为 JSON",
                summary = "含完整元数据，支持跨设备导入",
                onClick = {
                    showExportSheet = false
                    viewModel.exportPlaylistJson(playlistId) { content ->
                        pendingExportContent = content
                        pendingExportIsJson = true
                        exportLauncher.launch("${playlist?.name ?: "playlist"}.json")
                    }
                }
            )
        }
    }

    // 导出/导入结果提示（避让悬浮播放条；多选时播放条已隐藏，无需避让）
    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (multiSelectMode) 0.dp else LocalGlobalPlayBarHeight.current + 8.dp)
        )
    }

    // 编辑歌单（含默认播放模式 + 封面）
    if (showEditSheet) {
        PlaylistEditSheet(
            title = "编辑歌单",
            initialName = playlist?.name ?: "",
            initialDescription = playlist?.description.orEmpty(),
            initialPlayMode = playlist?.defaultPlayMode ?: 0,
            initialCoverPath = playlist?.coverPath,
            initialCoverSource = playlist?.coverSource ?: 0,
            coverSongs = songs,
            onDismiss = { showEditSheet = false },
            onSave = { name, desc, playMode, coverPath, coverSource ->
                viewModel.updatePlaylist(playlistId, name, desc, playMode)
                viewModel.setCover(playlistId, coverPath, coverSource)
                showEditSheet = false
            }
        )
    }
}

@Composable
private fun PlaylistStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.headline2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

private fun formatPlaylistDate(ts: Long): String {
    if (ts <= 0) return "-"
    return java.text.SimpleDateFormat(
        "yyyy年M月d日", java.util.Locale.getDefault()
    ).format(java.util.Date(ts))
}

private fun formatPlaylistDuration(ms: Long): String {
    val minutes = ms / 60000
    val hours = minutes / 60
    return if (hours > 0) "${hours}小时${minutes % 60}分" else "${minutes}分钟"
}

@Composable
private fun PlaylistSortSheet(
    currentSortType: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val options = listOf(
        cn.lemondrop.fhreborn.data.db.entity.PlaylistSortType.CUSTOM to "自定义（添加顺序）",
        cn.lemondrop.fhreborn.data.db.entity.PlaylistSortType.TITLE to "标题",
        cn.lemondrop.fhreborn.data.db.entity.PlaylistSortType.ARTIST to "艺术家",
        cn.lemondrop.fhreborn.data.db.entity.PlaylistSortType.ALBUM to "专辑",
        cn.lemondrop.fhreborn.data.db.entity.PlaylistSortType.DURATION to "时长"
    )
    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        title = "歌单排序",
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        options.forEach { (type, label) ->
            val isSelected = type == currentSortType
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelect(type)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 固定前导宽度：选中显示对号，未选中留空占位，文字始终对齐
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = "已选择",
                            modifier = Modifier.size(18.dp),
                            tint = MiuixTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MiuixTheme.textStyles.body1,
                    color = if (isSelected) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurface
                )
            }
        }
    }
}
