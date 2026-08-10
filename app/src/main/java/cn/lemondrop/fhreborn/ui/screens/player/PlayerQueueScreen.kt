package cn.lemondrop.fhreborn.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.PlaylistEditSheet
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import cn.lemondrop.fhreborn.ui.components.edgeFadeOut
import cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 底部播放条高度（封面 40dp + 上下 padding 20dp） */
private val QueuePlayBarHeight = 64.dp

/**
 * 全屏播放队列
 *
 * - 替代播放器页面的其他内容，列出完整队列
 * - 系统返回手势 / 顶部下滑 / 拖动条 都会回到播放器
 * - 底部播放条：点击回到播放器，含播放/暂停与队列操作（清空/保存为歌单）
 */
@Composable
fun PlayerQueueScreen(
    queue: List<Song>,
    currentIndex: Int,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onBack: () -> Unit,
    onItemClick: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onCloseDrag: (Float) -> Unit = {},
    onCloseDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val statusBarPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues()
        .calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()
        .calculateBottomPadding()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val cutoutLeft = cutoutPadding.calculateLeftPadding(LayoutDirection.Ltr)
    val cutoutRight = cutoutPadding.calculateRightPadding(LayoutDirection.Ltr)

    // 保存为歌单弹窗
    var showSaveSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 手柄配色与播放器页一致：流体自适应（深色白 / 浅色黑）+ 背景混合
    val isDarkTheme = cn.lemondrop.fhreborn.ui.theme.LocalAppDarkTheme.current
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val targetBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply

    // 进入/切换时滚动到当前播放项（无动画，避免打开时卡帧）
    LaunchedEffect(currentIndex) {
        if (currentIndex in queue.indices) {
            listState.scrollToItem(currentIndex)
        }
    }

    BackHandler {
        onBack()
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 列表滚动到顶部后继续向下滑，交给播放器页面收起队列
                if (available.y > 0f) {
                    onCloseDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                onCloseDragEnd()
                return super.onPostFling(consumed, available)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding, bottom = navBarPadding + 12.dp)
        ) {
            // 头部（文档流占位，列表不会与标题栏重叠）：拖动条 + 透明标题栏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                onCloseDragEnd()
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                // 向下滑动跟手关闭队列
                                if (dragAmount > 0f) {
                                    onCloseDrag(dragAmount)
                                }
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .fluidBlend(targetBlendMode)
                        .background(fluidOnColor.copy(alpha = 0.3f))
                )

                // 自定义标题行（SmallTopAppBar 内部无条件叠加状态栏 inset 会产生空白，故不用）
                // 样式对齐 miuix SmallTopAppBar：title3 字号 + Medium 字重
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentIndex in queue.indices) {
                            "正在播放队列（${currentIndex + 1}/${queue.size}）"
                        } else {
                            "正在播放队列"
                        },
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
            }

            // 列表与播放条同处文档流：列表滚动区域止于播放条上方，互不重叠
            // 顶部/底部边缘淡出：内容滚近透明标题栏与播放条时平滑渐隐
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(nestedScrollConnection)
                    .edgeFadeOut(top = 48.dp, bottom = 24.dp)
                    .padding(start = 16.dp + cutoutLeft, end = 16.dp + cutoutRight),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(queue, key = { index, song -> "${song.id}_$index" }) { index, song ->
                    QueueItem(
                        song = song,
                        isCurrent = index == currentIndex,
                        onClick = { onItemClick(index) },
                        onRemove = { onRemove(index) }
                    )
                }
            }

            // 底部播放条：点击回到播放器；仅播放/暂停 + 队列操作菜单
            QueuePlayBar(
                playerViewModel = playerViewModel,
                onBack = onBack,
                onClearQueue = {
                    playerViewModel.clearQueue()
                    scope.launch {
                        snackbarHostState.showSnackbar("已清空正在播放队列")
                    }
                },
                onSaveAsPlaylist = { showSaveSheet = true },
                modifier = Modifier.padding(start = 16.dp + cutoutLeft, end = 16.dp + cutoutRight)
            )
        }

        // 操作反馈 Snackbar（悬浮于播放条上方）
        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarPadding + QueuePlayBarHeight + 36.dp)
        )
    }

    // 保存为歌单
    if (showSaveSheet) {
        PlaylistEditSheet(
            title = "保存为歌单",
            initialName = "播放队列",
            initialDescription = "",
            coverSongs = queue,
            onDismiss = { showSaveSheet = false },
            onSave = { name, desc, playMode, _, _ ->
                val songIds = queue.map { it.id }
                playlistViewModel.createPlaylist(name, desc) { id ->
                    playlistViewModel.addSongs(id, songIds)
                }
                showSaveSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("已保存为歌单「$name」")
                }
            }
        )
    }
}

/** 底部播放条：封面 + 歌曲信息 + 播放/暂停 + 队列操作菜单；点击空白处回到播放器 */
@Composable
private fun QueuePlayBar(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onClearQueue: () -> Unit,
    onSaveAsPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(QueuePlayBarHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f))
            .clickable(onClick = onBack)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        currentSong?.let { song ->
            SongCoverImage(
                songId = song.id,
                modifier = Modifier.size(40.dp)
            )
        } ?: Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("♪", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentSong?.title ?: "未在播放",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentSong?.artist ?: "",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 播放/暂停（不显示下一首）
        IconButton(onClick = { playerViewModel.playPause() }) {
            Icon(
                imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(22.dp),
                tint = MiuixTheme.colorScheme.onSurface
            )
        }

        // 队列操作菜单
        top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu(
            entries = listOf(
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            "清空队列",
                            icon = { mod -> Icon(Lucide.Trash2, null, modifier = mod) },
                            onClick = onClearQueue
                        ),
                        DropdownItem(
                            "保存为歌单",
                            icon = { mod -> Icon(Lucide.Bookmark, null, modifier = mod) },
                            onClick = onSaveAsPlaylist
                        )
                    )
                )
            ),
            minHeight = 40.dp,
            minWidth = 40.dp,
        ) {
            Icon(
                imageVector = Lucide.EllipsisVertical,
                contentDescription = "更多",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongCoverImage(
            songId = song.id,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MiuixTheme.textStyles.body1,
                color = if (isCurrent) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} - ${song.album}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = "移除",
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

/** 与播放器页一致：离屏混合，让手柄颜色跟随流体背景（深色 Plus / 浅色 Multiply） */
private fun Modifier.fluidBlend(blendMode: BlendMode): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
    this.blendMode = blendMode
}
