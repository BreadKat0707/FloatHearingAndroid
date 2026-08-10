package cn.lemondrop.fhreborn.ui.screens.folderbrowser

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.LocalDrawerToggle
import cn.lemondrop.fhreborn.LocalDrawerVisible
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import cn.lemondrop.fhreborn.ui.screens.library.SongItem
import cn.lemondrop.fhreborn.ui.screens.library.FileBrowserItemRow
import cn.lemondrop.fhreborn.ui.screens.library.FileNode
import cn.lemondrop.fhreborn.ui.screens.library.buildFileTree
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 浏览路径页面。
 *
 * 使用与其他页面统一的 CloverAdaptiveShellScaffold + BottomCombined 布局，
 * 底部标题栏左侧为抽屉菜单按钮，地址栏固定在底部标题栏上方。
 */
@Composable
fun FolderBrowserScreen(
    currentRoute: String,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val drawerVisible = LocalDrawerVisible.current
    val drawerToggle = LocalDrawerToggle.current
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(context.applicationContext as Application)
    )
    val songs by viewModel.songs.collectAsState(initial = emptyList())
    val rootNode = remember(songs) { buildFileTree(songs) }
    var currentPath by remember { mutableStateOf(listOf<String>()) }
    val playingSongId by playerViewModel.currentSong.collectAsState()

    val listState = rememberLazyListState()
    // 顶栏滚动感知：列表滚离顶部时显示背景/模糊，回顶隐藏
    val topBarScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }.value

    val currentNode = remember(rootNode, currentPath) {
        var node = rootNode
        for (part in currentPath) {
            node = node.children[part] ?: break
        }
        node
    }

    val foldersInCurrent = remember(currentNode) {
        currentNode.children.values
            .filter { it.isDirectory }
            .sortedBy { it.name.lowercase() }
    }

    val songsInCurrent = remember(currentNode) {
        currentNode.children.values
            .filter { !it.isDirectory && it.song != null }
            .sortedBy { it.name.lowercase() }
            .mapNotNull { it.song }
    }

    val breadcrumb = listOf("根") + currentPath

    BackHandler {
        if (currentPath.isEmpty()) {
            onBack()
        } else {
            currentPath = currentPath.dropLast(1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 层背景：顶栏对其做真实模糊（页面内容捕获进 GraphicsLayer）
        val surfaceColor = MiuixTheme.colorScheme.surface
        val backdrop = rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
        Scaffold(
        topBar = {
            BlurTopBar(
                backdrop = backdrop,
                scrolled = topBarScrolled,
                title = if (currentPath.isEmpty()) "浏览路径" else currentNode.path,
                navigationIcon = {
                    IconButton(onClick = { drawerToggle() }) {
                        Icon(
                            imageVector = Lucide.Menu,
                            contentDescription = "菜单",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        val bottomOverlayHeight = LocalGlobalPlayBarHeight.current
        val addressBarHeight = 48.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + bottomOverlayHeight + addressBarHeight + 16.dp
                )
            ) {
                if (foldersInCurrent.isEmpty() && songsInCurrent.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("此目录为空", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                } else {
                    items(foldersInCurrent, key = { it.path }) { item ->
                        FileBrowserItemRow(
                            item = item,
                            onClick = { currentPath = currentPath + item.name }
                        )
                    }
                    items(songsInCurrent, key = { it.id }) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                playerViewModel.playSongs(songsInCurrent, songsInCurrent.indexOf(song))
                            },
                            onMoreClick = { /* TODO: 歌曲更多操作 */ }
                        )
                    }
                }
            }
            // 滚动条（跳过底部地址栏区域）
            cn.lemondrop.fhreborn.ui.components.LazyListScrollBar(
                listState = listState,
                modifier = Modifier.align(Alignment.CenterEnd),
                trackPadding = PaddingValues(
                    top = 8.dp,
                    bottom = bottomOverlayHeight + addressBarHeight + 16.dp
                )
            )

            // 地址栏：固定在底部标题栏上方，左侧返回上级，中间路径面包屑
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(addressBarHeight)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (currentPath.isEmpty()) {
                                onBack()
                            } else {
                                currentPath = currentPath.dropLast(1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = "返回上级",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }

                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(breadcrumb) { index, name ->
                            val isLast = index == breadcrumb.lastIndex
                            Text(
                                text = name,
                                style = MiuixTheme.textStyles.body2,
                                color = if (isLast) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable(enabled = !isLast) {
                                    currentPath = if (index <= 1) emptyList() else currentPath.take(index - 1)
                                }
                            )
                            if (!isLast) {
                                Icon(
                                    imageVector = Lucide.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                }
            }
        }

        }
    }
}