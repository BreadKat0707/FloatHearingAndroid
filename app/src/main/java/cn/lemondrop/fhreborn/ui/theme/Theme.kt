package cn.lemondrop.fhreborn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun FloatHearingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    if (useDynamicColor) {
        val monetMode = when (themeMode) {
            "light" -> ColorSchemeMode.MonetLight
            "dark" -> ColorSchemeMode.MonetDark
            else -> ColorSchemeMode.MonetSystem
        }
        val controller = remember(monetMode) {
            ThemeController(
                colorSchemeMode = monetMode,
                lightColors = AppLightColorScheme,
                darkColors = AppDarkColorScheme,
            )
        }
        MiuixTheme(
            controller = controller,
            textStyles = MiuixTextStyles,
            content = content
        )
    } else {
        val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme
        MiuixTheme(
            colors = colorScheme,
            textStyles = MiuixTextStyles,
            content = content
        )
    }
}
