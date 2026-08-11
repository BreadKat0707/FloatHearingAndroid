package cn.lemondrop.fhreborn.ui.screens.artist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.screens.library.AlbumItem
import cn.lemondrop.fhreborn.ui.screens.library.SongItem
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import top.yukonga.miuix.kmp.basic.TabRow
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
    val songs = remember(artistName) { libraryViewModel.getArtistSongs(artistName) }
    val albums = remember(artistName) { libraryViewModel.getArtistAlbums(artistName) }
    val guestAlbums = remember(artistName) { libraryViewModel.getGuestAlbumsForArtist(artistName) }

    val tabTitles = listOf(
        "歌曲 (${songs.size})",
        "专辑 (${albums.size})",
        "参与 (${guestAlbums.size})"
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    // 层背景：顶栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val artistListState = androidx.compose.foundation.lazy.rememberLazyListState()
    // 顶栏滚动感知：列表滚离顶部时显示背景/模糊，回顶隐藏
    val topBarScrolled = remember {
        derivedStateOf {
            artistListState.firstVisibleItemIndex > 0 || artistListState.firstVisibleItemScrollOffset > 0
        }
    }.value

    AppBackgroundLayer()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            BlurTopBar(
                backdrop = backdrop,
                scrolled = topBarScrolled,
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
            Text(
                text = artistName,
                style = MiuixTheme.textStyles.title1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TabRow(
                tabs = tabTitles,
                selectedTabIndex = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
            LazyColumn(
                state = artistListState,
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            SongItem(
                                song = song,
                                onClick = { playerViewModel.playSongs(songs, index) },
                                onMoreClick = { }
                            )
                        }
                    }
                    1 -> {
                        itemsIndexed(albums, key = { _, album -> album.name + album.artist }) { _, album ->
                            AlbumItem(
                                album = album,
                                onClick = { onNavigateToAlbum(album.name, album.artist) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    2 -> {
                        itemsIndexed(guestAlbums, key = { _, album -> album.name + album.artist }) { _, album ->
                            AlbumItem(
                                album = album,
                                onClick = { onNavigateToAlbum(album.name, album.artist) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
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
}