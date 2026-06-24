package cn.lemondrop.fhreborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverSizes
import cn.lemondrop.clover.CloverTitleBar
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import io.github.composefluent.component.Icon

/**
 * 主页面通用布局：内容 + MiniPlayBar + 可选底部插槽 + 底部标题栏 + Drawer 菜单
 *
 * 只有底部标题栏/导航栏共用一块亚克力材质，MiniPlayBar 浮在亚克力上方、不参与模糊。
 * 亚克力材质模糊的是其正下方的所有页面内容。
 */
@Composable
fun MainScaffold(
    playerViewModel: PlayerViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable (() -> Unit)? = null,
    onPlayerClick: () -> Unit,
    content: @Composable (paddingValues: PaddingValues, bottomOverlayHeight: Dp) -> Unit
) {
    val hazeState = remember { HazeState() }
    var showDrawer by remember { mutableStateOf(false) }

    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val statusBarPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()

    val barHazeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val barHazeTintColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val miniPlayBarHeight = 64.dp
    val acrylicBarHeight = CloverSizes.titleBarHeight +
            if (bottomBar != null) CloverSizes.titleBarHeight else 0.dp +
            16.dp
    val bottomOverlayHeight = miniPlayBarHeight + 8.dp + acrylicBarHeight + navBarPadding

    // 只给顶部留出状态栏安全距离；底部不裁剪，让内容延伸到亚克力底栏下方参与模糊
    val contentPadding = PaddingValues(top = statusBarPadding + 8.dp)

    val menuButton: @Composable () -> Unit = {
        FluentIconButton(onClick = { showDrawer = true }) {
            Icon(
                imageVector = Lucide.Menu,
                contentDescription = "菜单",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 页面主体作为 Haze 源：占满全屏，内容可延伸至底栏下方
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            content(contentPadding, bottomOverlayHeight)
        }

        // MiniPlayBar 浮在亚克力面板上方，自身不参与模糊
        MiniPlayBar(
            playerViewModel = playerViewModel,
            onClick = onPlayerClick,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = acrylicBarHeight + 8.dp)
        )

        // 底部标题栏 + 可选导航栏共用一块亚克力背景
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .hazeEffect(state = hazeState) {
                    blurRadius = 40.dp
                    backgroundColor = barHazeBackgroundColor
                    tints = listOf(HazeTint(barHazeTintColor))
                    noiseFactor = 0.1f
                }
                .pointerInput(Unit) {
                    // 拦截亚克力面板空白区域的点击，不触发后面列表项的点击
                    detectTapGestures(onTap = { })
                }
        ) {
            bottomBar?.invoke()

            if (bottomBar != null) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            CloverTitleBar(
                title = title,
                leading = menuButton,
                trailing = actions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navBarPadding),
                backgroundColor = null
            )
        }

        AppDrawer(
            visible = showDrawer,
            onDismiss = { showDrawer = false },
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            hazeState = hazeState
        )
    }
}
