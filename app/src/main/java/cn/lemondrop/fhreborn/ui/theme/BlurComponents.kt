package cn.lemondrop.fhreborn.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.blendColors
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 模糊半径（像素）。顶栏/底栏对 backdrop 层的实时模糊强度。 */
private const val BackdropBlurRadius = 60f

/** 叠加在模糊上的表面色不透明度（磨砂玻璃观感，越高越不透） */
private const val BackdropSurfaceAlpha = 0.8f

const val TITLE_BAR_STYLE_GAUSSIAN = "gaussian"
const val TITLE_BAR_STYLE_PROGRESSIVE = "progressive"

val LocalTitleBarStyle = staticCompositionLocalOf { TITLE_BAR_STYLE_GAUSSIAN }
val LocalBlurBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

data class TitleBarProgressiveConfig(
    val blurRadius: Float = 10f,
    val startFraction: Float = 0f,
    val endFraction: Float = 1f,
    val curve: Float = 2.2f
) {
    fun toProgressiveBlur(): ProgressiveBlur {
        return ProgressiveBlur.Top.copy(
            startFraction = startFraction.coerceIn(0f, 1f),
            endFraction = endFraction.coerceIn(0f, 1f),
            curve = curve.coerceAtLeast(0.01f)
        )
    }
}

val LocalTitleBarProgressiveConfig = staticCompositionLocalOf {
    TitleBarProgressiveConfig()
}

@Composable
private fun rememberProgressiveBackdropEffectColors(): BlurColors {
    val surfaceColor = MiuixTheme.colorScheme.surface
    return remember(surfaceColor) {
        BlurColors(
            blendColors = listOf(
                BlendColorEntry(color = surfaceColor.copy(alpha = 0.3f))
            )
        )
    }
}

/** 当前平台是否支持 RuntimeShader（Android 13+）——支持时才走真实模糊 */
internal val isRuntimeShaderSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * 顶栏/底栏的磨砂背景：模糊 + 表面色叠加。
 * 在 composable 上下文构造（BlurDefaults.blurColors 是 @Composable remember）。
 */
@Composable
private fun rememberBackdropEffectColors(): BlurColors {
    val surfaceColor = MiuixTheme.colorScheme.surface
    return remember(surfaceColor) {
        BlurColors(
            blendColors = listOf(
                BlendColorEntry(color = surfaceColor.copy(alpha = BackdropSurfaceAlpha))
            )
        )
    }
}

/**
 * 使用 Miuix 磨砂玻璃色值的 SmallTopAppBar 包装（滚动感知）。
 *
 * @param backdrop 非空且平台支持时，滚动后顶栏透明并对页面内容做真实模糊 + 表面色叠加；
 * @param scrolled 列表是否已滚动（内容滚到顶栏下）——false 时顶栏完全透明（无背景），
 *                 true 时显示模糊（或回退色值）
 */
@Composable
fun BlurTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    backdrop: LayerBackdrop? = null,
    scrolled: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val useBlur = backdrop != null && isRuntimeShaderSupported && scrolled
    val isProgressive = LocalTitleBarStyle.current == TITLE_BAR_STYLE_PROGRESSIVE
    if (useBlur) {
        val effectColors = rememberBackdropEffectColors()
        // 真实模糊采样层尚未就绪或滚动过渡期间，先铺一层半透明表面色兜底，
        // 避免状态栏/顶栏区域短暂变成全透明而丢失磨砂材质。
        val fallbackColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
        Box(
            modifier = if (isProgressive) {
                modifier
                    .fillMaxWidth()
                    .background(fallbackColor)
            } else {
                modifier
                    .fillMaxWidth()
                    .background(fallbackColor)
                    .textureBlur(
                        backdrop = backdrop!!,
                        shape = RoundedCornerShape(0.dp),
                        blurRadius = 40f,
                        colors = effectColors
                    )
            }
        ) {
            if (isProgressive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .progressiveTextureBlur(
                            backdrop = backdrop!!,
                            shape = RoundedCornerShape(0.dp),
                            blurRadius = LocalTitleBarProgressiveConfig.current.blurRadius,
                            gradient = LocalTitleBarProgressiveConfig.current.toProgressiveBlur(),
                            colors = rememberProgressiveBackdropEffectColors()
                        )
                )
            }
            SmallTopAppBar(
                title = title,
                color = Color.Transparent,
                subtitle = subtitle,
                navigationIcon = navigationIcon,
                actions = actions,
            )
        }
    } else {
        SmallTopAppBar(
            title = title,
            // 未滚动：完全透明（顶栏下无重要内容）；已滚动但平台不支持模糊：回退磨砂色值
            color = if (scrolled) MiuixTheme.colorScheme.surfaceContainer else Color.Transparent,
            subtitle = subtitle,
            navigationIcon = navigationIcon,
            actions = actions,
        )
    }
}

/**
 * 使用 Miuix 磨砂玻璃色值的 NavigationBar 包装（带顶部描边线）。
 *
 * @param backdrop 非空且平台支持时，底栏透明并对页面内容做真实模糊 + 表面色叠加；
 *                 否则回退色值。
 */
@Composable
fun BlurNavigationBar(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val useBlur = backdrop != null && isRuntimeShaderSupported
    if (useBlur) {
        val effectColors = rememberBackdropEffectColors()
        Box(
            modifier = modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop!!,
                    shape = { RoundedCornerShape(0.dp) },
                    effects = {
                        blur(radiusX = BackdropBlurRadius)
                        blendColors(effectColors)
                    }
                )
        ) {
            NavigationBar(
                color = Color.Transparent,
                showDivider = true,
                content = content,
            )
        }
    } else {
        NavigationBar(
            color = MiuixTheme.colorScheme.surfaceContainer,
            showDivider = true,
            content = content,
        )
    }
}
