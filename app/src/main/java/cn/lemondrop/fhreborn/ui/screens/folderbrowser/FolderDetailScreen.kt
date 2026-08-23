package cn.lemondrop.fhreborn.ui.screens.folderbrowser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import cn.lemondrop.fhreborn.ui.screens.library.SongItem
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 文件夹详情页（媒体库-文件夹 tab 的二级页面）。
 *
 * 展示该文件夹内的歌曲列表（仅直接子文件），点击播放；
 * 与专辑详情页样式一致：BlurTopBar + 层背景模糊 + 全局播放条保留。
 */
@Composable
fun FolderDetailScreen(
    folderPath: String,
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel
) {
    val context = LocalContext.current
    val allSongs by libraryViewModel.songs.collectAsState(initial = emptyList())
    // 仅直接子文件：与媒体库-文件夹 tab 的文件夹划分一致
    val folderSongs = remember(allSongs, folderPath) {
        allSongs.filter { it.path.substringBeforeLast('/') == folderPath }
    }
    val folderName = folderPath.substringAfterLast('/').ifBlank { folderPath }
    val totalDuration = folderSongs.sumOf { it.duration }
    val meta = "${folderSongs.size} 首 · ${formatFolderDuration(totalDuration)}"

    // 歌曲更多菜单（三点菜单）
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var addTargetSongId by remember { mutableStateOf<Long?>(null) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var propertiesSong by remember { mutableStateOf<Song?>(null) }
    var showSongProperties by remember { mutableStateOf(false) }

    // 层背景：顶栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val folderListState = rememberLazyListState()
    // 顶栏滚动感知：列表滚离顶部时显示背景/模糊，回顶隐藏
    val topBarScrolled = remember {
        derivedStateOf {
            folderListState.firstVisibleItemIndex > 0 || folderListState.firstVisibleItemScrollOffset > 0
        }
    }.value

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            BlurTopBar(
                backdrop = backdrop,
                scrolled = topBarScrolled,
                title = folderName,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        val bottomOverlayHeight = LocalGlobalPlayBarHeight.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            LazyColumn(
                state = folderListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + bottomOverlayHeight
                )
            ) {
                // 头部：文件夹图标 + 名称 + 信息 + 播放按钮
                item {
                    FolderHeader(
                        name = folderName,
                        meta = meta,
                        onPlayFolder = {
                            if (folderSongs.isNotEmpty()) playerViewModel.playSongs(folderSongs, 0)
                        }
                    )
                }

                if (folderSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("此文件夹为空", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                } else {
                    items(folderSongs, key = { it.id }) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                playerViewModel.playSongs(folderSongs, folderSongs.indexOf(song))
                            },
                            onMoreClick = { menuSong = song }
                        )
                    }
                }

                // 底部留白，避免内容被迷你播放条遮挡
                item { Spacer(modifier = Modifier.height(bottomOverlayHeight)) }
            }
            // 滚动条：自动淡入淡出，可拖动定位（不渲染在底栏之下层）
            LazyListScrollBar(
                listState = folderListState,
                modifier = Modifier.align(Alignment.CenterEnd),
                trackPadding = PaddingValues(bottom = bottomOverlayHeight)
            )
        }
    }

    // 歌曲更多菜单
    menuSong?.let { song ->
        cn.lemondrop.fhreborn.ui.components.SongMenuSheet(
            song = song,
            onDismiss = { menuSong = null },
            onAddToPlaylist = {
                addTargetSongId = song.id
                menuSong = null
                showAddToPlaylistSheet = true
            },
            onShare = { cn.lemondrop.fhreborn.util.SongFileUtils.shareSong(context, song) },
            onOpenWith = { cn.lemondrop.fhreborn.util.SongFileUtils.openWithOtherApp(context, song) },
            onProperties = {
                propertiesSong = song
                menuSong = null
                showSongProperties = true
            }
        )
    }

    // 歌曲属性弹窗（菜单"属性"）
    if (showSongProperties) {
        BackHandler { showSongProperties = false }
        propertiesSong?.let { song ->
            cn.lemondrop.fhreborn.util.SongFileUtils.SongPropertiesDialog(
                song = song,
                onDismiss = { showSongProperties = false }
            )
        }
    }

    // 加入歌单弹窗
    if (showAddToPlaylistSheet) {
        BackHandler { showAddToPlaylistSheet = false }
        val playlistViewModel: cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel =
            androidx.lifecycle.viewmodel.compose.viewModel(
                factory = cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel.Factory(
                    context.applicationContext as android.app.Application
                )
            )
        cn.lemondrop.fhreborn.ui.components.AddToPlaylistSheet(
            songIds = addTargetSongId?.let { listOf(it) } ?: emptyList(),
            viewModel = playlistViewModel,
            onDismiss = {
                showAddToPlaylistSheet = false
                addTargetSongId = null
            }
        )
    }
}

@Composable
private fun FolderHeader(
    name: String,
    meta: String,
    onPlayFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = name,
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = meta,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onPlayFolder) {
            Icon(
                imageVector = Lucide.Play,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "播放文件夹",
                color = MiuixTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatFolderDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}小时${minutes}分" else "${minutes}分钟"
}
