package cn.lemondrop.fhreborn.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverBottomSheet
import cn.lemondrop.clover.CloverHazeDefaults
import cn.lemondrop.clover.CloverMenuItem
import cn.lemondrop.fhreborn.Screen
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Clover
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Zap
import dev.chrisbanes.haze.HazeState

/**
 * 应用 Drawer 菜单
 *
 * @param visible 是否显示
 * @param onDismiss 关闭回调
 * @param currentRoute 当前路由，用于高亮当前项
 * @param onNavigate 导航回调；非页面项通过 onExtraClick 处理
 * @param hazeState Haze 状态
 * @param modifier 外部 modifier
 */
@Composable
fun AppDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible) {
        onDismiss()
    }

    if (!visible) return

    CloverBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        hazeState = hazeState,
        hazeTints = CloverHazeDefaults.tints(
            baseColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.40f)
        )
    ) {
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

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(8.dp)
        )

        // 快捷操作（当前为占位/调试入口）
        CloverMenuItem(
            label = "音频输出",
            icon = Lucide.Headphones,
            onClick = onDismiss
        )
        CloverMenuItem(
            label = "定时播放",
            icon = Lucide.Timer,
            onClick = onDismiss
        )
        DrawerNavItem(
            route = Screen.CloverDemo.route,
            label = "Clover Demo",
            icon = Lucide.Clover,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = onDismiss
        )
        DrawerNavItem(
            route = Screen.HazeDemo.route,
            label = "Haze Demo",
            icon = Lucide.Zap,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            onDismiss = onDismiss
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
    CloverMenuItem(
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
