package cn.lemondrop.fhreborn.ui.screens.album

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.LocalPlayBarOverride
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.AddToPlaylistSheet
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.MultiSelectToolbar
import cn.lemondrop.fhreborn.ui.components.SelectionStateButton
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Disc
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.ListChecks
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.X
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 专辑详情页。
 *
 * 数据源：按专辑名（可选专辑艺术家）从 [SongDao.getSongsByAlbum] 拉取曲目，
 * 页面内计算总时长/发行年份/参与艺术家等聚合信息。
 */
@Composable
fun AlbumDetailScreen(
    albumName: String,
    albumArtist: String?,
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val songsFlow = remember(albumName, albumArtist) {
        db.songDao().getSongsByAlbum(albumName, albumArtist)
    }
    val songs by songsFlow.collectAsState(initial = emptyList())
    val currentSong by playerViewModel.currentSong.collectAsState()

    val displayAlbum = albumName.ifBlank { "未知专辑" }
    val displayArtist = albumArtist
        ?: songs.firstOrNull()?.albumArtist
        ?: songs.firstOrNull()?.artist
        ?: "未知艺术家"
    val totalDuration = songs.sumOf { it.duration }
    val releaseYear = songs.firstNotNullOfOrNull { it.year }
    val participatingArtists = songs.map { it.artist }.distinct()
    val hasMultipleDiscs = songs.mapNotNull { it.discNumber }.distinct().size > 1
    val coverSongId = songs.firstOrNull()?.id

    val meta = buildString {
        append("${songs.size} 首")
        append(" · ")
        append(formatAlbumDuration(totalDuration))
        releaseYear?.let { append(" · $it") }
    }

    // 多选模式（批量操作）
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateSetOf<Long>() }
    var showBatchAddSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 歌曲更多菜单（三点菜单）
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var showSongProperties by remember { mutableStateOf(false) }
    var propertiesSong by remember { mutableStateOf<Song?>(null) }

    // 多选时隐藏全局播放条（底部由多选工具栏接管）；离开页面时复位
    val playBarOverride = LocalPlayBarOverride.current
    LaunchedEffect(multiSelectMode) {
        playBarOverride.value = multiSelectMode
    }
    DisposableEffect(Unit) {
        onDispose { playBarOverride.value = false }
    }

    // 批量操作依赖（加入歌单 / 删除文件）
    val playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModel.Factory(context.applicationContext as Application)
    )
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(context.applicationContext as Application)
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    // 层背景：顶栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val albumListState = androidx.compose.foundation.lazy.rememberLazyListState()
    // 顶栏滚动感知：列表滚离顶部时显示背景/模糊，回顶隐藏
    val topBarScrolled = remember {
        derivedStateOf {
            albumListState.firstVisibleItemIndex > 0 || albumListState.firstVisibleItemScrollOffset > 0
        }
    }.value

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            BlurTopBar(
                backdrop = backdrop,
                scrolled = topBarScrolled || multiSelectMode,
                title = if (multiSelectMode) "已选 ${selectedSongIds.size} 首" else "专辑",
                navigationIcon = {
                    if (multiSelectMode) {
                        IconButton(onClick = {
                            multiSelectMode = false
                            selectedSongIds.clear()
                        }) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "退出多选"
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = "返回",
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    if (multiSelectMode) {
                        // 选择状态：全选 / 选中多个 / 全未选（退出在标题栏左侧）
                        val allSongIds = songs.map { it.id }
                        SelectionStateButton(
                            selectedCount = selectedSongIds.size,
                            totalCount = allSongIds.size,
                            onSelectAll = {
                                selectedSongIds.clear()
                                selectedSongIds.addAll(allSongIds)
                            },
                            onDeselectAll = { selectedSongIds.clear() }
                        )
                    } else {
                        // 多选入口收纳进三点菜单
                        top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu(
                            entries = listOf(
                                DropdownEntry(
                                    items = listOf(
                                        DropdownItem(
                                            "多选",
                                            icon = { mod -> Icon(Lucide.ListChecks, null, modifier = mod) },
                                            onClick = { multiSelectMode = true }
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
        },
        bottomBar = {
            // 多选底部工具栏（退出在标题栏，工具栏不重复提供）
            if (multiSelectMode) {
                MultiSelectToolbar(
                    selectedCount = selectedSongIds.size,
                    backdrop = backdrop,
                    onAddToPlaylist = {
                        if (selectedSongIds.isNotEmpty()) showBatchAddSheet = true
                    },
                    onAddToQueue = {
                        val list = songs.filter { it.id in selectedSongIds }
                        if (list.isNotEmpty()) {
                            playerViewModel.addToQueue(list)
                            multiSelectMode = false
                            selectedSongIds.clear()
                        }
                    },
                    onShare = {
                        val list = songs.filter { it.id in selectedSongIds }
                        if (list.isNotEmpty()) {
                            cn.lemondrop.fhreborn.util.SongFileUtils.shareSongs(context, list)
                            multiSelectMode = false
                            selectedSongIds.clear()
                        }
                    },
                    onDelete = {
                        if (selectedSongIds.isNotEmpty()) showDeleteConfirm = true
                    }
                )
            }
        }
    ) { padding ->
        val bottomOverlayHeight = LocalGlobalPlayBarHeight.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
        LazyColumn(
            state = albumListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + (if (multiSelectMode) 0.dp else bottomOverlayHeight)
            )
        ) {
            // 头部：封面 + 专辑信息 + 播放按钮
            item {
                AlbumHeader(
                    album = displayAlbum,
                    artist = displayArtist,
                    meta = meta,
                    coverSongId = coverSongId,
                    onPlayAlbum = {
                        if (songs.isNotEmpty()) playerViewModel.playSongs(songs, 0)
                    }
                )
            }

            // 曲目列表（按碟号分组；行内以 Disc + 音轨号替代封面缩略图，
            // 顶部已有封面展示，避免重复加载缩略图）
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                cn.lemondrop.fhreborn.ui.components.FhListItem(
                    title = song.title,
                    summary = "${song.artist} - ${song.album}",
                    onClick = {
                        if (multiSelectMode) {
                            if (song.id in selectedSongIds) {
                                selectedSongIds.remove(song.id)
                            } else {
                                selectedSongIds.add(song.id)
                            }
                        } else {
                            playerViewModel.playSongs(songs, index)
                        }
                    },
                    leading = {
                        Column(horizontalAlignment = Alignment.End) {
                            if (hasMultipleDiscs) {
                                Text(
                                    text = "Disc ${song.discNumber ?: 1}",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                            Text(
                                text = "%02d".format(song.trackNumber ?: (index + 1)),
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    },
                    trailing = {
                        if (multiSelectMode) {
                            cn.lemondrop.fhreborn.ui.components.SelectionIndicator(
                                selected = song.id in selectedSongIds,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            IconButton(onClick = { menuSong = song }) {
                                Icon(
                                    imageVector = Lucide.EllipsisVertical,
                                    contentDescription = "更多",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            }

            // 参与的艺术家
            if (participatingArtists.isNotEmpty()) {
                item {
                    SectionHeader("参与的艺术家")
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        participatingArtists.forEach { artist ->
                            Text(
                                text = artist,
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 底部留白，避免内容被底栏/迷你播放条遮挡
            item { Spacer(modifier = Modifier.height(bottomOverlayHeight)) }
        }
        // 滚动条：自动淡入淡出，可拖动定位
        cn.lemondrop.fhreborn.ui.components.LazyListScrollBar(
            listState = albumListState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        }
    }

    // 多选时返回键退出多选（弹窗的 BackHandler 更晚组合，优先处理）
    if (multiSelectMode) {
        BackHandler {
            multiSelectMode = false
            selectedSongIds.clear()
        }
    }

    // 批量加入歌单弹窗
    if (showBatchAddSheet) {
        BackHandler { showBatchAddSheet = false }
        AddToPlaylistSheet(
            songIds = selectedSongIds.toList(),
            viewModel = playlistViewModel,
            onDismiss = {
                showBatchAddSheet = false
                multiSelectMode = false
                selectedSongIds.clear()
            }
        )
    }

    // 删除确认弹窗
    if (showDeleteConfirm) {
        BackHandler { showDeleteConfirm = false }
        FhBottomSheet(
            show = true,
            onDismissRequest = { showDeleteConfirm = false },
            title = "删除歌曲",
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "确定删除选中的 ${selectedSongIds.size} 首歌曲吗？文件将从设备中移除，此操作不可恢复。",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(text = "取消", onClick = { showDeleteConfirm = false })
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        text = "删除",
                        onClick = {
                            val targetSongs = songs.filter { it.id in selectedSongIds }
                            showDeleteConfirm = false
                            libraryViewModel.deleteSongs(context, targetSongs) { deleted ->
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar("已删除 $deleted 首歌曲")
                                }
                            }
                            multiSelectMode = false
                            selectedSongIds.clear()
                        }
                    )
                }
            }
        }
    }

    // 删除反馈 Snackbar（悬浮于内容区底部）
    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (multiSelectMode) 0.dp else LocalGlobalPlayBarHeight.current + 8.dp)
        )
    }

    // 歌曲更多菜单（三点菜单：查看专辑/分享/打开方式/属性）
    menuSong?.let { song ->
        cn.lemondrop.fhreborn.ui.components.SongMenuSheet(
            song = song,
            onDismiss = { menuSong = null },
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
        androidx.activity.compose.BackHandler { showSongProperties = false }
        propertiesSong?.let { song ->
            cn.lemondrop.fhreborn.util.SongFileUtils.SongPropertiesDialog(
                song = song,
                onDismiss = { showSongProperties = false }
            )
        }
    }
}

@Composable
private fun AlbumHeader(
    album: String,
    artist: String,
    meta: String,
    coverSongId: Long?,
    onPlayAlbum: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (coverSongId != null) {
            SongCoverImage(
                songId = coverSongId,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Disc,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = album,
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = artist,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = meta,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onPlayAlbum) {
            Icon(
                imageVector = Lucide.Play,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "播放专辑",
                color = MiuixTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.title3,
        color = MiuixTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

private fun formatAlbumDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}小时${minutes}分" else "${minutes}分钟"
}