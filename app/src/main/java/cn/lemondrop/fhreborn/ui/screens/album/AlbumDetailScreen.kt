package cn.lemondrop.fhreborn.ui.screens.album

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import cn.lemondrop.fhreborn.ui.screens.library.SongItem
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Disc
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 专辑详情页。
 *
 * 数据源：按专辑名（可选专辑艺术家）从 [SongDao.getSongsByAlbum] 拉取曲目，
 * 页面内计算总时长/发行年份/参与艺术家等聚合信息。
 */
@Composable
fun AlbumDetailScreen(
    albumName: String,
    albumArtist: String?,
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val songsFlow = remember(albumName, albumArtist) {
        db.songDao().getSongsByAlbum(albumName, albumArtist)
    }
    val songs by songsFlow.collectAsState(initial = emptyList())
    val currentSong by playerViewModel.currentSong.collectAsState()

    val displayAlbum = albumName.ifBlank { "未知专辑" }
    val displayArtist = albumArtist
        ?: songs.firstOrNull()?.albumArtist
        ?: songs.firstOrNull()?.artist
        ?: "未知艺术家"
    val totalDuration = songs.sumOf { it.duration }
    val releaseYear = songs.firstNotNullOfOrNull { it.year }
    val participatingArtists = songs.map { it.artist }.distinct()
    val hasMultipleDiscs = songs.mapNotNull { it.discNumber }.distinct().size > 1
    val coverSongId = songs.firstOrNull()?.id

    val meta = buildString {
        append("${songs.size} 首")
        append(" · ")
        append(formatAlbumDuration(totalDuration))
        releaseYear?.let { append(" · $it") }
    }

    // 层背景：顶栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    Scaffold(
        topBar = {
            BlurTopBar(
                backdrop = backdrop,
                title = "专辑",
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
        val albumListState = androidx.compose.foundation.lazy.rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .padding(padding)
        ) {
        LazyColumn(
            state = albumListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues()
        ) {
            // 头部：封面 + 专辑信息 + 播放按钮
            item {
                AlbumHeader(
                    album = displayAlbum,
                    artist = displayArtist,
                    meta = meta,
                    coverSongId = coverSongId,
                    onPlayAlbum = {
                        if (songs.isNotEmpty()) playerViewModel.playSongs(songs, 0)
                    }
                )
            }

            // 曲目列表（按碟号分组）
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                Column {
                    if (hasMultipleDiscs &&
                        (index == 0 || songs[index - 1].discNumber != song.discNumber)
                    ) {
                        SectionHeader("Disc ${song.discNumber ?: 1}")
                    }
                    SongItem(
                        song = song,
                        onClick = { playerViewModel.playSongs(songs, index) },
                        onMoreClick = { }
                    )
                }
            }

            // 参与的艺术家
            if (participatingArtists.isNotEmpty()) {
                item {
                    SectionHeader("参与的艺术家")
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        participatingArtists.forEach { artist ->
                            Text(
                                text = artist,
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 底部留白，避免内容被底栏/迷你播放条遮挡
            item { Spacer(modifier = Modifier.height(bottomOverlayHeight)) }
        }
        // 滚动条：自动淡入淡出，可拖动定位
        cn.lemondrop.fhreborn.ui.components.LazyListScrollBar(
            listState = albumListState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        }
    }
}

@Composable
private fun AlbumHeader(
    album: String,
    artist: String,
    meta: String,
    coverSongId: Long?,
    onPlayAlbum: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (coverSongId != null) {
            SongCoverImage(
                songId = coverSongId,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Disc,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = album,
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = artist,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = meta,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onPlayAlbum) {
            Icon(
                imageVector = Lucide.Play,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "播放专辑",
                color = MiuixTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.title3,
        color = MiuixTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

private fun formatAlbumDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}小时${minutes}分" else "${minutes}分钟"
}