package cn.lemondrop.fhreborn.ui.screens.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lemondrop.clover.CloverIconButton
import cn.lemondrop.clover.ui.layout.CloverAdaptiveShellScaffold
import cn.lemondrop.clover.ui.layout.CloverShellStrategy
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.components.AppDrawer
import cn.lemondrop.fhreborn.ui.components.MiniPlayBar
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import io.github.composefluent.component.Text

@Composable
fun PlaylistsScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onPlayerClick: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    var showDrawer by remember { mutableStateOf(false) }

    val titleText: @Composable () -> Unit = {
        Text(
            text = "歌单",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    val menuButton: @Composable () -> Unit = {
        CloverIconButton(
            icon = Lucide.Menu,
            contentDescription = "菜单",
            onClick = { showDrawer = true }
        )
    }

    CloverAdaptiveShellScaffold(
        strategy = CloverShellStrategy.BottomCombined,
        title = titleText,
        navigationIcon = menuButton,
        background = { AppBackgroundLayer() },
        overlay = { state ->
            AppDrawer(
                visible = showDrawer,
                onDismiss = { showDrawer = false },
                currentRoute = currentRoute,
                onNavigate = { route ->
                    showDrawer = false
                    onNavigate(route)
                },
                hazeState = state.hazeState,
                onScheduledPauseClick = { playerViewModel.showScheduledPause() }
            )

            MiniPlayBar(
                playerViewModel = playerViewModel,
                onClick = onPlayerClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = state.contentPadding.calculateBottomPadding() + 8.dp
                    )
            )
        },
        content = { state ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = state.contentPadding.calculateTopPadding()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "歌单页面（占位）",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
