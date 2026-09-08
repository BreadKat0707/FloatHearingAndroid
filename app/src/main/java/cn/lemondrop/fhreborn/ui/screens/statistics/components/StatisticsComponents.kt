package cn.lemondrop.fhreborn.ui.screens.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.dao.TopAlbumStat
import cn.lemondrop.fhreborn.data.db.dao.TopArtistStat
import cn.lemondrop.fhreborn.data.db.dao.TopSongStat
import cn.lemondrop.fhreborn.ui.components.FhListItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun StatHeaderCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    top.yukonga.miuix.kmp.basic.Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TopSongsList(
    songs: List<TopSongStat>,
    modifier: Modifier = Modifier,
    emptyText: String = "暂无播放记录"
) {
    Column(modifier = modifier) {
        SectionTitle("Top ${songs.size} 歌曲")
        Spacer(modifier = Modifier.height(8.dp))
        if (songs.isEmpty()) {
            EmptyHint(emptyText)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                songs.forEachIndexed { index, song ->
                    FhListItem(
                        title = song.title,
                        summary = "${song.artist} · ${song.album}",
                        onClick = { },
                        // 统计页列表自带 16dp 内容边距，行内不再重复
                        horizontalPadding = 0.dp,
                        leading = {
                            RankBadge(rank = index + 1)
                        },
                        trailing = {
                            Text(
                                text = "${song.count} 次",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TopArtistsRow(
    artists: List<TopArtistStat>,
    modifier: Modifier = Modifier,
    emptyText: String = "暂无数据",
    onArtistClick: (TopArtistStat) -> Unit = {}
) {
    Column(modifier = modifier) {
        SectionTitle("常听艺术家")
        Spacer(modifier = Modifier.height(8.dp))
        if (artists.isEmpty()) {
            EmptyHint(emptyText)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(artists) { artist ->
                    ArtistChip(
                        artist = artist,
                        onClick = { onArtistClick(artist) }
                    )
                }
            }
        }
    }
}

@Composable
fun TopAlbumsRow(
    albums: List<TopAlbumStat>,
    modifier: Modifier = Modifier,
    emptyText: String = "暂无数据",
    onAlbumClick: (TopAlbumStat) -> Unit = {}
) {
    Column(modifier = modifier) {
        SectionTitle("常听专辑")
        Spacer(modifier = Modifier.height(8.dp))
        if (albums.isEmpty()) {
            EmptyHint(emptyText)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(albums) { album ->
                    AlbumChip(
                        album = album,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }
    }
}

/** 常听艺术家项：首字母圆形头像 + 名称 + 统计（点击查看艺术家详情） */
@Composable
private fun ArtistChip(
    artist: TopArtistStat,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = artist.artist.take(1).uppercase(),
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = artist.artist,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${artist.count} 次 · ${statFormatDurationShort(artist.totalDuration)}",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 常听专辑项：封面 + 专辑名 + 统计（点击查看专辑详情） */
@Composable
private fun AlbumChip(
    album: TopAlbumStat,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        cn.lemondrop.fhreborn.ui.components.SongCoverImage(
            songId = album.coverSongId ?: 0L,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp)),
            targetPixelSize = cn.lemondrop.fhreborn.ui.components.CoverImageCache.MEDIUM_COVER_TARGET
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.album,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${album.albumArtist} · ${album.count} 次",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.title3,
        color = MiuixTheme.colorScheme.onSurface
    )
}

@Composable
fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val background = if (rank <= 3) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.surfaceVariant
    }
    val textColor = if (rank <= 3) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.toString(),
            style = MiuixTheme.textStyles.body2,
            color = textColor
        )
    }
}


// 本地短时间格式化辅助（不依赖外部文件）
private fun statFormatDurationShort(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalSeconds}s"
    }
}
