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
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
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
    val hiddenFolders by libraryViewModel.hiddenFolders.collectAsState(initial = emptySet())
    // 仅直接子文件：与媒体库-文件夹 tab 的文件夹划分一致
    val folderSongs = remember(allSongs, folderPath) {
        allSongs.filter { it.path.substringBeforeLast('/') == folderPath }
    }
    val folderName = folderPath.substringAfterLast('/').ifBlank { folderPath }
    val isHidden = hiddenFolders.contains(folderPath)
    // 当前文件夹下被隐藏的直接子文件夹（取消隐藏入口）
    val hiddenSubFolders = remember(allSongs, folderPath, hiddenFolders) {
        allSongs
            .map { it.path.substringBeforeLast('/') }
            .filter { it.startsWith("$folderPath/") }
            .map { it.removePrefix("$folderPath/") }
            .filter { !it.contains('/') }
            .map { "$folderPath/$it" }
            .distinct()
            .filter { it in hiddenFolders }
            .sortedBy { it.lowercase() }
    }

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
                },
                actions = {
                    // 播放整个文件夹
                    IconButton(onClick = {
                        if (folderSongs.isNotEmpty()) playerViewModel.playSongs(folderSongs, 0)
                    }) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = "播放文件夹",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                    // 已隐藏的文件夹：提供取消隐藏入口
                    if (isHidden) {
                        top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu(
                            entries = listOf(
                                top.yukonga.miuix.kmp.basic.DropdownEntry(
                                    items = listOf(
                                        top.yukonga.miuix.kmp.basic.DropdownItem(
                                            "取消隐藏",
                                            icon = { mod -> Icon(Lucide.EyeOff, null, modifier = mod) },
                                            onClick = { libraryViewModel.unhideFolder(folderPath) }
                                        )
                                    )
                                )
                            ),
                            minHeight = 40.dp,
                            minWidth = 40.dp,
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "更多"
                            )
                        }
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
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + bottomOverlayHeight
                )
            ) {
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

                // 已隐藏的子文件夹：单独列出，对应"取消隐藏"
                if (hiddenSubFolders.isNotEmpty()) {
                    item {
                        Text(
                            text = "已隐藏的文件夹",
                            style = MiuixTheme.textStyles.title3,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(hiddenSubFolders, key = { it }) { subFolder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = subFolder.substringAfterLast('/'),
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                text = "取消隐藏",
                                onClick = { libraryViewModel.unhideFolder(subFolder) }
                            )
                        }
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
            onPlayNext = { playerViewModel.playNext(listOf(song)) },
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
