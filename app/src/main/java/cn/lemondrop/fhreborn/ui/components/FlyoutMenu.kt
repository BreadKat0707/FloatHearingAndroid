package cn.lemondrop.fhreborn.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverMenuItem
import cn.lemondrop.clover.CloverSizes
import cn.lemondrop.clover.isCloverDark
import cn.lemondrop.clover.material.cloverAcrylic
import dev.chrisbanes.haze.HazeState

/**
 * 通用 Flyout 菜单项
 *
 * @param label 显示文字
 * @param icon 图标
 * @param onClick 点击回调
 */
data class FlyoutMenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * 通用 Flyout 菜单
 *
 * - 显示在屏幕右下角、系统导航栏上方
 * - 使用 [hazeState] 实现亚克力背景（模糊底层内容）
 * - 菜单项高度由内容决定，不会在末尾留出多余空白
 *
 * @param visible 是否显示
 * @param onDismiss 关闭回调
 * @param items 菜单项列表
 * @param hazeState 页面级 Haze 源状态；传入则启用亚克力模糊，否则用纯色背景兜底
 * @param modifier 外部 modifier
 */
@Composable
fun FlyoutMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    items: List<FlyoutMenuItem>,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isCloverDark()
    val solidBg = if (isDark) Color(0xFF2B2B2B) else Color(0xFFF3F3F3)
    val scrim = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.15f)
    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()
        .calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        BackHandler(enabled = visible) {
            onDismiss()
        }

        // 遮罩：仅淡入淡出
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // 菜单面板：带缩放 + 上滑 + 淡入
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = navBarPadding, end = 4.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)) +
                        slideInVertically(animationSpec = tween(250)) { it / 3 },
                exit = scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(180)
                ) + fadeOut(animationSpec = tween(150)) +
                        slideOutVertically(animationSpec = tween(180)) { it / 3 }
            ) {
                val panelModifier = Modifier
                    .width(210.dp)
                    .clip(RoundedCornerShape(CloverSizes.menuCornerRadius))

                Column(
                    modifier = if (hazeState != null) {
                        panelModifier
                            .cloverAcrylic(
                                state = hazeState,
                                backgroundColor = solidBg.copy(alpha = 0.15f),
                                blurRadius = 60.dp
                            )
                    } else {
                        panelModifier.background(solidBg)
                    }
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = {}
                        )
                        .padding(4.dp)
                ) {
                    items.forEach { item ->
                        CloverMenuItem(
                            label = item.label,
                            icon = item.icon,
                            onClick = {
                                onDismiss()
                                item.onClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 更灵活的 Flyout：可自定义内容，不传菜单项列表
 */
@Composable
fun FlyoutMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isCloverDark()
    val solidBg = if (isDark) Color(0xFF2B2B2B) else Color(0xFFF3F3F3)
    val scrim = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.15f)
    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()
        .calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        BackHandler(enabled = visible) {
            onDismiss()
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = navBarPadding, end = 4.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)) +
                        slideInVertically(animationSpec = tween(250)) { it / 3 },
                exit = scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(180)
                ) + fadeOut(animationSpec = tween(150)) +
                        slideOutVertically(animationSpec = tween(180)) { it / 3 }
            ) {
                val panelModifier = Modifier
                    .width(210.dp)
                    .clip(RoundedCornerShape(CloverSizes.menuCornerRadius))

                Column(
                    modifier = if (hazeState != null) {
                        panelModifier
                            .cloverAcrylic(
                                state = hazeState,
                                backgroundColor = solidBg.copy(alpha = 0.15f),
                                blurRadius = 60.dp
                            )
                    } else {
                        panelModifier.background(solidBg)
                    }
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = {}
                        )
                        .padding(4.dp)
                ) {
                    content()
                }
            }
        }
    }
}
