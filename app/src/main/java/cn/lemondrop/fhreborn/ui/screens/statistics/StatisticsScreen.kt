package cn.lemondrop.fhreborn.ui.screens.statistics

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import cn.lemondrop.fhreborn.LocalDrawerToggle
import cn.lemondrop.fhreborn.LocalDrawerVisible
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import cn.lemondrop.fhreborn.ui.theme.BlurNavigationBar
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    // 顶栏滚动感知：当前 Tab 列表滚离顶部时显示背景/模糊，回顶隐藏
    var topBarScrolled by remember { mutableStateOf(false) }

    val drawerVisible = LocalDrawerVisible.current
    val drawerToggle = LocalDrawerToggle.current

    val tabItems = remember {
        listOf(
            "今日" to Lucide.Clock,
            "本周" to Lucide.Calendar,
            "本月" to Lucide.Calendar,
            "总览" to Lucide.Activity
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 层背景：顶栏/底栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
        val surfaceColor = MiuixTheme.colorScheme.surface
        val backdrop = rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                topBar = {
                    BlurTopBar(
                        backdrop = backdrop,
                        scrolled = topBarScrolled,
                        title = "统计和数据分析",
                    navigationIcon = {
                        IconButton(onClick = { drawerToggle() }) {
                            Icon(
                                imageVector = Lucide.Menu,
                                contentDescription = "菜单"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BlurNavigationBar(backdrop = backdrop) {
                    tabItems.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = icon,
                            label = label
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                val topInset = padding.calculateTopPadding()
                val bottomInset = padding.calculateBottomPadding()
                when (selectedTab) {
                    0 -> TodayTab(viewModel = viewModel, modifier = Modifier.fillMaxSize(), topInset = topInset, bottomInset = bottomInset, onScrolledChange = { topBarScrolled = it })
                    1 -> WeekTab(viewModel = viewModel, modifier = Modifier.fillMaxSize(), topInset = topInset, bottomInset = bottomInset, onScrolledChange = { topBarScrolled = it })
                    2 -> MonthTab(viewModel = viewModel, modifier = Modifier.fillMaxSize(), topInset = topInset, bottomInset = bottomInset, onScrolledChange = { topBarScrolled = it })
                    3 -> OverviewTab(viewModel = viewModel, modifier = Modifier.fillMaxSize(), topInset = topInset, bottomInset = bottomInset, onScrolledChange = { topBarScrolled = it })
                }
            }
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