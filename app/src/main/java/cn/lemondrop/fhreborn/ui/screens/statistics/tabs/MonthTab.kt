package cn.lemondrop.fhreborn.ui.screens.statistics.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp,
    onScrolledChange: (Boolean) -> Unit = {}
) {
    val state by viewModel.monthUiState.collectAsState()

    val chartData = state.dailyData.map { ChartPoint(it.first, it.second) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // 滚动感知：滚离顶部时通知父层（顶栏显示背景/模糊）
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }.collect(onScrolledChange)
    }

    Box(modifier = modifier) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = topInset + 16.dp,
            bottom = bottomInset + 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
    // 滚动条（跳过顶部/底部内容 padding 区域）
    cn.lemondrop.fhreborn.ui.components.LazyListScrollBar(
        listState = listState,
        modifier = Modifier.align(Alignment.CenterEnd),
        trackPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    )
    }
}