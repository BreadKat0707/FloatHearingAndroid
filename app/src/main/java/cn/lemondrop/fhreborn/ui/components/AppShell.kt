package cn.lemondrop.fhreborn.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.Screen
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.X
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 大屏判定阈值（dp）：宽度 >= 该值显示常驻侧边栏 */
private val LARGE_SCREEN_MIN_WIDTH_DP = 600

/** 侧边栏宽度（MiniPlayBar 等全局元素需要知道以避让） */
val SidebarWidth = 280.dp

/**
 * 应用自适应外壳：大屏下侧边栏常驻并挤开内容，小屏下抽屉以 BottomSheet 弹出。
 *
 * 用法：包在页面最外层（AppBackgroundLayer 和 Scaffold 的父级）。
 *
 * @param drawerVisible 抽屉是否展开（由页面 menu 按钮控制）
 * @param onDismissDrawer 关闭抽屉回调
 * @param currentRoute 当前路由，用于高亮当前项
 * @param onNavigate 导航回调
 * @param onScheduledPauseClick 点击"计划暂停"菜单项的回调
 * @param content 页面内容（背景 + Scaffold）
 */
@Composable
fun AppShell(
    drawerVisible: Boolean,
    onDismissDrawer: () -> Unit,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onScheduledPauseClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= LARGE_SCREEN_MIN_WIDTH_DP

    if (isLargeScreen) {
        LargeScreenShell(
            drawerVisible = drawerVisible,
            onDismissDrawer = onDismissDrawer,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onScheduledPauseClick = onScheduledPauseClick,
            modifier = modifier,
            content = content
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            FhBottomSheet(
                show = drawerVisible,
                onDismissRequest = onDismissDrawer,
                backgroundColor = MiuixTheme.colorScheme.surfaceContainer
            ) {
                // FhBottomSheet 已统一处理底部导航栏留白
                DrawerContent(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    onDismiss = onDismissDrawer,
                    onScheduledPauseClick = onScheduledPauseClick
                )
            }
        }
    }
}

/**
 * 大屏布局：侧边栏 + 内容区。侧边栏展开时占用左侧区域，把内容挤到右边；
 * 收起时侧边栏滑出，内容占满全宽。
 */
@Composable
private fun LargeScreenShell(
    drawerVisible: Boolean,
    onDismissDrawer: () -> Unit,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onScheduledPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BackHandler(enabled = drawerVisible) {
        onDismissDrawer()
    }

    Row(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = drawerVisible,
            enter = slideInHorizontally(animationSpec = tween(250)) { -it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(animationSpec = tween(200)) { -it } + fadeOut(tween(150)),
            modifier = Modifier.fillMaxHeight()
        ) {
            SidebarPanel(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                onScheduledPauseClick = onScheduledPauseClick
            )
        }

        // 内容区：被侧边栏挤到右侧，侧边栏收起时自动占满
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}

/**
 * 大屏侧边栏面板：固定宽度，可滚动导航项（常驻，无关闭按钮）。
 */
@Composable
private fun SidebarPanel(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onScheduledPauseClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(SidebarWidth)
            .background(MiuixTheme.colorScheme.surfaceContainer)
    ) {
        // 常驻侧边栏：无标题/关闭按钮（由 menu 按钮 toggle），点击导航不收起
        DrawerContent(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = {},
            onScheduledPauseClick = onScheduledPauseClick
        )
    }
}

/**
 * 抽屉内的导航项列表（大屏侧边栏与小屏 BottomSheet 共用）。
 */
@Composable
private fun DrawerContent(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
    onScheduledPauseClick: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        // 主页面导航
        DrawerNavItem(
            route = Screen.Library.route,
            label = "媒体库",
            icon = Lucide.Music,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = onDismiss
        )
        DrawerNavItem(
            route = Screen.Playlists.route,
            label = "歌单",
            icon = Lucide.ListMusic,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = onDismiss
        )
        DrawerNavItem(
            route = Screen.FolderBrowser.route,
            label = "浏览路径",
            icon = Lucide.FolderOpen,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = onDismiss
        )
        DrawerNavItem(
            route = Screen.Ideas.route,
            label = "想法",
            icon = Lucide.Lightbulb,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = onDismiss
        )
        DrawerNavItem(
            route = Screen.Statistics.route,
            label = "统计和数据分析",
            icon = Lucide.Activity,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = onDismiss
        )
        DrawerNavItem(
            route = Screen.Settings.route,
            label = "设置",
            icon = Lucide.Settings,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = onDismiss
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 快捷操作
        MenuItemRow(
            label = "音频输出",
            icon = Lucide.Headphones,
            onClick = onDismiss
        )
        MenuItemRow(
            label = "计划暂停",
            icon = Lucide.Timer,
            onClick = {
                onDismiss()
                onScheduledPauseClick()
            }
        )
        DrawerNavItem(
            route = Screen.MicaDemo.route,
            label = "Mica Demo",
            icon = Lucide.Palette,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun DrawerNavItem(
    route: String,
    label: String,
    icon: ImageVector,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    MenuItemRow(
        label = label,
        icon = icon,
        isSelected = currentRoute == route,
        onClick = {
            onDismiss()
            if (currentRoute != route) {
                onNavigate(route)
            }
        }
    )
}

@Composable
private fun MenuItemRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
        )
    }
}
