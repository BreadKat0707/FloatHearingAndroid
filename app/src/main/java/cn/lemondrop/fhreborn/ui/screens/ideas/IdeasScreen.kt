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
import cn.lemondrop.fhreborn.ui.components.AppShell
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

@Composable
fun IdeasScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val drawerVisible = LocalDrawerVisible.current
    val drawerToggle = LocalDrawerToggle.current
    AppShell(
        drawerVisible = drawerVisible.value,
        onDismissDrawer = { drawerVisible.value = false },
        currentRoute = currentRoute,
        onNavigate = { route ->
            onNavigate(route)
        },
        onScheduledPauseClick = { playerViewModel.showScheduledPause() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppBackgroundLayer()
            Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                BlurTopBar(
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
                .padding(padding),
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