package cn.lemondrop.fhreborn.ui.screens.library

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ListItem
import io.github.composefluent.component.Text
import io.github.composefluent.component.ProgressRing
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.Screen
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.data.repository.PlayStatisticsRepository
import cn.lemondrop.fhreborn.scanner.ScanProgress
import cn.lemondrop.fhreborn.ui.components.AppDrawer
import cn.lemondrop.fhreborn.ui.components.MiniPlayBar
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import cn.lemondrop.fhreborn.ui.components.SongMenuSheet
import cn.lemondrop.fhreborn.ui.components.SortSheet
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import cn.lemondrop.fhreborn.ui.theme.FluentLargeCorner
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.clover.CloverBottomNavbar
import cn.lemondrop.clover.CloverBottomSheet
import cn.lemondrop.clover.CloverFlyout
import cn.lemondrop.clover.CloverMenuItem
import cn.lemondrop.clover.CloverNavItem
import cn.lemondrop.clover.CloverNavigationRail
import cn.lemondrop.clover.CloverSizes
import cn.lemondrop.clover.CloverTitleBar
import cn.lemondrop.clover.CloverTopAppBar
import cn.lemondrop.clover.cloverIsCompactWidth
import com.composables.icons.lucide.Album
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.LayoutList
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.ListChecks
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SkipForward

/**
 * 媒体库页面 — 底部倒置布局（PRD 规范）
 *
 * 从上到下（屏幕底部方向）：
 * 浮动 MiniPlayBar → 底部导航栏 → 标题栏（最底部，单手可达）
 */
@Composable
fun LibraryScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onPlayerClick: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(context.applicationContext as Application)
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

    var isSearching by remember { mutableStateOf(false) }
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }
    var showFolderBrowser by remember { mutableStateOf(false) }
    var folderBrowserInitialPath by remember { mutableStateOf(listOf<String>()) }
    var selectedSongId by remember { mutableStateOf<Long?>(null) }
    var playingSongId by remember { mutableStateOf<Long?>(null) }
    var showSongMenu by remember { mutableStateOf(false) }
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showTitleBarMenu by remember { mutableStateOf(false) }

    val displaySongs = if (searchQuery.isNotBlank()) searchResults else songs

    // 系统 insets
    val statusBarPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()

    // 底部控件高度
    val miniPlayBarHeight = 72.dp
    val navBarHeight = 72.dp
    val acrylicHeight = navBarHeight + CloverSizes.titleBarHeight + navBarPadding
    val bottomControlsHeight = miniPlayBarHeight + 8.dp + acrylicHeight + 16.dp

    val listState = remember(selectedNavIndex) { androidx.compose.foundation.lazy.LazyListState() }
    val drawerHazeState = remember { HazeState() }
    val barHazeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val barHazeTintColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val isCompact = cloverIsCompactWidth()

    val navItems = remember {
        listOf(
            CloverNavItem("歌曲", Lucide.Music),
            CloverNavItem("专辑", Lucide.Album),
            CloverNavItem("艺术家", Lucide.Mic),
            CloverNavItem("文件夹", Lucide.FolderOpen)
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
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    val menuButton: @Composable () -> Unit = {
        FluentIconButton(onClick = { showDrawer = true }) {
            Icon(
                imageVector = Lucide.Menu,
                contentDescription = "菜单",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    val titleActions: @Composable RowScope.() -> Unit = {
        FluentIconButton(onClick = { /* TODO: 全部顺序循环 */ }) {
            Icon(
                imageVector = Lucide.Repeat,
                contentDescription = "全部顺序循环",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        FluentIconButton(onClick = { /* TODO: 全部随机 */ }) {
            Icon(
                imageVector = Lucide.Shuffle,
                contentDescription = "全部随机",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        FluentIconButton(onClick = { showTitleBarMenu = true }) {
            Icon(
                imageVector = Lucide.EllipsisVertical,
                contentDescription = "更多",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    val libraryBody: @Composable (PaddingValues, Dp) -> Unit = { contentPadding, bottomSpacer ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 搜索栏
            if (isSearching) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("搜索歌曲、艺术家、专辑...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        singleLine = true
                    )
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
                            ProgressRing(modifier = Modifier.size(24.dp))
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
                    selectedSongId = selectedSongId,
                    playingSongId = playingSongId,
                    onSongClick = { song ->
                        selectedSongId = song.id
                        playingSongId = song.id
                        playerViewModel.playSongs(displaySongs, displaySongs.indexOf(song))
                    },
                    onMoreClick = { song ->
                        menuSong = song
                        showSongMenu = true
                    }
                )
                1 -> AlbumsContent(albums = albums)
                2 -> ArtistsContent(artists = artists)
                3 -> FoldersContent(
                    songs = displaySongs,
                    hiddenFolders = hiddenFolders,
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
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCompact) {
            // drawerHazeState 源：页面主体内容
            Box(modifier = Modifier.fillMaxSize().hazeSource(state = drawerHazeState)) {
                libraryBody(
                    PaddingValues(
                        top = statusBarPadding.calculateTopPadding() + 8.dp,
                        bottom = 0.dp
                    ),
                    bottomControlsHeight + 32.dp
                )
            }

            // MiniPlayBar 浮在亚克力面板上方，自身不参与模糊
            MiniPlayBar(
                playerViewModel = playerViewModel,
                onClick = onPlayerClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = acrylicHeight + 8.dp)
            )

            // 底部导航栏 + 标题栏共用一块亚克力背景
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .hazeEffect(state = drawerHazeState) {
                        blurRadius = 40.dp
                        backgroundColor = barHazeBackgroundColor
                        tints = listOf(HazeTint(barHazeTintColor))
                        noiseFactor = 0.1f
                    }
                    .pointerInput(Unit) {
                        // 拦截亚克力面板空白区域的点击，不触发后面列表项的点击
                        detectTapGestures(onTap = { })
                    }
            ) {
                CloverBottomNavbar(
                    items = navItems,
                    selectedIndex = selectedNavIndex,
                    onItemSelected = { selectedNavIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = null
                )

                CloverTitleBar(
                    title = titleText,
                    leading = menuButton,
                    trailing = titleActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = navBarPadding),
                    backgroundColor = null
                )
            }
        } else {
            // 大屏幕：左侧 NavigationRail + 顶部 TopAppBar
            Row(modifier = Modifier.fillMaxSize()) {
                CloverNavigationRail(
                    items = navItems,
                    selectedIndex = selectedNavIndex,
                    onItemSelected = { selectedNavIndex = it },
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                )

                Column(modifier = Modifier.weight(1f)) {
                    CloverTopAppBar(
                        title = titleText,
                        navigationIcon = menuButton,
                        actions = titleActions,
                        modifier = Modifier.statusBarsPadding()
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        libraryBody(
                            PaddingValues(
                                top = 8.dp,
                                bottom = navBarPadding + 8.dp
                            ),
                            miniPlayBarHeight + 32.dp
                        )

                        MiniPlayBar(
                            playerViewModel = playerViewModel,
                            onClick = onPlayerClick,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .padding(bottom = navBarPadding + 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Drawer 底部弹出菜单
    cn.lemondrop.fhreborn.ui.components.AppDrawer(
        visible = showDrawer,
        onDismiss = { showDrawer = false },
        currentRoute = currentRoute,
        onNavigate = { route ->
            showDrawer = false
            if (route == Screen.FolderBrowser.route) {
                showFolderBrowser = true
            } else {
                onNavigate(route)
            }
        },
        hazeState = drawerHazeState
    )

    // 标题栏 Flyout 菜单（浮动于底部栏之上）
    TitleBarFlyoutMenu(
        visible = showTitleBarMenu,
        onDismiss = { showTitleBarMenu = false },
        onSortClick = { showSortSheet = true },
        onMultiSelectClick = { /* TODO: multi-select */ },
        onScrollToTop = {
            // TODO: 回到顶部
        },
        onLocateCurrent = {
            // TODO: 定位当前播放位置
        },
        onLayoutToggle = {
            // TODO: 切换列表布局
        }
    )

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
                // TODO: 加入歌单
            },
            onViewAlbum = {
                // TODO: 查看专辑
            },
            onViewArtist = {
                // TODO: 查看艺术家
            },
            onGoToFolder = {
                // TODO: 转至文件夹
            },
            onShare = {
                // TODO: 分享文件
            },
            onDelete = {
                // TODO: 删除文件
            }
        )
    }
}

// ===== 内容区域 =====

private fun androidx.compose.foundation.lazy.LazyListScope.SongsContent(
    songs: List<Song>,
    selectedSongId: Long?,
    playingSongId: Long?,
    onSongClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit
) {
    item {
        Text(
            text = "${songs.size} 首歌曲",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    items(songs, key = { it.id }) { song ->
        SongItem(
            song = song,
            isSelected = song.id == selectedSongId,
            isPlaying = song.id == playingSongId,
            onClick = { onSongClick(song) },
            onMoreClick = { onMoreClick(song) }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.AlbumsContent(albums: List<LibraryViewModel.Album>) {
    item {
        Text(
            text = "${albums.size} 张专辑",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    val rows = albums.chunked(2)
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
                    modifier = Modifier.weight(1f)
                )
            }
            if (rowAlbums.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ArtistsContent(artists: List<LibraryViewModel.Artist>) {
    item {
        Text(
            text = "${artists.size} 位艺术家",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    items(artists.size, key = { artists[it].name }) { index ->
        ArtistItem(artist = artists[index])
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.FoldersContent(
    songs: List<Song>,
    hiddenFolders: Set<String>,
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
                Text("暂无歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val folderMap = visibleSongs.groupBy { it.path.substringBeforeLast('/') }
    val sortedFolders = folderMap.toSortedMap()

    item {
        Text(
            text = "${sortedFolders.size} 个文件夹",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        val parts = folderPath.split('/').filter { it.isNotEmpty() }
                        onFolderClick(parts)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folderPath.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = folderPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folderSongs.size} 首歌曲",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FluentIconButton(onClick = { showOptions = true }) {
                    Icon(
                        imageVector = Lucide.EllipsisVertical,
                        contentDescription = "更多",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    CloverBottomSheet(onDismiss = onDismiss) {
        CloverMenuItem(
            label = "在音乐库隐藏",
            icon = Lucide.EyeOff,
            onClick = {
                onHide()
                onDismiss()
            }
        )
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun SongItem(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val indicatorHeight = when {
        isPlaying -> 40.dp
        isSelected -> 24.dp
        else -> 0.dp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧指示条区域
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .width(3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (indicatorHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(indicatorHeight)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

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
                text = "${song.artist} - ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FluentIconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Lucide.EllipsisVertical,
                contentDescription = "更多",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumItem(
    album: LibraryViewModel.Album,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = { /* TODO: open album detail */ }),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SongCoverImage(
            songId = album.coverSongId,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ArtistItem(artist: LibraryViewModel.Artist) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { /* TODO: open artist detail */ })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = artist.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.albumCount} 张专辑 · ${artist.songCount} 首歌曲",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ===== 标题栏 Flyout 菜单（浮动于底部栏之上） =====

@Composable
private fun TitleBarFlyoutMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSortClick: () -> Unit = {},
    onMultiSelectClick: () -> Unit = {},
    onScrollToTop: () -> Unit = {},
    onLocateCurrent: () -> Unit = {},
    onLayoutToggle: () -> Unit = {}
) {
    BackHandler(enabled = visible) {
        onDismiss()
    }

    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()

    CloverFlyout(
        visible = visible,
        onDismiss = onDismiss
    ) {
        CloverMenuItem(
            label = "排序",
            icon = Lucide.ArrowUpDown,
            onClick = {
                onDismiss()
                onSortClick()
            }
        )
        CloverMenuItem(
            label = "多选",
            icon = Lucide.ListChecks,
            onClick = {
                onDismiss()
                onMultiSelectClick()
            }
        )
        CloverMenuItem(
            label = "回到顶部",
            icon = Lucide.ArrowUp,
            onClick = {
                onDismiss()
                onScrollToTop()
            }
        )
        CloverMenuItem(
            label = "定位当前播放",
            icon = Lucide.Music,
            onClick = {
                onDismiss()
                onLocateCurrent()
            }
        )
        CloverMenuItem(
            label = "列表布局",
            icon = Lucide.LayoutList,
            onClick = {
                onDismiss()
                onLayoutToggle()
            }
        )

        Spacer(modifier = Modifier.height(navBarPadding + 4.dp))
    }
}

// ===== 浏览路径 — 文件管理器式层级浏览 =====

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: MutableMap<String, FileNode> = mutableMapOf(),
    val song: Song? = null
)

private fun buildFileTree(songs: List<Song>): FileNode {
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
private fun FolderBrowserOverlay(
    songs: List<Song>,
    initialPath: List<String> = emptyList(),
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val rootNode = remember(songs) { buildFileTree(songs) }
    var currentPath by remember { mutableStateOf(initialPath) }
    val playingSongId by playerViewModel.currentSong.collectAsState()

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
    val bottomControlsHeight = CloverSizes.titleBarHeight + navBarPadding

    // 地址栏路径：根 > 一级 > 二级
    val breadcrumb = listOf("根") + currentPath

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding.calculateTopPadding())
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Text("此目录为空", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            isSelected = false,
                            isPlaying = song.id == playingSongId?.id,
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
        CloverTitleBar(
            title = {
                Text(
                    text = if (currentPath.isEmpty()) "浏览路径" else currentNode.path,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leading = {
                FluentIconButton(onClick = {
                    if (currentPath.isEmpty()) {
                        onDismiss()
                    } else {
                        currentPath = currentPath.dropLast(1)
                    }
                }) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = if (currentPath.isEmpty()) "关闭" else "返回上级",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarPadding)
        )
    }
}

@Composable
private fun FileBrowserItemRow(
    item: FileNode,
    onClick: () -> Unit
) {
    ListItem(
        onClick = onClick,
        icon = {
            Icon(
                imageVector = if (item.isDirectory) Lucide.FolderOpen else Lucide.Music,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        text = {
            Column {
                Text(
                    text = item.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.isDirectory && item.song != null) {
                    Text(
                        text = "${item.song.artist} · ${formatDuration(item.song.duration)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        trailing = if (item.isDirectory) {
            {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null
    )
}
