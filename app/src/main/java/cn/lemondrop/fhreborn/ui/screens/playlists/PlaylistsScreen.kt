package cn.lemondrop.fhreborn.ui.screens.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import cn.lemondrop.fhreborn.ui.components.MainScaffold
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import io.github.composefluent.component.Text

@Composable
fun PlaylistsScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onPlayerClick: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    MainScaffold(
        playerViewModel = playerViewModel,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        title = {
            Text(
                text = "歌单",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        onPlayerClick = onPlayerClick
    ) { paddingValues, bottomOverlayHeight ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "歌单页面（占位）",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
