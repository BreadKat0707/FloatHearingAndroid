package cn.lemondrop.fhreborn.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

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
    primaryVariant = AppColors.primaryLight,
    onPrimaryVariant = AppColors.onPrimaryLight,
    error = AppColors.errorLight,
    onError = AppColors.onErrorLight,
    primaryContainer = AppColors.primaryContainerLight,
    onPrimaryContainer = AppColors.onPrimaryContainerLight,
    secondary = AppColors.secondaryLight,
    onSecondary = AppColors.onSecondaryLight,
    secondaryVariant = AppColors.secondaryLight,
    onSecondaryVariant = AppColors.onSecondaryLight,
    secondaryContainer = AppColors.secondaryContainerLight,
    onSecondaryContainer = AppColors.onSecondaryContainerLight,
    background = AppColors.backgroundLight,
    onBackground = AppColors.onBackgroundLight,
    onBackgroundVariant = AppColors.onBackgroundLight,
    surface = AppColors.surfaceLight,
    onSurface = AppColors.onSurfaceLight,
    surfaceVariant = AppColors.surfaceVariantLight,
    onSurfaceVariantSummary = AppColors.onSurfaceVariantLight,
    outline = AppColors.outlineLight
)

val AppDarkColorScheme = darkColorScheme(
    primary = AppColors.primaryDark,
    onPrimary = AppColors.onPrimaryDark,
    primaryVariant = AppColors.primaryDark,
    onPrimaryVariant = AppColors.onPrimaryDark,
    error = AppColors.errorDark,
    onError = AppColors.onErrorDark,
    primaryContainer = AppColors.primaryContainerDark,
    onPrimaryContainer = AppColors.onPrimaryContainerDark,
    secondary = AppColors.secondaryDark,
    onSecondary = AppColors.onSecondaryDark,
    secondaryVariant = AppColors.secondaryDark,
    onSecondaryVariant = AppColors.onSecondaryDark,
    secondaryContainer = AppColors.secondaryContainerDark,
    onSecondaryContainer = AppColors.onSecondaryContainerDark,
    background = AppColors.backgroundDark,
    onBackground = AppColors.onBackgroundDark,
    onBackgroundVariant = AppColors.onBackgroundDark,
    surface = AppColors.surfaceDark,
    onSurface = AppColors.onSurfaceDark,
    surfaceVariant = AppColors.surfaceVariantDark,
    onSurfaceVariantSummary = AppColors.onSurfaceVariantDark,
    outline = AppColors.outlineDark
)

/** 从设置字符串解析主题色："default"（或空）返回默认色，支持 "#RRGGBB" 格式，失败回退默认 */
fun parseAccentColor(value: String): Color = when {
    value.isBlank() || value.equals("default", ignoreCase = true) -> AppColors.accent
    else -> runCatching {
        val hex = value.removePrefix("#")
        Color(hex.toLong(16) or 0xFF000000L)
    }.getOrDefault(AppColors.accent)
}

/**
 * 基于主题色生成完整 ColorScheme（非 Monet 模式使用）。
 * primary 系颜色由 accent 派生，其余色板沿用 AppColors 静态值。
 */
fun accentColorScheme(accent: Color, isDark: Boolean): Colors {
    val base = if (isDark) AppDarkColorScheme else AppLightColorScheme
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val container = if (isDark) lerp(accent, Color.Black, 0.75f) else lerp(accent, Color.White, 0.82f)
    val onContainer = if (isDark) lerp(accent, Color.White, 0.85f) else lerp(accent, Color.Black, 0.8f)
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryVariant = accent,
        onPrimaryVariant = onAccent,
        primaryContainer = container,
        onPrimaryContainer = onContainer
    )
}
