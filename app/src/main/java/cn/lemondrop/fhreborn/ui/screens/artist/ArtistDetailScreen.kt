package cn.lemondrop.fhreborn.ui.screens.artist

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.edgeFadeOut
import cn.lemondrop.fhreborn.ui.components.responsiveColumnCount
import cn.lemondrop.fhreborn.ui.screens.library.AlbumItem
import cn.lemondrop.fhreborn.util.ArtistSplitter
import kotlinx.coroutines.flow.combine
import cn.lemondrop.fhreborn.ui.screens.library.SongItem
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 艺术家详情页。
 *
 * 展示该艺术家的歌曲、主理专辑以及参与的专辑。
 */
@Composable
fun ArtistDetailScreen(
    artistName: String,
    onBack: () -> Unit,
    onNavigateToAlbum: (String, String?) -> Unit,
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel
) {
    // 响应式订阅：媒体库数据异步加载完成后自动刷新（不能用 remember 一次性缓存，
    // 否则进入页面时数据未就绪会一直显示空列表）
    val songs by libraryViewModel.songs
        .combine(libraryViewModel.artistSeparators) { songs, separators ->
            songs.filter { ArtistSplitter.containsArtist(it.artist, artistName, separators) }
        }
        .collectAsState(initial = emptyList())
    val albums by libraryViewModel.albums
        .combine(libraryViewModel.artistSeparators) { albums, _ ->
            albums.filter { it.artist.equals(artistName, ignoreCase = true) }
        }
        .collectAsState(initial = emptyList())
    val guestAlbums by libraryViewModel.albums
        .combine(libraryViewModel.artistSeparators) { albums, separators ->
            albums.filter { album ->
                !album.artist.equals(artistName, ignoreCase = true) &&
                    album.songs.any { song ->
                        ArtistSplitter.containsArtist(song.artist, artistName, separators)
                    }
            }
        }
        .collectAsState(initial = emptyList())

    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showSongProperties by remember { mutableStateOf(false) }
    var propertiesSong by remember { mutableStateOf<Song?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val tabTitles = listOf(
        "歌曲 (${songs.size})",
        "专辑 (${albums.size})",
        "参与 (${guestAlbums.size})"
    )
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }

    // 层背景：顶栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    // 每个 tab 各自的滚动位置（按 selectedTab 分 key），离开页面返回后恢复
    val artistListState = androidx.compose.runtime.saveable.rememberSaveable(
        selectedTab,
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) { androidx.compose.foundation.lazy.LazyListState() }
    // 列表滚动感知：用于顶部边缘淡出的条件（未滚动时第一项在顶部，不做淡出）
    val topBarScrolled = remember {
        derivedStateOf {
            artistListState.firstVisibleItemIndex > 0 || artistListState.firstVisibleItemScrollOffset > 0
        }
    }.value

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // 去掉底部 inset 避让：列表内容延伸到系统导航栏后面（edge-to-edge）
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BlurTopBar(
                // TabRow 位于标题栏正下方，列表内容不会滚到标题栏后面，
                // 标题栏不需要背景/模糊，恒为透明
                backdrop = backdrop,
                scrolled = false,
                title = artistName,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        val bottomOverlayHeight = LocalGlobalPlayBarHeight.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRowWithContour(
                tabs = tabTitles,
                selectedTabIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                // 两侧留边距，避免贴住窗口/内容区边缘
                modifier = Modifier.padding(horizontal = 16.dp),
                // 半透明配色：图片背景时容器/选中项不会形成突兀的实色块
                colors = top.yukonga.miuix.kmp.basic.TabRowDefaults.tabRowColors(
                    backgroundColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.5f),
                    contentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    selectedBackgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.85f),
                    selectedContentColor = MiuixTheme.colorScheme.onPrimary
                )
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
            // 专辑/参与网格列数：与媒体库-专辑一致（大屏响应式多列）
            val albumColumns = responsiveColumnCount(maxWidth, minItemWidthDp = 170, minColumns = 2)
            LazyColumn(
                state = artistListState,
                // 左右滑动切换 tab（翻页）；垂直滚动仍由 LazyColumn 处理
                // 边缘淡出：仅顶部，且列表滚离顶部（第一项滑走）后才启用——
                // 未滚动时第一项在顶部，不做淡出避免遮住表头
                modifier = Modifier
                    .fillMaxSize()
                    .edgeFadeOut(top = if (topBarScrolled) 48.dp else 0.dp, bottom = 0.dp)
                    .pointerInput(Unit) {
                        var accumulated = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onDragEnd = {
                                val threshold = 80.dp.toPx()
                                when {
                                    accumulated > threshold && selectedTab > 0 -> selectedTab -= 1
                                    accumulated < -threshold && selectedTab < tabTitles.lastIndex -> selectedTab += 1
                                }
                                accumulated = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulated += dragAmount
                            }
                        )
                    }
            ) {
                when (selectedTab) {
                    0 -> {
                        if (songs.isEmpty()) {
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
                        }
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            SongItem(
                                song = song,
                                onClick = { playerViewModel.playSongs(songs, index) },
                                onMoreClick = { menuSong = song }
                            )
                        }
                    }
                    1 -> {
                        if (albums.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂无专辑", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                }
                            }
                        }
                        val rows = albums.chunked(albumColumns)
                        items(rows.size, key = { rows[it].first().name + rows[it].first().artist }) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rows[index].forEach { album ->
                                    AlbumItem(
                                        album = album,
                                        onClick = { onNavigateToAlbum(album.name, album.artist) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rows[index].size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    2 -> {
                        if (guestAlbums.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂无参与专辑", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                }
                            }
                        }
                        val rows = guestAlbums.chunked(albumColumns)
                        items(rows.size, key = { rows[it].first().name + rows[it].first().artist }) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rows[index].forEach { album ->
                                    AlbumItem(
                                        album = album,
                                        onClick = { onNavigateToAlbum(album.name, album.artist) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rows[index].size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(bottomOverlayHeight)) }
            }
            // 滚动条：自动淡入淡出，可拖动定位（跳过底部播放条占位）
            cn.lemondrop.fhreborn.ui.components.LazyListScrollBar(
                listState = artistListState,
                modifier = Modifier.align(Alignment.CenterEnd),
                trackPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomOverlayHeight)
            )
            }
        }
    }

    // 歌曲更多菜单（基础项：专辑/分享/打开方式/属性）
    menuSong?.let { song ->
        cn.lemondrop.fhreborn.ui.components.SongMenuSheet(
            song = song,
            onDismiss = { menuSong = null },
            onViewAlbum = { onNavigateToAlbum(song.album, song.albumArtist ?: song.artist) },
            onShare = { cn.lemondrop.fhreborn.util.SongFileUtils.shareSong(context, song) },
            onOpenWith = { cn.lemondrop.fhreborn.util.SongFileUtils.openWithOtherApp(context, song) },
            onProperties = {
                propertiesSong = song
                menuSong = null
                showSongProperties = true
            }
        )
    }

    // 歌曲属性弹窗（菜单"属性"）
    if (showSongProperties) {
        androidx.activity.compose.BackHandler { showSongProperties = false }
        propertiesSong?.let { song ->
            cn.lemondrop.fhreborn.util.SongFileUtils.SongPropertiesDialog(
                song = song,
                onDismiss = { showSongProperties = false }
            )
        }
    }
}