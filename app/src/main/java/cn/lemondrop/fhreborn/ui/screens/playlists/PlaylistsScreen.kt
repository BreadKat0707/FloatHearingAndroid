package cn.lemondrop.fhreborn.ui.screens.playlists

import android.app.Application
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.LocalDrawerToggle
import cn.lemondrop.fhreborn.LocalDrawerVisible
import cn.lemondrop.fhreborn.LocalGlobalPlayBarHeight
import cn.lemondrop.fhreborn.data.db.dao.PlaylistWithCount
import cn.lemondrop.fhreborn.data.db.entity.Playlist
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.AppBackgroundLayer
import cn.lemondrop.fhreborn.ui.components.AppShell
import cn.lemondrop.fhreborn.ui.components.FhBottomSheet
import cn.lemondrop.fhreborn.ui.components.PlaylistCover
import cn.lemondrop.fhreborn.ui.components.PlaylistEditSheet
import cn.lemondrop.fhreborn.ui.theme.BlurTopBar
import cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PlaylistsScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    playerViewModel: PlayerViewModel,
    onOpenPlaylist: (Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModel.Factory(context.applicationContext as Application)
    )
    val playlists by viewModel.getAllPlaylists().collectAsState(initial = emptyList())
    val drawerVisible = LocalDrawerVisible.current
    val drawerToggle = LocalDrawerToggle.current

    var showCreateSheet by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var deletingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    // 导入：OpenDocument 读取 JSON/M3U 文件
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            snackbarScope.launch {
                val fileName = withContext(Dispatchers.IO) {
                    context.contentResolver.query(
                        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: uri.lastPathSegment
                }
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }
                }
                if (content.isNullOrBlank()) {
                    snackbarHostState.showSnackbar("无法读取文件")
                    return@launch
                }
                val isJson = fileName?.endsWith(".json", ignoreCase = true) == true
                viewModel.importPlaylist(
                    suggestedName = fileName ?: "导入的歌单",
                    content = content,
                    isJson = isJson,
                    onResult = { success ->
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) "导入成功" else "未匹配到任何歌曲"
                            )
                        }
                    }
                )
            }
        }
    }

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
                containerColor = Color.Transparent,
                topBar = {
                    BlurTopBar(
                        title = "歌单",
                        navigationIcon = {
                            IconButton(onClick = { drawerToggle() }) {
                                Icon(
                                    imageVector = Lucide.Menu,
                                    contentDescription = "菜单"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showCreateSheet = true }) {
                                Icon(
                                    imageVector = Lucide.Plus,
                                    contentDescription = "新建歌单"
                                )
                            }
                            IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "audio/x-mpegurl", "*/*")) }) {
                                Icon(
                                    imageVector = Lucide.Upload,
                                    contentDescription = "导入歌单"
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                val playBarHeight = LocalGlobalPlayBarHeight.current
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = playBarHeight + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (playlists.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "还没有歌单\n点击右上角 + 创建",
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    style = MiuixTheme.textStyles.body2
                                )
                            }
                        }
                    }
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            viewModel = viewModel,
                            onClick = { onOpenPlaylist(playlist.id) },
                            onPlayClick = { viewModel.playPlaylist(playlist.id, playerViewModel) },
                            onLongClick = { editingPlaylist = playlist },
                            onMenuClick = { editingPlaylist = playlist }
                        )
                    }
                }
            }
            SnackbarHost(
                state = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // 新建歌单
    if (showCreateSheet) {
        PlaylistEditSheet(
            title = "新建歌单",
            initialName = "",
            initialDescription = "",
            onDismiss = { showCreateSheet = false },
            onSave = { name, desc, _, _, _ ->
                viewModel.createPlaylist(name, desc)
                showCreateSheet = false
            }
        )
    }

    // 编辑歌单
    editingPlaylist?.let { playlist ->
        var editCoverSongs by remember(playlist.id) { mutableStateOf<List<Song>>(emptyList()) }
        LaunchedEffect(playlist.id) {
            viewModel.getSongsSnapshot(playlist.id) { songs -> editCoverSongs = songs }
        }
        PlaylistEditSheet(
            title = "编辑歌单",
            initialName = playlist.name,
            initialDescription = playlist.description.orEmpty(),
            initialPlayMode = playlist.defaultPlayMode,
            initialCoverPath = playlist.coverPath,
            initialCoverSource = playlist.coverSource,
            coverSongs = editCoverSongs,
            onDismiss = { editingPlaylist = null },
            onSave = { name, desc, playMode, coverPath, coverSource ->
                viewModel.updatePlaylist(playlist.id, name, desc, playMode)
                viewModel.setCover(playlist.id, coverPath, coverSource)
                editingPlaylist = null
            },
            onDelete = {
                editingPlaylist = null
                deletingPlaylist = playlist
            }
        )
    }

    // 删除确认
    deletingPlaylist?.let { playlist ->
        FhBottomSheet(
            show = true,
            onDismissRequest = { deletingPlaylist = null },
            title = "删除歌单",
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    text = "确定删除「${playlist.name}」吗？歌单中的歌曲不会从媒体库删除。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(text = "取消", onClick = { deletingPlaylist = null })
                    TextButton(
                        text = "删除",
                        onClick = {
                            viewModel.deletePlaylist(playlist.id)
                            deletingPlaylist = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistWithCount,
    viewModel: PlaylistViewModel,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    var coverSongIds by remember(playlist.id) { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(playlist.id, playlist.songCount) {
        viewModel.getFirstSongIds(playlist.id) { ids -> coverSongIds = ids }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistCover(
            songIds = coverSongIds,
            coverPath = playlist.coverPath,
            coverSource = playlist.coverSource,
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onClick)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.songCount} 首歌曲",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        IconButton(onClick = onPlayClick) {
            Icon(
                imageVector = Lucide.Play,
                contentDescription = "播放歌单",
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}
