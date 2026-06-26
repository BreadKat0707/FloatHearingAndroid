package cn.lemondrop.fhreborn.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.darkColors
import io.github.composefluent.lightColors

// 保留原 Fluent 回退色板，但默认已改用 AppColors
private val LightColorScheme = lightColorScheme(
    primary = FluentLightColors.primary,
    onPrimary = FluentLightColors.onPrimary,
    primaryContainer = FluentLightColors.primaryContainer,
    secondary = FluentLightColors.secondary,
    onSecondary = FluentLightColors.onSecondary,
    surface = FluentLightColors.surface,
    onSurface = FluentLightColors.onSurface,
    surfaceVariant = FluentLightColors.surfaceVariant,
    onSurfaceVariant = FluentLightColors.onSurfaceVariant,
    background = FluentLightColors.background,
    onBackground = FluentLightColors.onBackground,
    outline = FluentLightColors.outline,
    error = FluentLightColors.error,
)

private val DarkColorScheme = darkColorScheme(
    primary = FluentDarkColors.primary,
    onPrimary = FluentDarkColors.onPrimary,
    primaryContainer = FluentDarkColors.primaryContainer,
    secondary = FluentDarkColors.secondary,
    onSecondary = FluentDarkColors.onSecondary,
    surface = FluentDarkColors.surface,
    onSurface = FluentDarkColors.onSurface,
    surfaceVariant = FluentDarkColors.surfaceVariant,
    onSurfaceVariant = FluentDarkColors.onSurfaceVariant,
    background = FluentDarkColors.background,
    onBackground = FluentDarkColors.onBackground,
    outline = FluentDarkColors.outline,
    error = FluentDarkColors.error,
)

@OptIn(ExperimentalFluentApi::class)
@Composable
fun FloatHearingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> if (darkTheme) AppDarkColorScheme else AppLightColorScheme
    }

    // 用当前 Material3 primary 作为 compose-fluent 的 accent
    val fluentColors = remember(colorScheme.primary, darkTheme) {
        if (darkTheme) darkColors(accent = colorScheme.primary)
        else lightColors(accent = colorScheme.primary)
    }

    FluentTheme(colors = fluentColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FluentTypography,
            content = content
        )
    }
}
