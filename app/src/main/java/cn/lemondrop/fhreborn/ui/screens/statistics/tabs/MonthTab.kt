package cn.lemondrop.fhreborn.ui.screens.statistics.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverSpacing
import cn.lemondrop.fhreborn.ui.screens.statistics.components.ChartPoint
import cn.lemondrop.fhreborn.ui.screens.statistics.components.PlayDurationLineChart
import cn.lemondrop.fhreborn.ui.screens.statistics.components.StatHeaderCard
import cn.lemondrop.fhreborn.ui.screens.statistics.components.TopAlbumsRow
import cn.lemondrop.fhreborn.ui.screens.statistics.components.TopArtistsRow
import cn.lemondrop.fhreborn.ui.screens.statistics.components.TopSongsList
import cn.lemondrop.fhreborn.ui.screens.statistics.formatStatDuration
import cn.lemondrop.fhreborn.ui.viewmodel.StatisticsViewModel

@Composable
fun MonthTab(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.monthUiState.collectAsState()

    val chartData = state.dailyData.map { ChartPoint(it.first, it.second) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = CloverSpacing.lg,
            end = CloverSpacing.lg,
            top = 16.dp,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(CloverSpacing.lg)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CloverSpacing.md)
            ) {
                StatHeaderCard(
                    title = "本月播放时长",
                    value = formatStatDuration(state.duration),
                    modifier = Modifier.weight(1f)
                )
                StatHeaderCard(
                    title = "本月播放次数",
                    value = "${state.count} 次",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            StatHeaderCard(
                title = "本月播放歌曲数",
                value = "${state.uniqueSongs} 首",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            PlayDurationLineChart(
                data = chartData,
                title = "本月每天播放时长",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            TopSongsList(songs = state.topSongs, modifier = Modifier.fillMaxWidth())
        }

        item {
            TopArtistsRow(artists = state.topArtists, modifier = Modifier.fillMaxWidth())
        }

        item {
            TopAlbumsRow(albums = state.topAlbums, modifier = Modifier.fillMaxWidth())
        }
    }
}
