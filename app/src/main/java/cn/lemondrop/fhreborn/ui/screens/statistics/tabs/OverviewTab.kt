package cn.lemondrop.fhreborn.ui.screens.statistics.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.ui.screens.statistics.components.StatHeaderCard
import cn.lemondrop.fhreborn.ui.screens.statistics.formatStatDuration
import cn.lemondrop.fhreborn.ui.viewmodel.StatisticsViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OverviewTab(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.overviewUiState.collectAsState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 16.dp,
            bottom = 88.dp
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

        items(state.songs.size) { index ->
            val song = state.songs[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${song.artist} · ${song.album}",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${song.count} 次",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}