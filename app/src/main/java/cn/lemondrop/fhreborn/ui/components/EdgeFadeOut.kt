package cn.lemondrop.fhreborn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 列表边缘淡出：内容滚动到顶部/底部边缘时平滑渐隐（DST_IN 渐变，等效
 * Accord/Gramophone 的 FadingVerticalEdgeLayout）。
 *
 * 用法：`LazyColumn(modifier = Modifier.edgeFadeOut(top = 48.dp, bottom = 24.dp))`
 */
@Composable
fun Modifier.edgeFadeOut(top: Dp = 48.dp, bottom: Dp = 24.dp): Modifier {
    val density = LocalDensity.current
    val topPx = with(density) { top.toPx() }
    val bottomPx = with(density) { bottom.toPx() }
    return graphicsLayer {
        // DST_IN 需要离屏缓冲才生效（等效 View 版 saveLayer）；
        // 否则渐变退化为普通叠加，会画出"黑色遮罩"而非淡出
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithContent {
        drawContent()
        // 渐变仅贡献 alpha（DstIn 混合忽略颜色），内容按 alpha 渐隐露出下层背景
        if (topPx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(0f to Color.Transparent, 1f to Color.Black),
                    startY = 0f,
                    endY = topPx
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (bottomPx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(0f to Color.Black, 1f to Color.Transparent),
                    startY = size.height - bottomPx,
                    endY = size.height
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }
}
