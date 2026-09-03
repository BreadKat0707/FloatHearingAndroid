package cn.lemondrop.fhreborn.ui.screens.library

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.LocalDrawerToggle
import cn.lemondrop.fhreborn.LocalDrawerVisible
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import cn.lemondrop.fhreborn.LocalPlayBarOverride
import cn.lemondrop.fhreborn.Screen
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.scanner.ScanProgress
import cn.lemondrop.fhreborn.ui.components.AddToPlaylistSheet
import cn.lemondrop.fhreborn.ui.components.FhListItem
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import cn.lemondrop.fhreborn.ui.components.SongMenuSheet
import cn.lemondrop.fhreborn.ui.components.SortSheet
import cn.lemondrop.fhreborn.ui.theme.BlurNavigationBar
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.util.ArtistSplitter
import cn.lemondrop.fhreborn.util.PermissionUtils
import com.composables.icons.lucide.DiscAlbum
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.LayoutList
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.LayoutList
import com.composables.icons.lucide.ListChecks
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.X
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Repeat1
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Shuffle
import cn.lemondrop.fhreborn.ui.viewmodel.SortField
import cn.lemondrop.fhreborn.ui.viewmodel.SortOrder
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import cn.lemondrop.fhreborn.ui.components.LayoutStyleSheet
import cn.lemondrop.fhreborn.ui.components.MultiSelectToolbar
import cn.lemondrop.fhreborn.ui.components.SelectionIndicator
import cn.lemondrop.fhreborn.ui.components.responsiveColumnCount
import cn.lemondrop.fhreborn.ui.components.SelectionStateButton

@Composable
fun LibraryScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // 背景类型：图片背景时搜索框等控件改用半透明色
    val bgRepo = remember { cn.lemondrop.fhreborn.data.repository.AppSettingsRepository(context) }
    val bgType by bgRepo.bgType.collectAsState(initial = "color")
    val isImageBackground = bgType == "image"
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(context.applicationContext as Application)
    )
    val playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModel.Factory(context.applicationContext as Application)
    )

    val songs by viewModel.songs.collectAsState(initial = emptyList())
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val hiddenFolders by viewModel.hiddenFolders.collectAsState(initial = emptySet())
    val currentSong by playerViewModel.currentSong.collectAsState()

    // 启动时自动快速刷新音乐库（仅 MediaStore，需要存储/音频权限）
    val hasStoragePermission = PermissionUtils.hasStoragePermission(context)
    LaunchedEffect(hasStoragePermission) {
        viewModel.autoScanIfNeeded(hasStoragePermission)
    }

    // 播放模式状态（歌曲页搜索栏旁的循环/随机按钮）
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val shuffleMode by playerViewModel.shuffleMode.collectAsState()

    // 当前 tab 保存：从专辑/艺术家等 tab 进入二级页返回后仍停留原 tab
    var selectedNavIndex by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var showSongMenu by remember { mutableStateOf(false) }
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showAlbumLayoutSheet by remember { mutableStateOf(false) }
    val menuScope = rememberCoroutineScope()

    // 各 tab 的排序选项（歌曲全字段 / 专辑标题·艺术家·年份 / 文件夹路径·名称）
    val sortOptions = when (selectedNavIndex) {
        0 -> listOf(
            SortField.TITLE to "标题",
            SortField.ARTIST_ALBUM to "艺术家 - 专辑",
            SortField.ALBUM_DISC_TRACK to "专辑 - 碟号 - 音轨号",
            SortField.MODIFIED_TIME to "修改时间",
            SortField.ADDED_TIME to "添加时间",
            SortField.PLAY_COUNT to "播放次数",
            SortField.PATH_FILENAME to "路径 - 文件名",
            SortField.FILE_NAME to "文件名",
            SortField.RELEASE_YEAR to "发行时间",
            SortField.DURATION to "曲目时长"
        )
        1 -> listOf(
            SortField.TITLE to "标题",
            SortField.ARTIST_ALBUM to "艺术家",
            SortField.RELEASE_YEAR to "发行年份"
        )
        3 -> listOf(
            SortField.FOLDER_PATH to "路径",
            SortField.FOLDER_NAME to "文件夹名称"
        )
        else -> emptyList()
    }

    // 专辑视图样式（list / grid / card / square，与歌单页一致，持久化）
    val albumViewStyle by bgRepo.albumViewStyle.collectAsState(initial = "grid")

    // 专辑排序：标题 / 艺术家 / 发行年份（仅专辑 tab 应用）
    val sortedAlbums = remember(albums, sortField, sortOrder) {
        val sorted = when (sortField) {
            SortField.ARTIST_ALBUM -> albums.sortedWith(
                compareBy<cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel.Album> { it.artist.lowercase() }
                    .thenBy { it.name.lowercase() }
            )
            SortField.RELEASE_YEAR -> albums.sortedBy {
                it.songs.firstNotNullOfOrNull { s -> s.year } ?: 0
            }
            else -> albums.sortedBy { it.name.lowercase() }
        }
        if (sortOrder == SortOrder.DESC) sorted.reversed() else sorted
    }

    var showArtistChooser by remember { mutableStateOf(false) }
    var showSongProperties by remember { mutableStateOf(false) }

    // 多选模式（批量操作）
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateSetOf<Long>() }
    var showBatchAddSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingLocateSongId by remember { mutableStateOf<Long?>(null) }

    // 多选时隐藏全局播放条（底部由多选工具栏接管）；离开页面时复位
    val playBarOverride = LocalPlayBarOverride.current
    LaunchedEffect(multiSelectMode) {
        playBarOverride.value = multiSelectMode
    }
    DisposableEffect(Unit) {
        onDispose { playBarOverride.value = false }
    }

    val displaySongs = if (searchQuery.isNotBlank()) searchResults else songs

    // 系统 insets

    // 抽屉（侧边栏）可见状态
    val drawerVisible = LocalDrawerVisible.current
    val drawerToggle = LocalDrawerToggle.current

    // 底部控件高度
    val miniPlayBarHeight = LocalGlobalPlayBarHeight.current
    val navBarHeight = 64.dp

    // 刷新完成 Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.refreshCompleted.collect { count ->
            snackbarScope.launch {
                snackbarHostState.showSnackbar("刷新完成，扫描到 $count 首歌曲")
            }
        }
    }

    // 滚动位置按 tab 保存：切 tab / 离开页面返回后恢复
    val listState = rememberSaveable(
        selectedNavIndex,
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) { androidx.compose.foundation.lazy.LazyListState() }

    // 手动滚动位置兜底：LazyListState.Saver 的恢复值在数据异步加载期间会被
    // 空列表 clamp 丢失，故按 tab 数值保存，数据就绪后再恢复
    var savedScrollIndex by rememberSaveable(selectedNavIndex) { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable(selectedNavIndex) { mutableIntStateOf(0) }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.drop(1).collect { (index, offset) ->
            savedScrollIndex = index
            savedScrollOffset = offset
        }
    }
    LaunchedEffect(selectedNavIndex, displaySongs, albums, artists) {
        val dataReady = when (selectedNavIndex) {
            1 -> albums.isNotEmpty()
            2 -> artists.isNotEmpty()
            else -> displaySongs.isNotEmpty()
        }
        if (dataReady && savedScrollIndex > 0) {
            listState.scrollToItem(savedScrollIndex, savedScrollOffset)
        }
    }
    // 顶栏滚动感知：列表滚离顶部时显示背景/模糊，回顶隐藏
    val topBarScrolled = remember {
        androidx.compose.runtime.derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }.value

    // 定位当前播放：切到"歌曲"标签后滚动到对应项
    LaunchedEffect(pendingLocateSongId, displaySongs) {
        val targetId = pendingLocateSongId ?: return@LaunchedEffect
        val index = displaySongs.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            val headerCount = (if (selectedNavIndex == 0) 1 else 0) + 1
            listState.animateScrollToItem(headerCount + index)
            pendingLocateSongId = null
        }
    }

    val navItems = remember {
        listOf(
            "歌曲" to Lucide.Music,
            "专辑" to Lucide.DiscAlbum,
            "艺术家" to Lucide.UserRound,
            "文件夹" to Lucide.FolderOpen
        )
    }

    val titleText: @Composable () -> Unit = {
        Text(
            text = when (selectedNavIndex) {
                0 -> "媒体库"
                1 -> "专辑"
                2 -> "艺术家"
                3 -> "文件夹"
                else -> "媒体库"
            },
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface
        )
    }

    val libraryBody: @Composable (PaddingValues, Dp) -> Unit = { contentPadding, bottomSpacer ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 专辑网格响应式列数：按内容区实际宽度计算（侧边栏收折/窗口拉伸自适应），手机保持 2 列
        val albumColumns = responsiveColumnCount(maxWidth, minItemWidthDp = 170, minColumns = 2)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 歌曲 tab：搜索框（自绘，图片背景时可透明）+ 播放模式按钮（已从标题栏移入页面）
            if (selectedNavIndex == 0) {
                item {
                    LibrarySearchField(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        label = "搜索",
                        transparent = isImageBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 顺序循环：立即按列表循环播放全部歌曲（命令按钮，等宽均分与搜索框对齐）
                        FhPlayModeButton(
                            icon = Lucide.Repeat,
                            label = "顺序循环",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                playerViewModel.setShuffle(false)
                                playerViewModel.playSongs(displaySongs, 0)
                            }
                        )
                        // 随机循环：立即随机播放全部歌曲（随机选一首作为起始，不从头开始）
                        FhPlayModeButton(
                            icon = Lucide.Shuffle,
                            label = "随机循环",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                playerViewModel.setShuffle(true)
                                val startIndex = if (displaySongs.isEmpty()) 0 else kotlin.random.Random.nextInt(displaySongs.size)
                                playerViewModel.playSongs(displaySongs, startIndex)
                            }
                        )
                    }
                }
            }

            // 扫描状态
            when (val progress = scanProgress) {
                is ScanProgress.Scanning -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 扫描状态指示
                        Icon(
                                imageVector = Lucide.RotateCcw,
                                contentDescription = "扫描中",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                is ScanProgress.Error -> {
                    item {
                        Text(
                            text = "扫描出错: ${progress.message}",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {}
            }

            // 根据导航标签显示不同内容
            when (selectedNavIndex) {
                0 -> SongsContent(
                    songs = displaySongs,
                    selectionMode = multiSelectMode,
                    selectedSongIds = selectedSongIds,
                    onSongClick = { song ->
                        playerViewModel.playSongs(displaySongs, displaySongs.indexOf(song))
                    },
                    onMoreClick = { song ->
                        menuSong = song
                        showSongMenu = true
                    }
                )
                1 -> AlbumsContent(
                    albums = sortedAlbums,
                    columns = albumColumns,
                    viewStyle = albumViewStyle,
                    selectionMode = multiSelectMode,
                    selectedSongIds = selectedSongIds,
                    onAlbumClick = { album ->
                        onNavigate(Screen.AlbumDetail.createRoute(album.name, album.artist))
                    }
                )
                2 -> ArtistsContent(
                    artists = artists,
                    allSongs = viewModel.songs.value,
                    selectionMode = multiSelectMode,
                    selectedSongIds = selectedSongIds,
                    onArtistClick = { artist ->
                        onNavigate(Screen.ArtistDetail.createRoute(artist.name))
                    }
                )
                3 -> FoldersContent(
                    songs = displaySongs,
                    hiddenFolders = hiddenFolders,
                    sortField = sortField,
                    sortOrder = sortOrder,
                    selectionMode = multiSelectMode,
                    selectedSongIds = selectedSongIds,
                    onFolderClick = { folderPath ->
                        onNavigate(Screen.FolderDetail.createRoute(folderPath))
                    },
                    onHideFolder = { viewModel.hideFolder(it) }
                )
            }

            // 底部占位，让内容可以滚动到播放器/底部栏下方
            item {
                Spacer(modifier = Modifier.height(bottomSpacer))
            }
        }
        // 滚动条：自动淡入淡出，可拖动定位（跳过标题栏/底部栏区域）
        LazyListScrollBar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd),
            trackPadding = contentPadding
        )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 层背景：顶栏/底栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
        val surfaceColor = MiuixTheme.colorScheme.surface
        val backdrop = rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            BlurTopBar(
                backdrop = backdrop,
                // 列表滚动后（或多选时）才显示顶栏背景/模糊；回顶隐藏
                scrolled = topBarScrolled || multiSelectMode,
                title = if (multiSelectMode) "已选 ${selectedSongIds.size} 首" else when (selectedNavIndex) {
                    0 -> "媒体库"
                    1 -> "专辑"
                    2 -> "艺术家"
                    3 -> "文件夹"
                    else -> "媒体库"
                },
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
                        IconButton(onClick = { drawerToggle() }) {
                            Icon(
                                imageVector = Lucide.Menu,
                                contentDescription = "菜单"
                            )
                        }
                    }
                },
                actions = {
                    if (multiSelectMode) {
                        // 选择状态：全选 / 选中多个 / 全未选（点击切换全选）
                        val all = when (selectedNavIndex) {
                            0 -> displaySongs.map { it.id }
                            1 -> albums.flatMap { it.songs }.map { it.id }
                            2 -> artists.flatMap { artist ->
                                viewModel.songs.value.filter { it.artist == artist.name }.map { it.id }
                            }
                            3 -> displaySongs.filterNot { song ->
                                hiddenFolders.any { h -> song.path.startsWith(h) }
                            }.map { it.id }
                            else -> emptyList()
                        }
                        SelectionStateButton(
                            selectedCount = selectedSongIds.size,
                            totalCount = all.distinct().size,
                            onSelectAll = {
                                selectedSongIds.clear()
                                selectedSongIds.addAll(all)
                            },
                            onDeselectAll = { selectedSongIds.clear() }
                        )
                    } else {
                        // 菜单按 tab 独立：歌曲/专辑/艺术家/文件夹各显示自己的项（单组无分割线）
                        val menuEntries = listOf(
                            DropdownEntry(
                                items = buildList {
                                    add(DropdownItem("刷新", icon = { mod -> Icon(Lucide.RotateCcw, null, modifier = mod) }, onClick = { viewModel.refreshMediaStore() }))
                                    if (sortOptions.isNotEmpty()) {
                                        add(DropdownItem("排序", icon = { mod -> Icon(Lucide.ArrowUpDown, null, modifier = mod) }, onClick = { showSortSheet = true }))
                                    }
                                    // 专辑：列表布局弹窗选择（miuix 无级联子菜单，用弹窗替代）
                                    if (selectedNavIndex == 1) {
                                        add(DropdownItem(
                                            "列表布局",
                                            icon = { mod -> Icon(Lucide.LayoutList, null, modifier = mod) },
                                            onClick = { showAlbumLayoutSheet = true }
                                        ))
                                    }
                                    // 文件夹：查看隐藏的文件夹 → 二级页面
                                    if (selectedNavIndex == 3) {
                                        add(DropdownItem(
                                            "查看隐藏的文件夹",
                                            icon = { mod -> Icon(Lucide.EyeOff, null, modifier = mod) },
                                            onClick = { onNavigate(Screen.HiddenFolders.createRoute()) }
                                        ))
                                    }
                                    add(DropdownItem("多选", icon = { mod -> Icon(Lucide.ListChecks, null, modifier = mod) }, onClick = {
                                        multiSelectMode = true
                                        selectedSongIds.clear()
                                    }))
                                    add(DropdownItem("回到顶部", icon = { mod -> Icon(Lucide.ArrowUp, null, modifier = mod) }, onClick = {
                                        menuScope.launch { listState.animateScrollToItem(0) }
                                    }))
                                    // 定位当前播放：仅歌曲 tab（播放列表即歌曲列表）
                                    if (selectedNavIndex == 0) {
                                        add(DropdownItem("定位当前播放", icon = { mod -> Icon(Lucide.MapPin, null, modifier = mod) }, onClick = {
                                            currentSong?.let { song ->
                                                selectedNavIndex = 0
                                                pendingLocateSongId = song.id
                                            }
                                        }))
                                    }
                                }
                            )
                        )
                        OverlayIconDropdownMenu(
                            entries = menuEntries,
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
        },
        bottomBar = {
            if (multiSelectMode) {
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
                        if (selectedSongIds.isNotEmpty()) showDeleteConfirm = true
                    }
                )
            } else {
                BlurNavigationBar(
                    backdrop = backdrop,
                ) {
                    navItems.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedNavIndex == index,
                            onClick = { selectedNavIndex = index },
                            icon = icon,
                            label = label
                        )
                    }
                }
            }
        }
    ) { padding ->
        // 排序弹窗
        if (showSortSheet) {
            BackHandler { showSortSheet = false }
            SortSheet(
                options = sortOptions,
                currentField = sortField,
                currentOrder = sortOrder,
                onDismiss = { showSortSheet = false },
                onSelectField = { viewModel.setSortField(it) },
                onToggleOrder = { viewModel.toggleSortOrder() }
            )
        }

        // 专辑列表布局弹窗
        if (showAlbumLayoutSheet) {
            BackHandler { showAlbumLayoutSheet = false }
            LayoutStyleSheet(
                title = "列表布局",
                options = listOf(
                    "list" to "列表",
                    "grid" to "双栏列表",
                    "card" to "卡片",
                    "square" to "方形"
                ),
                currentStyle = albumViewStyle,
                onSelect = { style -> menuScope.launch { bgRepo.setAlbumViewStyle(style) } },
                onDismiss = { showAlbumLayoutSheet = false }
            )
        }

        // 歌曲上下文菜单
        if (showSongMenu && menuSong != null) {
            BackHandler { showSongMenu = false }
            SongMenuSheet(
                song = menuSong!!,
                onDismiss = { showSongMenu = false },
                onPlayNext = {
                    menuSong?.let { playerViewModel.playNext(listOf(it)) }
                },
                onAddToPlaylist = {
                    showSongMenu = false
                    showAddToPlaylist = true
                },
                onViewAlbum = {
                    menuSong?.let { s ->
                        onNavigate(Screen.AlbumDetail.createRoute(s.album, s.albumArtist))
                    }
                },
                onViewArtist = {
                    menuSong?.let { s ->
                        val separators = viewModel.artistSeparators.value
                        val artistList = ArtistSplitter.split(s.artist, separators)
                        if (artistList.size == 1) {
                            showSongMenu = false
                            onNavigate(Screen.ArtistDetail.createRoute(artistList.first()))
                        } else if (artistList.isNotEmpty()) {
                            showSongMenu = false
                            showArtistChooser = true
                        }
                    }
                },
                onGoToFolder = {
                    // TODO: 转至文件夹
                },
                onShare = {
                    menuSong?.let { s ->
                        cn.lemondrop.fhreborn.util.SongFileUtils.shareSong(context, s)
                    }
                },
                onOpenWith = {
                    menuSong?.let { s ->
                        cn.lemondrop.fhreborn.util.SongFileUtils.openWithOtherApp(context, s)
                    }
                },
                onProperties = {
                    menuSong?.let { s ->
                        showSongProperties = true
                    }
                },
                onDelete = {
                    // TODO: 删除文件
                }
            )
        }

        // 多艺术家选择器
        if (showArtistChooser && menuSong != null) {
            val separators = viewModel.artistSeparators.value
            val artistList = remember(menuSong, separators) {
                ArtistSplitter.split(menuSong!!.artist, separators)
            }
            BackHandler { showArtistChooser = false }
            FhBottomSheet(
                show = true,
                onDismissRequest = { showArtistChooser = false },
                title = "选择艺术家",
                backgroundColor = MiuixTheme.colorScheme.surfaceContainer
            ) {
                Column {
                    artistList.forEach { artist ->
                        FhListItem(
                            title = artist,
                            onClick = {
                                showArtistChooser = false
                                onNavigate(Screen.ArtistDetail.createRoute(artist))
                            }
                        )
                    }
                }
            }
        }

    // 加入歌单弹窗
    if (showAddToPlaylist && menuSong != null) {
        AddToPlaylistSheet(
            songIds = listOf(menuSong!!.id),
            viewModel = playlistViewModel,
            onDismiss = { showAddToPlaylist = false }
        )
    }

    // 歌曲属性弹窗
    if (showSongProperties && menuSong != null) {
            BackHandler { showSongProperties = false }
            cn.lemondrop.fhreborn.util.SongFileUtils.SongPropertiesDialog(
                song = menuSong!!,
                onDismiss = { showSongProperties = false }
            )
        }

        // 主内容（捕获进 backdrop 层，供顶栏/底栏模糊）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
        libraryBody(
            padding,
            miniPlayBarHeight
        )

        // 刷新完成提示（悬浮于内容区底部；非多选时避让悬浮播放条）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SnackbarHost(
                state = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (multiSelectMode) 0.dp else LocalGlobalPlayBarHeight.current + 8.dp)
            )
        }
        }

        // 多选时返回键退出多选（弹窗的 BackHandler 更晚组合，优先处理）
        if (multiSelectMode) {
            BackHandler {
                multiSelectMode = false
                selectedSongIds.clear()
            }
        }

        // 批量加入歌单弹窗
        if (showBatchAddSheet) {
            BackHandler {
                showBatchAddSheet = false
            }
            AddToPlaylistSheet(
                songIds = selectedSongIds.toList(),
                viewModel = playlistViewModel,
                onDismiss = {
                    showBatchAddSheet = false
                    multiSelectMode = false
                    selectedSongIds.clear()
                }
            )
        }

        // 删除确认弹窗
        if (showDeleteConfirm) {
            BackHandler { showDeleteConfirm = false }
            FhBottomSheet(
                show = true,
                onDismissRequest = { showDeleteConfirm = false },
                title = "删除歌曲",
                backgroundColor = MiuixTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "确定删除选中的 ${selectedSongIds.size} 首歌曲吗？文件将从设备中移除，此操作不可恢复。",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(text = "取消", onClick = { showDeleteConfirm = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            text = "删除",
                            onClick = {
                                val songs = displaySongs.filter { it.id in selectedSongIds }
                                showDeleteConfirm = false
                                viewModel.deleteSongs(context, songs) { deleted ->
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
    }
        }
    }

}

/** 搜索输入框：图片背景时半透明胶囊，否则实色（替代 miuix InputField，其背景写死不可调） */
@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    transparent: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(45.dp)
            .clip(CircleShape)
            .background(
                if (transparent) MiuixTheme.colorScheme.surface.copy(alpha = 0.5f)
                else MiuixTheme.colorScheme.surfaceContainerHigh
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MiuixTheme.textStyles.body1.copy(color = MiuixTheme.colorScheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MiuixTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                if (query.isEmpty()) {
                    Text(
                        text = label,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        }
    }
}

// ===== 内容区域 =====

private fun androidx.compose.foundation.lazy.LazyListScope.SongsContent(
    songs: List<Song>,
    selectionMode: Boolean = false,
    selectedSongIds: SnapshotStateSet<Long>? = null,
    onSongClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit
) {
    item {
        Text(
            text = "${songs.size} 首歌曲",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    items(songs, key = { it.id }) { song ->
        SongItem(
            song = song,
            selectionMode = selectionMode,
            selected = selectedSongIds?.contains(song.id) == true,
            onToggleSelect = {
                if (selectedSongIds?.contains(song.id) == true) {
                    selectedSongIds.remove(song.id)
                } else {
                    selectedSongIds?.add(song.id)
                }
            },
            onClick = { onSongClick(song) },
            onMoreClick = { onMoreClick(song) }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.AlbumsContent(
    albums: List<cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel.Album>,
    columns: Int,
    viewStyle: String,
    selectionMode: Boolean = false,
    selectedSongIds: SnapshotStateSet<Long>? = null,
    onAlbumClick: (cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel.Album) -> Unit
) {
    item {
        Text(
            text = "${albums.size} 张专辑",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    fun isSelected(album: cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel.Album): Boolean =
        selectedSongIds?.let { sel ->
            album.songs.isNotEmpty() && album.songs.all { it.id in sel }
        } == true

    fun onAlbumToggle(album: cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel.Album) {
        if (selectionMode && selectedSongIds != null) {
            val ids = album.songs.map { it.id }
            val allSelected = ids.isNotEmpty() && ids.all { it in selectedSongIds }
            if (allSelected) {
                ids.forEach { selectedSongIds.remove(it) }
            } else {
                selectedSongIds.addAll(ids)
            }
        } else {
            onAlbumClick(album)
        }
    }

    fun albumKey(album: cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel.Album) = album.name + "#" + album.artist

    when (viewStyle) {
        // 列表：整行封面 + 文字，选择标志右中（对齐歌单列表）
        "list" -> {
            items(albums, key = { albumKey(it) }) { album ->
                AlbumListRow(
                    album = album,
                    selectionMode = selectionMode,
                    selected = isSelected(album),
                    onClick = { onAlbumToggle(album) }
                )
            }
        }
        // 卡片：Card 大封面 + 文字，选择标志右上
        "card" -> {
            val rows = albums.chunked(columns)
            items(rows.size, key = { albumKey(rows[it].first()) }) { index ->
                val rowAlbums = rows[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowAlbums.forEach { album ->
                        AlbumCardItem(
                            album = album,
                            selectionMode = selectionMode,
                            selected = isSelected(album),
                            onClick = { onAlbumToggle(album) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowAlbums.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        // 方形：方形封面 + 名称/艺术家，选择标志右上（对齐歌单方形）
        "square" -> {
            val rows = albums.chunked(columns)
            items(rows.size, key = { albumKey(rows[it].first()) }) { index ->
                val rowAlbums = rows[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowAlbums.forEach { album ->
                        AlbumItem(
                            album = album,
                            selectionMode = selectionMode,
                            selected = isSelected(album),
                            onClick = { onAlbumToggle(album) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowAlbums.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        // 双栏列表：横向紧凑卡片，选择标志右中（对齐歌单双栏列表）
        else -> {
            val rows = albums.chunked(columns)
            items(rows.size, key = { albumKey(rows[it].first()) }) { index ->
                val rowAlbums = rows[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowAlbums.forEach { album ->
                        AlbumGridItem(
                            album = album,
                            selectionMode = selectionMode,
                            selected = isSelected(album),
                            onClick = { onAlbumToggle(album) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowAlbums.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** 双栏列表项：56dp 圆角封面 + 名称/艺术家（仿歌单双栏列表） */
@Composable
private fun AlbumGridItem(
    album: cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel.Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SongCoverImage(
                songId = album.coverSongId,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selectionMode) {
            // 多选标志统一右侧垂直居中（对齐歌单双栏列表）
            SelectionIndicator(
                selected = selected,
                modifier = Modifier.align(Alignment.CenterEnd).size(20.dp)
            )
        }
    }
}

/** 列表项：48dp 圆角封面 + 名称/艺术家（仿歌单列表） */
@Composable
private fun AlbumListRow(
    album: cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel.Album,
    onClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongCoverImage(
            songId = album.coverSongId,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selectionMode) {
            // 多选标志统一右侧垂直居中
            SelectionIndicator(
                selected = selected,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** 卡片项：Card 大封面 + 名称/艺术家（仿歌单卡片） */
@Composable
private fun AlbumCardItem(
    album: cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel.Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    // Card 背景圆角不裁切内容，需 clip 整卡让封面顶部贴合圆角（封面本身不加圆角）
    top.yukonga.miuix.kmp.basic.Card(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Sink,
        onClick = onClick,
    ) {
        Box {
            Column(modifier = Modifier.fillMaxWidth()) {
                SongCoverImage(
                    songId = album.coverSongId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        // 卡片视图：封面自身不带圆角，由 Card 裁切上方圆角
                        .clip(RoundedCornerShape(0.dp))
                )
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = album.name,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = album.artist,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selectionMode) {
                // 卡片视图：多选标志保留封面右上角
                SelectionIndicator(
                    selected = selected,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ArtistsContent(
    artists: List<LibraryViewModel.Artist>,
    allSongs: List<Song> = emptyList(),
    selectionMode: Boolean = false,
    selectedSongIds: SnapshotStateSet<Long>? = null,
    onArtistClick: (LibraryViewModel.Artist) -> Unit
) {
    item {
        Text(
            text = "${artists.size} 位艺术家",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    items(artists.size, key = { artists[it].name }) { index ->
        val artist = artists[index]
        ArtistItem(
            artist = artist,
            selectionMode = selectionMode,
            selected = selectedSongIds?.let { sel ->
                val ids = allSongs.filter { it.artist == artist.name }.map { it.id }
                ids.isNotEmpty() && ids.all { it in sel }
            } == true,
            onClick = {
                if (selectionMode && selectedSongIds != null) {
                    val ids = allSongs.filter { it.artist == artist.name }.map { it.id }
                    val allSelected = ids.isNotEmpty() && ids.all { it in selectedSongIds }
                    if (allSelected) {
                        ids.forEach { selectedSongIds.remove(it) }
                    } else {
                        selectedSongIds.addAll(ids)
                    }
                } else {
                    onArtistClick(artist)
                }
            }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.FoldersContent(
    songs: List<Song>,
    hiddenFolders: Set<String>,
    sortField: SortField = SortField.FOLDER_PATH,
    sortOrder: SortOrder = SortOrder.ASC,
    selectionMode: Boolean = false,
    selectedSongIds: SnapshotStateSet<Long>? = null,
    onFolderClick: (String) -> Unit,
    onHideFolder: (String) -> Unit
) {
    // 隐藏的文件夹始终不显示在媒体库
    val visibleSongs = songs.filterNot { song ->
        hiddenFolders.any { hidden -> song.path.startsWith(hidden) }
    }

    if (visibleSongs.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无歌曲", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        return
    }

    val folderMap = visibleSongs.groupBy { it.path.substringBeforeLast('/') }
    // 排序：路径 / 文件夹名称（方向由 SortOrder 控制）
    val sortedFolders = when (sortField) {
        SortField.FOLDER_NAME -> {
            val entries = folderMap.entries.sortedBy { it.key.substringAfterLast('/').lowercase() }
            if (sortOrder == SortOrder.DESC) entries.reversed() else entries
        }
        else -> {
            val entries = folderMap.entries.sortedBy { it.key.lowercase() }
            if (sortOrder == SortOrder.DESC) entries.reversed() else entries
        }
    }

    item {
        Text(
            text = "${sortedFolders.size} 个文件夹",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    sortedFolders.forEach { (folderPath, folderSongs) ->
        item(key = folderPath) {
            var showOptions by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selectionMode && selectedSongIds != null) {
                            val ids = folderSongs.map { it.id }
                            val allSelected = ids.isNotEmpty() && ids.all { it in selectedSongIds }
                            if (allSelected) {
                                ids.forEach { selectedSongIds.remove(it) }
                            } else {
                                selectedSongIds.addAll(ids)
                            }
                        } else {
                            onFolderClick(folderPath)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folderPath.substringAfterLast('/'),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = folderPath,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folderSongs.size} 首歌曲",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                IconButton(onClick = { showOptions = true }) {
                    Icon(
                        imageVector = Lucide.EllipsisVertical,
                        contentDescription = "更多",
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                if (selectionMode && selectedSongIds != null) {
                    val allSelected = folderSongs.isNotEmpty() && folderSongs.all { it.id in selectedSongIds }
                    SelectionIndicator(
                        selected = allSelected,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showOptions) {
                FolderOptionsSheet(
                    folderPath = folderPath,
                    onDismiss = { showOptions = false },
                    onHide = {
                        onHideFolder(folderPath)
                        showOptions = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FolderOptionsSheet(
    folderPath: String,
    onDismiss: () -> Unit,
    onHide: () -> Unit
) {
    FhBottomSheet(
        show = true,
        onDismissRequest = onDismiss,
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onHide()
                    onDismiss()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.EyeOff,
                contentDescription = "在音乐库隐藏",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "在音乐库隐藏",
                style = MiuixTheme.textStyles.body1
            )
        }
    }
}

@Composable
private fun SongItemSmall(song: Song) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = song.title,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatDuration(song.duration),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

// ===== 子组件 =====

@Composable
internal fun SongItem(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null
) {
    if (selectionMode) {
        FhListItem(
            title = song.title,
            summary = "${song.artist} - ${song.album}",
            onClick = { onToggleSelect?.invoke() },
            leading = {
                SongCoverImage(
                    songId = song.id,
                    modifier = Modifier.size(48.dp)
                )
            },
            trailing = {
                SelectionIndicator(
                    selected = selected,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    } else {
        FhListItem(
            title = song.title,
            summary = "${song.artist} - ${song.album}",
            onClick = onClick,
            leading = {
                SongCoverImage(
                    songId = song.id,
                    modifier = Modifier.size(48.dp)
                )
            },
            trailing = {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Lucide.EllipsisVertical,
                        contentDescription = "更多",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        )
    }
}

@Composable
internal fun AlbumItem(
    album: LibraryViewModel.Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            SongCoverImage(
                songId = album.coverSongId,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            )
            if (selectionMode) {
                // 方形视图：多选标志保留封面右上角（对齐歌单方形视图）
                SelectionIndicator(
                    selected = selected,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = album.artist,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ArtistItem(
    artist: LibraryViewModel.Artist,
    onClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MiuixTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = artist.name.take(1).uppercase(),
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.albumCount} 张专辑 · ${artist.songCount} 首歌曲",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selectionMode) {
            SelectionIndicator(
                selected = selected,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// ===== 浏览路径 — 文件管理器式层级浏览 =====

internal data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: MutableMap<String, FileNode> = mutableMapOf(),
    val song: Song? = null
)

internal fun buildFileTree(songs: List<Song>): FileNode {
    val root = FileNode(name = "", path = "", isDirectory = true)
    for (song in songs) {
        val parts = song.path.split('/').filter { it.isNotEmpty() }
        var current = root
        var builtPath = ""
        for (i in parts.indices) {
            val part = parts[i]
            builtPath = if (builtPath.isEmpty()) "/$part" else "$builtPath/$part"
            val isLast = i == parts.lastIndex

            if (!current.children.containsKey(part)) {
                current.children[part] = FileNode(
                    name = part,
                    path = builtPath,
                    isDirectory = !isLast,
                    song = if (isLast) song else null
                )
            }
            current = current.children[part]!!
        }
    }
    return root
}


@Composable
internal fun FileBrowserItemRow(
    item: FileNode,
    onClick: () -> Unit
) {
    FhListItem(
        title = item.name,
        summary = if (!item.isDirectory && item.song != null) {
            "${item.song.artist} · ${formatDuration(item.song.duration)}"
        } else null,
        onClick = onClick,
        leading = {
            Icon(
                imageVector = if (item.isDirectory) Lucide.FolderOpen else Lucide.Music,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        },
        trailing = if (item.isDirectory) {
            {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        } else null
    )
}
/** 播放模式按钮（顺序循环 / 随机循环）：激活时 primary 强调色 */
@Composable
private fun FhPlayModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // 命令按钮：统一强调色（点击立即按该模式播放，非状态开关）
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColorsPrimary()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MiuixTheme.textStyles.body2)
    }
}
