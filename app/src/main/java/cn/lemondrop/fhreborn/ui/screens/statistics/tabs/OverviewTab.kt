package cn.lemondrop.fhreborn.ui.screens.statistics.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverListItem
import cn.lemondrop.clover.CloverSpacing
import cn.lemondrop.fhreborn.ui.screens.statistics.components.StatHeaderCard
import cn.lemondrop.fhreborn.ui.screens.statistics.formatStatDuration
import cn.lemondrop.fhreborn.ui.viewmodel.StatisticsViewModel
import io.github.composefluent.component.Text

@Composable
fun OverviewTab(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.overviewUiState.collectAsState()

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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(state.songs.size) { index ->
            val song = state.songs[index]
            CloverListItem(
                title = song.title,
                subtitle = "${song.artist} · ${song.album}",
                onClick = { },
                leading = {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
