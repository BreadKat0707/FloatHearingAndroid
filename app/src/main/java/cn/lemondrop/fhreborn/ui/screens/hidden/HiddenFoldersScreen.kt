package cn.lemondrop.fhreborn.ui.screens.hidden

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.data.db.AppDatabase
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.scanner.ScanSourceMode
import cn.lemondrop.fhreborn.ui.components.FhListItem
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.components.LazyListScrollBar
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import cn.lemondrop.fhreborn.ui.theme.LocalBlurBackdrop
import cn.lemondrop.fhreborn.ui.viewmodel.LibraryViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import cn.lemondrop.fhreborn.util.PathUtils
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 已隐藏的文件夹（二级页面）。
 *
 * 媒体库-文件夹 tab 菜单"查看隐藏的文件夹"与设置-隐藏文件夹 均进入此页。
 * 列出所有被隐藏的文件夹及其中的歌曲，支持展开查看与取消隐藏。
 */
@Composable
fun HiddenFoldersScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    // 层背景：顶栏对其做真实模糊
    val backdrop = LocalBlurBackdrop.current ?: return
    val listState = rememberLazyListState()
    val topBarScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }.value

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            BlurTopBar(
                backdrop = backdrop,
                scrolled = topBarScrolled,
                title = "已隐藏的文件夹",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            AppBackgroundLayer()
            HiddenFoldersContent(
                libraryViewModel = libraryViewModel,
                playerViewModel = playerViewModel,
                contentPadding = padding
            )
        }
    }
}

/**
 * 隐藏文件夹内容（无外壳）。媒体库路由页与设置页共用。
 */
@Composable
fun HiddenFoldersContent(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    contentPadding: PaddingValues,
    onScrolledChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val hiddenFolders by libraryViewModel.hiddenFolders.collectAsState(initial = emptySet())
    val db = remember { AppDatabase.getInstance(context) }
    val settingsRepository = remember(context) { AppSettingsRepository(context) }
    val sourceMode by settingsRepository.scanSourceMode.collectAsState(initial = ScanSourceMode.MEDIA_STORE)
    val storedSongs by remember {
        db.songDao().getAllSongs()
    }.collectAsState(initial = emptyList())
    val allSongs = remember(storedSongs, sourceMode) {
        val source = ScanSourceMode.toSongSource(sourceMode)
        storedSongs.filter { it.source == source }
    }

    // 每个隐藏文件夹 -> 其中的歌曲（直接子文件 + 子层全部）
    val folders = remember(allSongs, hiddenFolders) {
        hiddenFolders.map { folderPath ->
            folderPath to allSongs.filter { song ->
                PathUtils.isPathUnderFolder(song.path, folderPath)
            }
        }.sortedBy { it.first.lowercase() }
    }
    val hiddenCount = hiddenFolders.size
    val totalSongs = folders.sumOf { it.second.size }

    val listState = rememberLazyListState()
    cn.lemondrop.fhreborn.ui.screens.settings.observeSettingsScroll(listState, onScrolledChange)
    val bottomOverlayHeight = LocalGlobalPlayBarHeight.current
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + bottomOverlayHeight
            )
        ) {
                item {
                    Text(
                        text = if (folders.isEmpty()) "没有已隐藏的文件夹" else "$hiddenCount 个文件夹 · $totalSongs 首歌曲",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (folders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("在文件夹上选择\"在音乐库隐藏\"后，可在此处取消隐藏", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                }

                items(folders, key = { it.first }) { (folderPath, folderSongs) ->
                    var expanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MiuixTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folderPath.substringAfterLast('/'),
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = folderPath,
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${folderSongs.size} 首",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Icon(
                                imageVector = if (expanded) Lucide.ChevronDown else Lucide.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                text = "取消隐藏",
                                onClick = { libraryViewModel.unhideFolder(folderPath) }
                            )
                        }
                        if (expanded) {
                            folderSongs.forEach { song ->
                                FhListItem(
                                    title = song.title,
                                    summary = "${song.artist} - ${song.album}",
                                    onClick = {
                                        playerViewModel.playSongs(folderSongs, folderSongs.indexOf(song))
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(bottomOverlayHeight)) }
            }
            LazyListScrollBar(
                listState = listState,
                modifier = Modifier.align(Alignment.CenterEnd),
                trackPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + bottomOverlayHeight
                )
            )
        }
    }
