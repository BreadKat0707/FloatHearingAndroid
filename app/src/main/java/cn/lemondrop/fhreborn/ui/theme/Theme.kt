package cn.lemondrop.fhreborn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun FloatHearingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    themeMode: String = "system",
    accentColor: Color = AppColors.accent,
    foregroundMode: String = "auto",
    content: @Composable () -> Unit
) {
    // 统一走 ThemeController 单分支（避免 if/else 两个 MiuixTheme 重载切换导致子树组合重建，
    // 重建会让页面 remember 状态丢失——如设置页 currentPage 在切换 Material You 时被重置跳回主页）。
    // 静态模式用 System/Light/Dark（承载自定义 accent 色板），动态取色用 Monet*（跟随壁纸）。
    val colorSchemeMode = when {
        useDynamicColor -> when (themeMode) {
            "light" -> ColorSchemeMode.MonetLight
            "dark" -> ColorSchemeMode.MonetDark
            else -> ColorSchemeMode.MonetSystem
        }
        else -> when (themeMode) {
            "light" -> ColorSchemeMode.Light
            "dark" -> ColorSchemeMode.Dark
            else -> ColorSchemeMode.System
        }
    }
    val lightColors = remember(accentColor, foregroundMode) { accentColorScheme(accentColor, isDark = false, foregroundMode) }
    val darkColors = remember(accentColor, foregroundMode) { accentColorScheme(accentColor, isDark = true, foregroundMode) }
    val controller = remember(colorSchemeMode, lightColors, darkColors) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            lightColors = lightColors,
            darkColors = darkColors
        )
    }
    MiuixTheme(
        controller = controller,
        textStyles = MiuixTextStyles,
        content = content
    )
}
