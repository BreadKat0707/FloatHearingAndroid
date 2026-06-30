package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverDialog
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.MoonStar
import com.composables.icons.lucide.Plus
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text

/**
 * 计划暂停对话框（统一“定时播放 / 睡眠定时器”）。
 *
 * 两种状态：
 * - 设置态（无活动计时）：H:M 步进器 + 「播完当前曲目暂停」开关 + 「播完本曲」选项，按钮「开始」。
 * - 运行态（有活动计时）：显示倒计时 + 「播完当前曲目暂停」开关，按钮「停止」。
 *
 * @param timerRemaining 时间模式剩余毫秒（0 表示无时间计时）
 * @param isEndOfSongTimer 是否处于“播完当前曲目即暂停”状态
 * @param pauseAfterCurrentSong 到点后是否等当前曲目播完再暂停
 * @param onSetTimer 启动时间计时（分钟）
 * @param onSetEndOfSong 立即进入“播完当前曲目即暂停”
 * @param onSetPauseAfterCurrentSong 切换“播完当前曲目暂停”
 * @param onCancel 取消当前计时
 * @param onDismiss 关闭对话框
 */
@Composable
fun ScheduledPauseDialog(
    timerRemaining: Long,
    isEndOfSongTimer: Boolean,
    pauseAfterCurrentSong: Boolean,
    onSetTimer: (Int) -> Unit,
    onSetEndOfSong: () -> Unit,
    onSetPauseAfterCurrentSong: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasActiveTimer = timerRemaining > 0 || isEndOfSongTimer

    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
    val totalMinutes = hours * 60 + minutes

    CloverDialog(
        onDismissRequest = onDismiss,
        title = "计划暂停",
        buttons = {
            if (hasActiveTimer) {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
                TextButton(
                    onClick = {
                        onCancel()
                        onDismiss()
                    }
                ) {
                    Text("停止")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                TextButton(
                    enabled = totalMinutes > 0,
                    onClick = {
                        onSetTimer(totalMinutes)
                        onDismiss()
                    }
                ) {
                    Text("开始")
                }
            }
        }
    ) {
        if (hasActiveTimer) {
            // 运行态：倒计时 + 开关
            val statusText = if (timerRemaining > 0) {
                "剩余 ${formatRemaining(timerRemaining)}"
            } else {
                "播完当前曲目后暂停"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            PauseAfterSongSwitch(
                checked = pauseAfterCurrentSong,
                onCheckedChange = onSetPauseAfterCurrentSong
            )
        } else {
            // 设置态：H:M 步进器 + 开关 + 「播完本曲」
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Stepper(
                    value = hours,
                    unit = "小时",
                    onDecrease = { if (hours > 0) hours -= 1 },
                    onIncrease = { if (hours < 23) hours += 1 }
                )
                Stepper(
                    value = minutes,
                    unit = "分钟",
                    onDecrease = { if (minutes > 0) minutes -= 1 },
                    onIncrease = { if (minutes < 59) minutes += 1 }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            PauseAfterSongSwitch(
                checked = pauseAfterCurrentSong,
                onCheckedChange = onSetPauseAfterCurrentSong
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 「播完本曲」：立即进入“播完当前曲目即暂停”
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onSetEndOfSong()
                        onDismiss()
                    }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.MoonStar,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "播完本曲",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PauseAfterSongSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "播完当前曲目暂停",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switcher(
            checked = checked,
            onCheckStateChange = onCheckedChange
        )
    }
}

@Composable
private fun Stepper(
    value: Int,
    unit: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FluentIconButton(onClick = onDecrease) {
            Icon(
                imageVector = Lucide.Minus,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FluentIconButton(onClick = onIncrease) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
