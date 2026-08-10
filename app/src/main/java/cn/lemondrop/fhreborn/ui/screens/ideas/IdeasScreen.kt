package cn.lemondrop.fhreborn.ui.screens.ideas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.LocalDrawerToggle
import cn.lemondrop.fhreborn.LocalDrawerVisible
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.CompositionLocalProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@Composable
fun IdeasScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val drawerVisible = LocalDrawerVisible.current
    val drawerToggle = LocalDrawerToggle.current
    Box(modifier = Modifier.fillMaxSize()) {
        // 层背景：顶栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
        val surfaceColor = MiuixTheme.colorScheme.surface
        val backdrop = rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
        Box(modifier = Modifier.fillMaxSize()) {
            AppBackgroundLayer()
            Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                BlurTopBar(
                    backdrop = backdrop,
                    title = "想法",
                navigationIcon = {
                    IconButton(onClick = { drawerToggle() }) {
                        Icon(
                            imageVector = Lucide.Menu,
                            contentDescription = "菜单"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "想法页面（占位）",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
    }
    }
}