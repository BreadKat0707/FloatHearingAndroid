package cn.lemondrop.fhreborn.ui.screens.library

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.data.repository.PlayStatisticsRepository
import cn.lemondrop.fhreborn.scanner.ScanProgress
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import cn.lemondrop.fhreborn.ui.theme.FluentLargeCorner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.ui.screens.player.FluidBackground
import cn.lemondrop.fhreborn.ui.components.SongMenuSheet
import cn.lemondrop.fhreborn.ui.components.SortSheet
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.composables.icons.lucide.Album
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.LayoutList
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.ListChecks
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Timer

/**
 * 媒体库页面 — 底部倒置布局（PRD 规范）
 *
 * 从上到下（屏幕底部方向）：
 * 浮动 MiniPlayBar → 底部导航栏 → 标题栏（最底部，单手可达）
 */
@Composable
fun LibraryScreen(
    playerViewModel: PlayerViewModel,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToPlayer: () -> Unit = {}
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

    var isSearching by remember { mutableStateOf(false) }
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }
    var showFolderBrowser by remember { mutableStateOf(false) }
    var selectedSongId by remember { mutableStateOf<Long?>(null) }
    var playingSongId by remember { mutableStateOf<Long?>(null) }
    var showSongMenu by remember { mutableStateOf(false) }
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showTitleBarMenu by remember { mutableStateOf(false) }

    val displaySongs = if (searchQuery.isNotBlank()) searchResults else songs

    // Haze 状态用于内容模糊效果
    val hazeState = remember { HazeState() }


    // 系统 insets
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // 底部控件高度
    val miniPlayBarHeight = 72.dp
    val navBarHeight = 72.dp
    val titleBarHeight = 64.dp
    val bottomControlsHeight = miniPlayBarHeight + navBarHeight + titleBarHeight + 16.dp

    val listState = remember(selectedNavIndex) { androidx.compose.foundation.lazy.LazyListState() }

    Box(modifier = Modifier.fillMaxSize()) {
        // 内容区域（顶部留空给状态栏，底部留空给底部控件）
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .hazeSource(state = hazeState),
            contentPadding = PaddingValues(
                top = statusBarPadding.calculateTopPadding() + 8.dp,
                bottom = navBarPadding.calculateBottomPadding() + 8.dp
            ),
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
                3 -> FoldersContent(songs = displaySongs)
            }

            // 底部占位，让内容可以滚动到底部栏下方，确保 Acrylic 有内容可模糊
            item {
                Spacer(modifier = Modifier.height(bottomControlsHeight + 32.dp))
            }
        }

        // 底部控件堆叠（从屏幕底部向上）
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // 浮动 MiniPlayBar
            MiniPlayBar(
                hazeState = hazeState,
                playerViewModel = playerViewModel,
                onClick = onNavigateToPlayer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 底部导航栏：歌曲 / 专辑 / 艺术家 / 文件夹
            LibraryNavBar(
                hazeState = hazeState,
                selectedIndex = selectedNavIndex,
                onSelect = { selectedNavIndex = it },
                modifier = Modifier.fillMaxWidth()
            )

            // 标题栏（最底部，单手可达）
            LibraryTitleBar(
                hazeState = hazeState,
                title = when (selectedNavIndex) {
                    0 -> "媒体库"
                    1 -> "专辑"
                    2 -> "艺术家"
                    3 -> "文件夹"
                    else -> "媒体库"
                },
                onDrawerClick = { showDrawer = true },
                onMoreClick = { showTitleBarMenu = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Drawer 底部弹出菜单
    if (showDrawer) {
        DrawerMenuSheet(
            onDismiss = { showDrawer = false },
            onNavigateToFolders = {
                showDrawer = false
                showFolderBrowser = true
            },
            onNavigateToSettings = {
                showDrawer = false
                onNavigateToSettings()
            },
            onNavigateToStatistics = {
                showDrawer = false
                onNavigateToStatistics()
            },
            hazeState = hazeState
        )
    }

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
        },
        hazeState = hazeState
    )

    // 浏览路径 — 文件管理器式覆盖层
    if (showFolderBrowser) {
        FolderBrowserOverlay(
            songs = songs,
            onDismiss = { showFolderBrowser = false }
        )
    }

    // 排序弹窗
    if (showSortSheet) {
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
    songs: List<Song>
) {
    if (songs.isEmpty()) {
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

    val folderMap = songs.groupBy { it.path.substringBeforeLast('/') }
    val sortedFolders = folderMap.toSortedMap()

    sortedFolders.forEach { (folderPath, folderSongs) ->
        item(key = folderPath) {
            var expanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FluentLargeCorner))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expanded = !expanded }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (expanded) 90f else 0f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folderPath.substringAfterLast('/'),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = folderPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${folderSongs.size} 首",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (expanded) {
                    folderSongs.forEach { song ->
                        SongItemSmall(song = song)
                    }
                }
            }
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

@Composable
internal fun SongCoverImage(
    songId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(songId) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(songId) {
        withContext(Dispatchers.IO) {
            bitmap = try {
                val uri = Uri.parse("content://media/external/audio/media/$songId/albumart")
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♪",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun MiniPlayBar(
    playerViewModel: PlayerViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val fluidOnColorSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(FluentLargeCorner))
            .clickable(onClick = onClick)
    ) {
        // 流体背景
        FluidBackground(
            songId = currentSong?.id,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            currentSong?.let { song ->
                SongCoverImage(
                    songId = song.id,
                    modifier = Modifier.size(40.dp)
                )
            } ?: Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(fluidOnColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("♪", color = fluidOnColorSecondary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "Float Hearing",
                    color = fluidOnColor,
                    maxLines = 1
                )
                Text(
                    text = currentSong?.artist ?: "Make some sounds",
                    style = MaterialTheme.typography.bodySmall,
                    color = fluidOnColorSecondary,
                    maxLines = 1
                )
            }

            currentSong?.let {
                val targetBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply
                FluentIconButton(onClick = { playerViewModel.playPause() }) {
                    Icon(
                        imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                                blendMode = targetBlendMode
                            },
                        tint = fluidOnColor.copy(alpha = 0.6f)
                    )
                }
                FluentIconButton(onClick = { playerViewModel.next() }) {
                    Icon(
                        imageVector = Lucide.SkipForward,
                        contentDescription = "下一首",
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                                blendMode = targetBlendMode
                            },
                        tint = fluidOnColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 底部导航栏：歌曲 / 专辑 / 艺术家 / 文件夹
 */
@Suppress("DEPRECATION")
@Composable
private fun LibraryNavBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val items = listOf(
        "歌曲" to Lucide.Music,
        "专辑" to Lucide.Album,
        "艺术家" to Lucide.Mic,
        "文件夹" to Lucide.FolderOpen,
    )
    val hazeTint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val hazeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Row(
        modifier = modifier
            .height(64.dp)
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = 32.dp
                        backgroundColor = hazeBg
                        tints = listOf(HazeTint(color = hazeTint))
                    }
                } else Modifier
            )
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, (label, icon) ->
            val selected = index == selectedIndex
            FluentIconButton(onClick = { onSelect(index) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 标题栏（最底部，单手可达）
 */
@Suppress("DEPRECATION")
@Composable
private fun LibraryTitleBar(
    title: String,
    onDrawerClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val hazeTint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val hazeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = 32.dp
                        backgroundColor = hazeBg
                        tints = listOf(HazeTint(color = hazeTint))
                    }
                } else Modifier
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drawer 图标
        FluentIconButton(onClick = onDrawerClick) {
            Icon(
                imageVector = Lucide.Menu,
                contentDescription = "菜单",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // 全部顺序循环
        FluentIconButton(onClick = { }) {
            Icon(
                imageVector = Lucide.Repeat,
                contentDescription = "全部顺序循环",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 全部随机
        FluentIconButton(onClick = { }) {
            Icon(
                imageVector = Lucide.Shuffle,
                contentDescription = "全部随机",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 更多
        FluentIconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Lucide.EllipsisVertical,
                contentDescription = "更多",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
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
    onLayoutToggle: () -> Unit = {},
    hazeState: HazeState? = null
) {
    val flyoutHazeTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    val flyoutHazeBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    val menuItems = listOf(
        Triple("排序", Lucide.ArrowUpDown, onSortClick),
        Triple("多选", Lucide.ListChecks, onMultiSelectClick),
        Triple("回到顶部", Lucide.ArrowUp, onScrollToTop),
        Triple("定位当前播放", Lucide.Music, onLocateCurrent),
        Triple("列表布局", Lucide.LayoutList, onLayoutToggle)
    )

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // 点击外部关闭的透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    // 菜单面板：从底栏位置向上弹出，覆盖在底栏上层
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 4.dp, end = 4.dp),
        enter = fadeIn() + scaleIn(
            initialScale = 0.85f,
            transformOrigin = TransformOrigin(1f, 1f)
        ),
        exit = fadeOut() + scaleOut(
            targetScale = 0.85f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .width(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) {
                                blurRadius = 40.dp
                                backgroundColor = flyoutHazeBg
                                tints = listOf(HazeTint(color = flyoutHazeTint))
                            }
                        } else Modifier
                    )
                    .padding(vertical = 10.dp)
            ) {
                menuItems.forEach { (label, icon, onClick) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onClick()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ===== Drawer 底部弹出菜单 =====

@Suppress("DEPRECATION")
@Composable
private fun DrawerMenuSheet(
    onDismiss: () -> Unit,
    onNavigateToFolders: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    hazeState: HazeState? = null
) {
    val drawerHazeTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    val drawerHazeBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = 32.dp
                            backgroundColor = drawerHazeBg
                            tints = listOf(HazeTint(color = drawerHazeTint))
                        }
                    } else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .clickable(enabled = false) { /* 阻止点击穿透到背景 */ }
        ) {
            DrawerItem(icon = Lucide.Music, label = "媒体库")
            DrawerItem(icon = Lucide.ListMusic, label = "歌单")
            DrawerItem(icon = Lucide.FolderOpen, label = "浏览路径", onClick = onNavigateToFolders)
            DrawerItem(icon = Lucide.Lightbulb, label = "想法")
            DrawerItem(icon = Lucide.Activity, label = "统计和数据分析", onClick = onNavigateToStatistics)

            Spacer(modifier = Modifier.height(8.dp))

            DrawerItem(icon = Lucide.Headphones, label = "音频输出")
            DrawerItem(icon = Lucide.Timer, label = "定时播放")
            DrawerItem(icon = Lucide.Settings, label = "设置", onClick = onNavigateToSettings)
        }
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
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
    onDismiss: () -> Unit
) {
    val rootNode = remember(songs) { buildFileTree(songs) }
    var currentPath by remember { mutableStateOf(listOf<String>()) }

    val currentNode = remember(rootNode, currentPath) {
        var node = rootNode
        for (part in currentPath) {
            node = node.children[part] ?: break
        }
        node
    }

    val items = remember(currentNode) {
        currentNode.children.values.sortedWith(
            compareBy({ !it.isDirectory }, { it.name.lowercase() })
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FluentIconButton(onClick = {
                    if (currentPath.isEmpty()) {
                        onDismiss()
                    } else {
                        currentPath = currentPath.dropLast(1)
                    }
                }) {
                    Icon(
                        imageVector = if (currentPath.isEmpty()) Lucide.ArrowLeft else Lucide.ArrowLeft,
                        contentDescription = if (currentPath.isEmpty()) "关闭" else "返回上级",
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (currentPath.isEmpty()) "浏览路径" else currentNode.path,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // 文件/文件夹列表
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (items.isEmpty()) {
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
                    items(items, key = { it.path }) { item ->
                        FileBrowserItemRow(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    currentPath = currentPath + item.name
                                }
                            }
                        )
                    }
                }
            }
        }
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
