package cn.lemondrop.fhreborn.ui.theme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 使用 Miuix 磨砂玻璃色值的 SmallTopAppBar 包装。
 */
@Composable
fun BlurTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    SmallTopAppBar(
        title = title,
        color = MiuixTheme.colorScheme.surfaceContainer,
        subtitle = subtitle,
        navigationIcon = navigationIcon,
        actions = actions,
    )
}

/**
 * 使用 Miuix 磨砂玻璃色值的 NavigationBar 包装。
 */
@Composable
fun BlurNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    NavigationBar(
        color = MiuixTheme.colorScheme.surfaceContainer,
        showDivider = false,
        content = content,
    )
}
