package cn.lemondrop.fhreborn.ui.screens.statistics

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.clover.CloverIconButton
import cn.lemondrop.clover.CloverNavItem
import cn.lemondrop.clover.ui.layout.CloverAdaptiveShellScaffold
import cn.lemondrop.clover.ui.layout.CloverShellStrategy
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.components.AppDrawer
import cn.lemondrop.fhreborn.ui.screens.statistics.tabs.MonthTab
import cn.lemondrop.fhreborn.ui.screens.statistics.tabs.OverviewTab
import cn.lemondrop.fhreborn.ui.screens.statistics.tabs.TodayTab
import cn.lemondrop.fhreborn.ui.screens.statistics.tabs.WeekTab
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.StatisticsViewModel
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import io.github.composefluent.component.Text

@Composable
fun StatisticsScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val context = LocalContext.current
    val viewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.Factory(context.applicationContext as Application)
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }

    val tabItems = remember {
        listOf(
            CloverNavItem("今日", Lucide.Clock),
            CloverNavItem("本周", Lucide.Calendar),
            CloverNavItem("本月", Lucide.Calendar),
            CloverNavItem("总览", Lucide.Activity)
        )
    }

    val titleText: @Composable () -> Unit = {
        Text(
            text = "统计和数据分析",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    val menuButton: @Composable () -> Unit = {
        CloverIconButton(
            icon = Lucide.Menu,
            contentDescription = "菜单",
            onClick = { showDrawer = true }
        )
    }

    CloverAdaptiveShellScaffold(
        strategy = CloverShellStrategy.BottomCombined,
        title = titleText,
        navigationIcon = menuButton,
        items = tabItems,
        selectedIndex = selectedTab,
        onItemSelected = { selectedTab = it },
        background = { AppBackgroundLayer() },
        overlay = { state ->
            AppDrawer(
                visible = showDrawer,
                onDismiss = { showDrawer = false },
                currentRoute = currentRoute,
                onNavigate = { route ->
                    showDrawer = false
                    onNavigate(route)
                },
                hazeState = state.hazeState,
                onScheduledPauseClick = { playerViewModel.showScheduledPause() }
            )
        },
        content = { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                when (selectedTab) {
                    0 -> TodayTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    1 -> WeekTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    2 -> MonthTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    3 -> OverviewTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
            }
        }
    )
}

fun formatStatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days}天 ${hours % 24}小时 ${minutes % 60}分钟"
        hours > 0 -> "${hours}小时 ${minutes % 60}分钟"
        minutes > 0 -> "${minutes}分钟"
        else -> "${seconds}秒"
    }
}
