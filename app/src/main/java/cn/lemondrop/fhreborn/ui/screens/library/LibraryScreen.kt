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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
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
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.LayoutList
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.FolderOpen
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
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

    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var showFolderBrowser by remember { mutableStateOf(false) }
    var folderBrowserInitialPath by remember { mutableStateOf(listOf<String>()) }
    var showSongMenu by remember { mutableStateOf(false) }
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

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

    val listState = remember(selectedNavIndex) { androidx.compose.foundation.lazy.LazyListState() }
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
            // 歌曲 tab：搜索框（miuix SearchBar）+ 播放模式按钮（已从标题栏移入页面）
            if (selectedNavIndex == 0) {
                item {
                    SearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        // 去掉内部边距，输入框与下方列表项左对齐（外层 16dp 一致）
                        insideMargin = androidx.compose.ui.unit.DpSize(0.dp, 0.dp),
                        inputField = {
                            InputField(
                                query = searchQuery,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                onSearch = {},
                                expanded = false,
                                onExpandedChange = {},
                                label = "搜索"
                            )
                        },
                        onExpandedChange = {},
                        expanded = false
                    ) { }
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
                                playerViewModel.playSongs(displaySongs, 0)
                            }
                        )
                        // 随机循环：立即随机播放全部歌曲
                        FhPlayModeButton(
                            icon = Lucide.Shuffle,
                            label = "随机循环",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                playerViewModel.setShuffle(true)
                                playerViewModel.playSongs(displaySongs, 0)
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
                    albums = albums,
                    columns = albumColumns,
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
                    selectionMode = multiSelectMode,
                    selectedSongIds = selectedSongIds,
                    onFolderClick = { pathParts ->
                        folderBrowserInitialPath = pathParts
                        showFolderBrowser = true
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
            AppBackgroundLayer()
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
                    OverlayIconDropdownMenu(
                        entries = listOf(
                            DropdownEntry(
                                items = listOf(
                                    DropdownItem("刷新", icon = { mod -> Icon(Lucide.RotateCcw, null, modifier = mod) }, onClick = { viewModel.refreshMediaStore() }),
                                    DropdownItem("排序", icon = { mod -> Icon(Lucide.ArrowUpDown, null, modifier = mod) }, onClick = { showSortSheet = true }),
                                    DropdownItem("多选", icon = { mod -> Icon(Lucide.ListChecks, null, modifier = mod) }, onClick = {
                                        multiSelectMode = true
                                        selectedSongIds.clear()
                                    }),
                                    DropdownItem("回到顶部", icon = { mod -> Icon(Lucide.ArrowUp, null, modifier = mod) }, onClick = { /* TODO */ }),
                                    DropdownItem("定位当前播放", icon = { mod -> Icon(Lucide.MapPin, null, modifier = mod) }, onClick = {
                                        currentSong?.let { song ->
                                            selectedNavIndex = 0
                                            pendingLocateSongId = song.id
                                        }
                                    }),
                                    DropdownItem("列表布局", icon = { mod -> Icon(Lucide.LayoutList, null, modifier = mod) }, onClick = { /* TODO */ }),
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
        // 浏览路径 — 文件管理器式覆盖层
        if (showFolderBrowser) {
            BackHandler { showFolderBrowser = false }
            FolderBrowserOverlay(
                songs = songs,
                initialPath = folderBrowserInitialPath,
                playerViewModel = playerViewModel,
                onDismiss = { showFolderBrowser = false }
            )
        }

        // 排序弹窗
        if (showSortSheet) {
            BackHandler { showSortSheet = false }
            SortSheet(
                currentField = sortField,
                currentOrder = sortOrder,
                onDismiss = { showSortSheet = false },
                onSelectField = { viewModel.setSortField(it) },
                onToggleOrder = { viewModel.toggleSortOrder() }
            )
        }

        // 歌曲上下文菜单
        if (showSongMenu && menuSong != null) {
            BackHandler { showSongMenu = false }
            SongMenuSheet(
                song = menuSong!!,
                onDismiss = { showSongMenu = false },
                onPlayNext = {
                    // TODO: 将歌曲加入播放队列的下一首
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
    albums: List<LibraryViewModel.Album>,
    columns: Int,
    selectionMode: Boolean = false,
    selectedSongIds: SnapshotStateSet<Long>? = null,
    onAlbumClick: (LibraryViewModel.Album) -> Unit
) {
    item {
        Text(
            text = "${albums.size} 张专辑",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    val rows = albums.chunked(columns)
    items(rows.size, key = { rows[it].first().name + "#" + rows[it].first().artist }) { index ->
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
                    selected = selectedSongIds?.let { sel ->
                        album.songs.isNotEmpty() && album.songs.all { it.id in sel }
                    } == true,
                    onClick = {
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
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            if (rowAlbums.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
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
    selectionMode: Boolean = false,
    selectedSongIds: SnapshotStateSet<Long>? = null,
    onFolderClick: (List<String>) -> Unit,
    onHideFolder: (String) -> Unit
) {
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
    val sortedFolders = folderMap.toSortedMap()

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
                            val parts = folderPath.split('/').filter { it.isNotEmpty() }
                            onFolderClick(parts)
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
                SelectionIndicator(
                    selected = selected,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
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
internal fun FolderBrowserOverlay(
    songs: List<Song>,
    initialPath: List<String> = emptyList(),
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val rootNode = remember(songs) { buildFileTree(songs) }
    var currentPath by remember { mutableStateOf(initialPath) }

    val currentNode = remember(rootNode, currentPath) {
        var node = rootNode
        for (part in currentPath) {
            node = node.children[part] ?: break
        }
        node
    }

    val foldersInCurrent = remember(currentNode) {
        currentNode.children.values
            .filter { it.isDirectory }
            .sortedBy { it.name.lowercase() }
    }

    val songsInCurrent = remember(currentNode) {
        currentNode.children.values
            .filter { !it.isDirectory && it.song != null }
            .sortedBy { it.name.lowercase() }
            .mapNotNull { it.song }
    }

    val statusBarPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val bottomControlsHeight = 56.dp + navBarPadding

    // 地址栏路径：根 > 一级 > 二级
    val breadcrumb = listOf("根") + currentPath

    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val cutoutLeft = cutoutPadding.calculateLeftPadding(LayoutDirection.Ltr)
    val cutoutRight = cutoutPadding.calculateRightPadding(LayoutDirection.Ltr)

    BackHandler {
        if (currentPath.isEmpty()) {
            onDismiss()
        } else {
            currentPath = currentPath.dropLast(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 覆盖层底部铺统一背景，避免透出下方媒体库
        AppBackgroundLayer(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = statusBarPadding.calculateTopPadding(),
                    start = cutoutLeft,
                    end = cutoutRight
                )
        ) {
            // 地址栏
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(breadcrumb) { index, name ->
                    val isLast = index == breadcrumb.lastIndex
                    Text(
                        text = name,
                        style = MiuixTheme.textStyles.body2,
                        color = if (isLast) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable(enabled = !isLast) {
                            // breadcrumb: ["根", part0, part1, ...]
                            // 点击根 -> 空路径；点击第 n 个 -> 保留前 n-1 段
                            currentPath = if (index <= 1) emptyList() else currentPath.take(index - 1)
                        }
                    )
                    if (!isLast) {
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }

            HorizontalDivider()

            // 文件/文件夹列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = bottomControlsHeight + 16.dp
                )
            ) {
                if (foldersInCurrent.isEmpty() && songsInCurrent.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("此目录为空", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                } else {
                    items(foldersInCurrent, key = { it.path }) { item ->
                        FileBrowserItemRow(
                            item = item,
                            onClick = {
                                currentPath = currentPath + item.name
                            }
                        )
                    }
                    items(songsInCurrent, key = { it.id }) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                playerViewModel.playSongs(songsInCurrent, songsInCurrent.indexOf(song))
                            },
                            onMoreClick = { /* TODO: 歌曲更多操作 */ }
                        )
                    }
                }
            }
        }

        // 底部标题栏
        BlurTopBar(
            title = if (currentPath.isEmpty()) "浏览路径" else currentNode.path,
            navigationIcon = {
                IconButton(onClick = {
                    if (currentPath.isEmpty()) {
                        onDismiss()
                    } else {
                        currentPath = currentPath.dropLast(1)
                    }
                }) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = if (currentPath.isEmpty()) "关闭" else "返回上级",
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = navBarPadding,
                    start = cutoutLeft,
                    end = cutoutRight
                )
        )
    }
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
