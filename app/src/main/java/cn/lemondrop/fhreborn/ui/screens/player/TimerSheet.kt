package cn.lemondrop.fhreborn.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverBottomSheet
import cn.lemondrop.fhreborn.ui.theme.FluentButton
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text

@Composable
fun TimerSheet(
    currentMinutes: Int,
    isEndOfSongTimer: Boolean = false,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onSetEndOfSongTimer: () -> Unit,
    onCancelTimer: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(currentMinutes.coerceAtLeast(15)) }
    var isEndOfSong by remember { mutableStateOf(isEndOfSongTimer) }
    val hasActiveTimer = currentMinutes > 0 || isEndOfSongTimer

    CloverBottomSheet(
        onDismiss = onDismiss,
        title = "定时播放"
    ) {
        // 播完整首歌曲选项
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isEndOfSong) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable { isEndOfSong = !isEndOfSong }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "播完整首歌曲后停止",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isEndOfSong) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isEndOfSong) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isEndOfSong) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 时间选择器
        if (!isEndOfSong) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerButton(
                    icon = Lucide.Minus,
                    onClick = { if (selectedMinutes > 5) selectedMinutes -= 5 }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$selectedMinutes",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "分钟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TimerButton(
                    icon = Lucide.Plus,
                    onClick = { selectedMinutes += 5 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 快捷选项
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(15, 30, 45, 60).forEach { min ->
                    val selected = selectedMinutes == min && !isEndOfSong
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                isEndOfSong = false
                                selectedMinutes = min
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${min}分",
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasActiveTimer) {
                FluentButton(
                    onClick = {
                        onCancelTimer()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消定时")
                }
            }
            FluentButton(
                onClick = {
                    if (isEndOfSong) {
                        onSetEndOfSongTimer()
                    } else {
                        onSetTimer(selectedMinutes)
                    }
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isEndOfSong) "播完整首停止" else "开始定时")
            }
        }
    }
}

@Composable
private fun TimerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FluentIconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
