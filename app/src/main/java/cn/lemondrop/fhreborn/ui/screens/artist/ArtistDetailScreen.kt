package cn.lemondrop.fhreborn.ui.screens.artist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverSizes
import cn.lemondrop.clover.CloverSpacing
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.MainScaffold
import cn.lemondrop.fhreborn.ui.screens.library.AlbumItem
import cn.lemondrop.fhreborn.ui.screens.library.SongItem
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import io.github.composefluent.component.Icon

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
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    val songs = remember(artistName) { libraryViewModel.getArtistSongs(artistName) }
    val albums = remember(artistName) { libraryViewModel.getArtistAlbums(artistName) }
    val guestAlbums = remember(artistName) { libraryViewModel.getGuestAlbumsForArtist(artistName) }

    val tabs = listOf(
        "歌曲 (${songs.size})",
        "专辑 (${albums.size})",
        "参与 (${guestAlbums.size})"
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    MainScaffold(
        currentRoute = "artist",
        title = { Text(artistName) },
        onNavigate = { },
        onPlayerClick = { },
        playerViewModel = playerViewModel,
        navigationIcon = {
            FluentIconButton(onClick = onBack) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "返回"
                )
            }
        }
    ) { paddingValues, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Text(
                text = artistName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = CloverSizes.listOuterHorizontalPadding, vertical = CloverSpacing.md)
            )

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            SongItem(
                                song = song,
                                isSelected = false,
                                isPlaying = song.id == currentSong?.id && isPlaying,
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
                                    .padding(horizontal = CloverSizes.listOuterHorizontalPadding, vertical = CloverSpacing.sm)
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
                                    .padding(horizontal = CloverSizes.listOuterHorizontalPadding, vertical = CloverSpacing.sm)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
