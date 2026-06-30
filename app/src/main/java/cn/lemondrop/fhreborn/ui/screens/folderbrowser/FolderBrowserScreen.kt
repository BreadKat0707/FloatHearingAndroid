package cn.lemondrop.fhreborn.ui.screens.folderbrowser

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.ui.screens.library.FolderBrowserOverlay
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel

/**
 * 浏览路径页面：直接复用媒体库的文件管理器式覆盖层（FolderBrowserOverlay），
 * 自行从 LibraryViewModel 取全部歌曲构建文件树。
 *
 * 这样从任意页面进入「浏览路径」都显示真正的文件夹浏览，而非占位页。
 */
@Composable
fun FolderBrowserScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(context.applicationContext as Application)
    )
    val songs by viewModel.songs.collectAsState(initial = emptyList())

    FolderBrowserOverlay(
        songs = songs,
        initialPath = emptyList(),
        playerViewModel = playerViewModel,
        onDismiss = onBack
    )
}
