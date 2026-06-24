package cn.lemondrop.fhreborn.ui.screens.statistics

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.clover.CloverBottomNavbar
import cn.lemondrop.clover.CloverNavItem
import cn.lemondrop.fhreborn.ui.components.MainScaffold
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
import io.github.composefluent.component.Text

@Composable
fun StatisticsScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onPlayerClick: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    val context = LocalContext.current
    val viewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.Factory(context.applicationContext as Application)
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabItems = remember {
        listOf(
            CloverNavItem("今日", Lucide.Clock),
            CloverNavItem("本周", Lucide.Calendar),
            CloverNavItem("本月", Lucide.Calendar),
            CloverNavItem("总览", Lucide.Activity)
        )
    }

    MainScaffold(
        playerViewModel = playerViewModel,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        title = {
            Text(
                text = "统计和数据分析",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        bottomBar = {
            CloverBottomNavbar(
                items = tabItems,
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it },
                modifier = Modifier.fillMaxWidth()
            )
        },
        onPlayerClick = onPlayerClick
    ) { paddingValues, bottomOverlayHeight ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            when (selectedTab) {
                0 -> TodayTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                1 -> WeekTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                2 -> MonthTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                3 -> OverviewTab(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
        }
    }
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
