package cn.lemondrop.fhreborn.ui.screens.statistics.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.ui.screens.statistics.components.StatHeaderCard
import cn.lemondrop.fhreborn.ui.screens.statistics.formatStatDuration
import cn.lemondrop.fhreborn.ui.viewmodel.StatisticsViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OverviewTab(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp,
    onScrolledChange: (Boolean) -> Unit = {}
) {
    val state by viewModel.overviewUiState.collectAsState()
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
            start = 16.dp,
            end = 16.dp,
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
                    title = "媒体库歌曲",
                    value = "${state.songCount} 首",
                    modifier = Modifier.weight(1f)
                )
                StatHeaderCard(
                    title = "累计播放时长",
                    value = formatStatDuration(state.totalDuration),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            StatHeaderCard(
                title = "累计播放次数",
                value = "${state.totalCount} 次",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "歌曲播放量排行",
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface
            )
        }

        // 排行行作为单个区块项，行间无间距（区块间距由 LazyColumn spacedBy 提供）
        item {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                state.songs.forEachIndexed { index, song ->
                    cn.lemondrop.fhreborn.ui.components.FhListItem(
                        title = song.title,
                        summary = "${song.artist} · ${song.album}",
                        onClick = { },
                        // 统计页列表自带 16dp 内容边距，行内不再重复
                        horizontalPadding = 0.dp,
                        leading = {
                            Text(
                                text = (index + 1).toString(),
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(end = 4.dp)
                            )
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
    // 滚动条（跳过顶部/底部内容 padding 区域）
    cn.lemondrop.fhreborn.ui.components.LazyListScrollBar(
        listState = listState,
        modifier = Modifier.align(Alignment.CenterEnd),
        trackPadding = PaddingValues(top = topInset + 16.dp, bottom = bottomInset + 88.dp)
    )
    }
}
