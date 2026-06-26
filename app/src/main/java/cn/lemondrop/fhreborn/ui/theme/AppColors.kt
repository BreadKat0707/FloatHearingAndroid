package cn.lemondrop.fhreborn.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * FloatHearing 统一调色板。
 *
 * 不再完全依赖 Material You 动态取色，避免壁纸/系统主题变化时 UI 颜色混乱。
 * 默认使用这套静态配色；设置里开启动态颜色时才回退到系统动态色。
 */
object AppColors {

    // 主题强调色
    val accent = Color(0xFF869AFF)
    val onAccent = Color(0xFF000000)

    // Light
    val primaryLight = accent
    val onPrimaryLight = onAccent
    val primaryContainerLight = Color(0xFFDEE5FF)
    val onPrimaryContainerLight = Color(0xFF1A2333)

    val secondaryLight = Color(0xFF5F6368)
    val onSecondaryLight = Color(0xFFFFFFFF)
    val secondaryContainerLight = Color(0xFFEFF1F3)
    val onSecondaryContainerLight = Color(0xFF202124)

    val backgroundLight = Color(0xFFF0F0F0)
    val onBackgroundLight = Color(0xFF1A1A1E)
    val surfaceLight = Color(0xFFFFFFFF)
    val onSurfaceLight = Color(0xFF1A1A1E)
    val surfaceVariantLight = Color(0xFFEFF1F3)
    val onSurfaceVariantLight = Color(0xFF5F6368)

    val outlineLight = Color(0xFF74777F)
    val errorLight = Color(0xFFDC2626)
    val onErrorLight = Color(0xFFFFFFFF)

    // Dark
    val primaryDark = accent
    val onPrimaryDark = onAccent
    val primaryContainerDark = Color(0xFF2A3344)
    val onPrimaryContainerDark = Color(0xFFDEE5FF)

    val secondaryDark = Color(0xFF9AA0A6)
    val onSecondaryDark = Color(0xFF202124)
    val secondaryContainerDark = Color(0xFF3C4043)
    val onSecondaryContainerDark = Color(0xFFEFF1F3)

    val backgroundDark = Color(0xFF0F0F11)
    val onBackgroundDark = Color(0xFFE9ECEF)
    val surfaceDark = Color(0xFF1A1A1E)
    val onSurfaceDark = Color(0xFFE9ECEF)
    val surfaceVariantDark = Color(0xFF252528)
    val onSurfaceVariantDark = Color(0xFF9AA0A6)

    val outlineDark = Color(0xFF8E9199)
    val errorDark = Color(0xFFEF4444)
    val onErrorDark = Color(0xFF1A1A1E)
}

val AppLightColorScheme = lightColorScheme(
    primary = AppColors.primaryLight,
    onPrimary = AppColors.onPrimaryLight,
    primaryContainer = AppColors.primaryContainerLight,
    onPrimaryContainer = AppColors.onPrimaryContainerLight,
    secondary = AppColors.secondaryLight,
    onSecondary = AppColors.onSecondaryLight,
    secondaryContainer = AppColors.secondaryContainerLight,
    onSecondaryContainer = AppColors.onSecondaryContainerLight,
    background = AppColors.backgroundLight,
    onBackground = AppColors.onBackgroundLight,
    surface = AppColors.surfaceLight,
    onSurface = AppColors.onSurfaceLight,
    surfaceVariant = AppColors.surfaceVariantLight,
    onSurfaceVariant = AppColors.onSurfaceVariantLight,
    outline = AppColors.outlineLight,
    error = AppColors.errorLight,
    onError = AppColors.onErrorLight
)

val AppDarkColorScheme = darkColorScheme(
    primary = AppColors.primaryDark,
    onPrimary = AppColors.onPrimaryDark,
    primaryContainer = AppColors.primaryContainerDark,
    onPrimaryContainer = AppColors.onPrimaryContainerDark,
    secondary = AppColors.secondaryDark,
    onSecondary = AppColors.onSecondaryDark,
    secondaryContainer = AppColors.secondaryContainerDark,
    onSecondaryContainer = AppColors.onSecondaryContainerDark,
    background = AppColors.backgroundDark,
    onBackground = AppColors.onBackgroundDark,
    surface = AppColors.surfaceDark,
    onSurface = AppColors.onSurfaceDark,
    surfaceVariant = AppColors.surfaceVariantDark,
    onSurfaceVariant = AppColors.onSurfaceVariantDark,
    outline = AppColors.outlineDark,
    error = AppColors.errorDark,
    onError = AppColors.onErrorDark
)
