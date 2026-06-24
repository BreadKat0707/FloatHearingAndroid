package cn.lemondrop.fhreborn.ui.screens.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverListItem
import cn.lemondrop.clover.CloverSpacing
import cn.lemondrop.fhreborn.data.db.dao.TopAlbumStat
import cn.lemondrop.fhreborn.data.db.dao.TopArtistStat
import cn.lemondrop.fhreborn.data.db.dao.TopSongStat
import io.github.composefluent.component.Text

@Composable
fun StatHeaderCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                songs.forEachIndexed { index, song ->
                    CloverListItem(
                        title = song.title,
                        subtitle = "${song.artist} · ${song.album}",
                        onClick = { },
                        leading = {
                            RankBadge(rank = index + 1)
                        },
                        trailing = {
                            Text(
                                text = "${song.count} 次",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    emptyText: String = "暂无数据"
) {
    Column(modifier = modifier) {
        SectionTitle("常听艺术家")
        Spacer(modifier = Modifier.height(8.dp))
        if (artists.isEmpty()) {
            EmptyHint(emptyText)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(artists) { artist ->
                    StatChip(
                        title = artist.artist,
                        subtitle = "${artist.count} 次 · ${formatDurationShort(artist.totalDuration)}"
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
    emptyText: String = "暂无数据"
) {
    Column(modifier = modifier) {
        SectionTitle("常听专辑")
        Spacer(modifier = Modifier.height(8.dp))
        if (albums.isEmpty()) {
            EmptyHint(emptyText)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(albums) { album ->
                    StatChip(
                        title = album.album,
                        subtitle = "${album.albumArtist} · ${album.count} 次"
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val background = if (rank <= 3) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (rank <= 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun StatChip(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
