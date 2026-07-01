@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3WindowSizeClassApi::class)

package cn.lemondrop.fhreborn.ui.screens.player

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import cn.lemondrop.fhreborn.ui.theme.LocalAppDarkTheme
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import android.app.Activity
import android.view.WindowManager
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.Player
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import cn.lemondrop.fhreborn.ui.theme.FluentLargeCorner
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Repeat1
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Volume2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit = {}
) {
    val viewModel = playerViewModel
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val lyricAlignCenter by settingsRepository.isLyricAlignCenter.collectAsState(initial = false)

    val appSettingsRepository = remember { AppSettingsRepository(context) }
    val keepScreenOn by appSettingsRepository.wakeLock.collectAsState(initial = true)
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    // 横屏 / 大屏（Expanded 宽度，≥840dp）时启用左右双栏布局；竖屏保持单栏
    val configuration = LocalConfiguration.current
    val activity = context as? Activity
    val widthSizeClass = activity?.let { calculateWindowSizeClass(it).widthSizeClass }
    val isExpandedWidth = widthSizeClass == WindowWidthSizeClass.Expanded
    val isCompactLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && !isExpandedWidth
    val isTwoPane = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || isExpandedWidth

    // 刘海/挖孔屏安全区（左右上下都避开）
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()

    var showLyrics by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showCoverViewer by remember { mutableStateOf(false) }
    var currentCoverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val queueProgress = remember { Animatable(0f) }
    val isQueueOpen by remember { derivedStateOf { queueProgress.value > 0.5f } }
    val lyricsBackProgress = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 300.dp.toPx() }
    val snapThreshold = with(density) { 150.dp.toPx() }
    val screenHeightPx = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    fun openQueue() {
        scope.launch { queueProgress.animateTo(1f, tween(250, easing = FastOutSlowInEasing)) }
    }
    fun closeQueue() {
        scope.launch { queueProgress.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) }
    }

    val offsetY = remember { Animatable(screenHeightPx) }

    // 进入动画：从屏幕底部滑入
    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, animationSpec = tween(350, easing = FastOutSlowInEasing))
    }

    // 播放器页返回：预测返回手势驱动页面下滑收起
    PredictiveBackHandler(
        enabled = !showLyrics && !isQueueOpen && !showMore && !showCoverViewer
    ) { progress ->
        try {
            progress.collect { event ->
                val p = event.progress.coerceIn(0f, 1f)
                offsetY.snapTo(p * screenHeightPx)
            }
            scope.launch {
                offsetY.animateTo(screenHeightPx, tween(250))
                onBack()
            }
        } catch (_: CancellationException) {
            scope.launch { offsetY.animateTo(0f, tween(200)) }
        }
    }

    val dragOffsetY = offsetY.value
    val progress = (dragOffsetY / dismissThreshold).coerceIn(0f, 1f)
    val scale = 1f - progress * 0.08f
    val cornerRadius = (progress * 32).dp

    val statusBarPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues()
    val initialNavBarPadding = remember(density) {
        with(density) {
            val decorView = (context as? android.app.Activity)?.window?.decorView
            val rootInsets = decorView?.let { ViewCompat.getRootWindowInsets(it) }
            val bottom = rootInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            bottom.toDp()
        }
    }
    val navBarPadding = maxOf(
        initialNavBarPadding,
        WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    )

    // 流体背景上的前景色（深色用白，浅色用黑）
    val isDarkTheme = LocalAppDarkTheme.current
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val fluidOnColorSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
    val fluidOnColorTertiary = if (isDarkTheme) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f)
    val fluidOnColorHint = if (isDarkTheme) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.4f)
    val fluidOnColorVeryHint = if (isDarkTheme) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)
    val targetBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply

    // 当队列打开时向下滑，应优先收起队列；同一手势内不要把队列收起误判为播放器收起
    var queueHandled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffsetY.toInt()) }
            .clip(RoundedCornerShape(cornerRadius))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        queueHandled = queueProgress.value > 0f
                    },
                    onDragEnd = {
                        when {
                            queueHandled -> {
                                scope.launch {
                                    if (queueProgress.value > 0.5f) {
                                        queueProgress.animateTo(1f, tween(250))
                                    } else {
                                        queueProgress.animateTo(0f, tween(250))
                                    }
                                }
                            }
                            offsetY.value > snapThreshold -> {
                                scope.launch {
                                    offsetY.animateTo(screenHeightPx, animationSpec = tween(250))
                                    onBack()
                                }
                            }
                            offsetY.value > 0f -> {
                                scope.launch {
                                    offsetY.animateTo(0f, animationSpec = tween(250))
                                }
                            }
                        }
                        queueHandled = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        when {
                            queueHandled || queueProgress.value > 0f -> {
                                queueHandled = true
                                // 队列已打开或正在打开：跟手拖动控制队列进度
                                val delta = dragAmount / screenHeightPx
                                scope.launch {
                                    queueProgress.snapTo(
                                        (queueProgress.value - delta).coerceIn(0f, 1f)
                                    )
                                }
                            }
                            dragAmount > 0f || offsetY.value > 0f -> {
                                // 向下滑动：收起播放器
                                val newValue = (offsetY.value + dragAmount).coerceAtLeast(0f)
                                scope.launch { offsetY.snapTo(newValue) }
                            }
                            dragAmount < 0f && offsetY.value == 0f -> {
                                // 向上滑动：跟手打开队列
                                val delta = -dragAmount / screenHeightPx
                                scope.launch {
                                    queueProgress.snapTo(
                                        (queueProgress.value + delta).coerceIn(0f, 1f)
                                    )
                                }
                            }
                        }
                    }
                )
            }
    ) {
        SharedTransitionLayout {
            val sharedTransitionScope = this@SharedTransitionLayout

            // 播放器背景（根据设置选择：旋转流体 / AGSL 流体 / 封面模糊 / 默认颜色）
        PlayerBackground(
            songId = currentSong?.id,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        // 歌词返回手势：驱动 showLyrics 关闭，退出形变交给 AnimatedContent
        PredictiveBackHandler(enabled = showLyrics) { progress ->
            try {
                progress.collect { event ->
                    lyricsBackProgress.snapTo(event.progress.coerceIn(0f, 1f))
                }
                showLyrics = false
                lyricsBackProgress.snapTo(0f)
            } catch (_: CancellationException) {
                scope.launch { lyricsBackProgress.animateTo(0f, tween(200)) }
            }
        }

        if (isTwoPane) {
            // 横屏 / 大屏：左右双栏。左栏播放控件，右栏歌词；竖屏走下方单栏分支。
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(cutoutPadding)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = -queueProgress.value * screenHeightPx
                    }
            ) {
                val coverSizeMultiplier by animateFloatAsState(
                    targetValue = if (isCompactLandscape) 1f else if (isPlaying) 1f else 0.92f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "coverSizeMultiplierTwoPane"
                )
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                        .padding(
                            top = statusBarPadding.calculateTopPadding(),
                            bottom = navBarPadding
                        )
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 手机横屏：封面与歌曲信息同一行；大屏/pad：垂直排列
                    if (isCompactLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerCoverSection(
                                currentSong = currentSong,
                                currentCoverBitmap = currentCoverBitmap,
                                coverSizeMultiplier = coverSizeMultiplier,
                                isDarkTheme = isDarkTheme,
                                targetBlendMode = targetBlendMode,
                                onLongClickCover = { showCoverViewer = true },
                                onCoverBitmapLoaded = { currentCoverBitmap = it },
                                modifier = Modifier
                                    .width(120.dp)
                                    .aspectRatio(1f),
                                useSharedTransition = false
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            PlayerSongInfoSection(
                                currentSong = currentSong,
                                isDarkTheme = isDarkTheme,
                                useSharedTransition = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        PlayerCoverSection(
                            currentSong = currentSong,
                            currentCoverBitmap = currentCoverBitmap,
                            coverSizeMultiplier = coverSizeMultiplier,
                            isDarkTheme = isDarkTheme,
                            targetBlendMode = targetBlendMode,
                            onLongClickCover = { showCoverViewer = true },
                            onCoverBitmapLoaded = { currentCoverBitmap = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            useSharedTransition = false
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        PlayerSongInfoSection(
                            currentSong = currentSong,
                            isDarkTheme = isDarkTheme,
                            useSharedTransition = false
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PlayerProgressSection(
                        position = position,
                        duration = duration,
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        useSharedTransition = false
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PlayerControlsSection(
                        isPlaying = isPlaying,
                        isShuffle = shuffleMode,
                        repeatMode = repeatMode,
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PlayerBottomActionsSection(
                        openQueue = { openQueue() },
                        onShowMore = { showMore = true },
                        isDarkTheme = isDarkTheme
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                        .padding(
                            top = statusBarPadding.calculateTopPadding(),
                            bottom = navBarPadding,
                            end = 24.dp
                        )
                ) {
                    PlayerLyricsPane(
                        lyrics = lyrics,
                        currentPosition = position,
                        currentLyricIndex = currentLyricIndex,
                        isDarkTheme = isDarkTheme,
                        onLineClicked = { line -> viewModel.seekTo(line.start.toLong()) }
                    )
                }
            }
        } else {
        AnimatedContent(
            targetState = showLyrics,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "player_to_lyrics",
            modifier = Modifier.fillMaxSize()
        ) { isLyrics ->
            if (isLyrics) {
                LyricSheet(
                    lyrics = lyrics,
                    currentPosition = position,
                    isDarkTheme = isDarkTheme,
                    song = currentSong,
                    coverBitmap = currentCoverBitmap,
                    duration = duration,
                    onDismiss = { showLyrics = false },
                    onSeek = { time ->
                        viewModel.seekTo(time)
                    },
                    onMoreClick = {
                        // 在歌词页上直接弹出更多菜单，不退出歌词页
                        showMore = true
                    },
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = sharedTransitionScope
                )
            } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(cutoutPadding),
            contentAlignment = Alignment.TopStart
        ) {
            // 播放器主内容：随队列呼出向上滑走；打开歌词时非共享元素淡出
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = -queueProgress.value * screenHeightPx
                    }
                    .padding(
                        top = statusBarPadding.calculateTopPadding(),
                        bottom = navBarPadding
                    )
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部拖动条（纯视觉，不可点击）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .fluidBlend(targetBlendMode)
                            .background(fluidOnColor.copy(alpha = 0.3f))
                    )
                }

                // 封面区域：按原图比例适配容器，非正方形也能显示完整封面；切歌时封面内容淡入淡出
                val coverSizeMultiplier by animateFloatAsState(
                    targetValue = if (isPlaying) 1f else 0.92f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "coverSizeMultiplier"
                )
                PlayerCoverSection(
                    currentSong = currentSong,
                    currentCoverBitmap = currentCoverBitmap,
                    coverSizeMultiplier = coverSizeMultiplier,
                    isDarkTheme = isDarkTheme,
                    targetBlendMode = targetBlendMode,
                    onLongClickCover = { showCoverViewer = true },
                    onCoverBitmapLoaded = { currentCoverBitmap = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    useSharedTransition = true,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedContent
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 歌曲信息（与歌词页底部面板共享元素过渡）
                PlayerSongInfoSection(
                    currentSong = currentSong,
                    isDarkTheme = isDarkTheme,
                    useSharedTransition = true,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedContent
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 歌词预览（两行）始终留占位
                PlayerLyricsPreviewSection(
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    lyricAlignCenter = lyricAlignCenter,
                    fluidOnColorSecondary = fluidOnColorSecondary,
                    fluidOnColorHint = fluidOnColorHint,
                    fluidOnColorVeryHint = fluidOnColorVeryHint,
                    targetBlendMode = targetBlendMode,
                    onClick = { showLyrics = true }
                )
                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(8.dp))

                // 进度条（与歌词页底部面板共享元素过渡）
                PlayerProgressSection(
                    position = position,
                    duration = duration,
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    useSharedTransition = true,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedContent
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 播放控制
                PlayerControlsSection(
                    isPlaying = isPlaying,
                    isShuffle = shuffleMode,
                    repeatMode = repeatMode,
                    viewModel = viewModel
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 上划打开播放队列 + 底部操作栏
                PlayerBottomActionsSection(
                    openQueue = { openQueue() },
                    onShowMore = { showMore = true },
                    isDarkTheme = isDarkTheme
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            // 播放队列已移至单栏/双栏分支之后，见下方共用覆盖层
        }
            }
        }
        }

        // 播放队列：从底部滑入（单栏/双栏共用，全屏浮于上方）
        PredictiveBackHandler(enabled = isQueueOpen) { progress ->
            try {
                progress.collect { event ->
                    queueProgress.snapTo((1f - event.progress).coerceIn(0f, 1f))
                }
                closeQueue()
            } catch (_: CancellationException) {
                scope.launch { queueProgress.animateTo(1f, tween(200)) }
            }
        }
        if (queueProgress.value > 0f) {
            PlayerQueueScreen(
                queue = queue,
                currentIndex = currentIndex,
                onBack = { closeQueue() },
                onItemClick = { index ->
                    viewModel.seekTo(index)
                    closeQueue()
                },
                onRemove = { index ->
                    viewModel.removeFromQueue(index)
                },
                onCloseDrag = { dragAmount ->
                    scope.launch {
                        queueProgress.snapTo(
                            (queueProgress.value - dragAmount / screenHeightPx).coerceIn(0f, 1f)
                        )
                    }
                },
                onCloseDragEnd = {
                    scope.launch {
                        if (queueProgress.value > 0.5f) {
                            queueProgress.animateTo(1f, tween(200))
                        } else {
                            queueProgress.animateTo(0f, tween(200))
                        }
                    }
                },
                modifier = Modifier.graphicsLayer {
                    translationY = (1f - queueProgress.value) * screenHeightPx
                }
            )
        }

        // 更多菜单弹窗（放在歌词弹窗之后，确保显示在歌词层之上）
        if (showMore) {
            BackHandler { showMore = false }
            PlayerMoreSheet(
                onDismiss = { showMore = false },
                onTimerClick = { viewModel.showScheduledPause() },
                onShareClick = { shareCurrentSong(context, currentSong) }
            )
        }

        // 封面大图查看器
        if (showCoverViewer && currentCoverBitmap != null) {
            BackHandler { showCoverViewer = false }
            CoverViewer(
                bitmap = currentCoverBitmap!!,
                title = currentSong?.title ?: "cover",
                onDismiss = { showCoverViewer = false }
            )
        }
        }
    }
}

@Composable
private fun PlayerProgressSlider(
    position: Long,
    duration: Long,
    onProgressChange: (Float) -> Unit,
    onDragStateChange: (isDragging: Boolean, dragProgress: Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val safeDuration = duration.coerceAtLeast(1L)
    val progress = (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    var dragProgress by remember { mutableFloatStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }
    val displayProgress = if (isDragging) dragProgress else progress
    val isDarkTheme = LocalAppDarkTheme.current
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val fluidOnColorSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)

    LaunchedEffect(isDragging, dragProgress) {
        onDragStateChange(isDragging, dragProgress)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 进度条本体（拖动时目标时间由父级悬浮显示，不内嵌在此）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragProgress = progress
                        },
                        onDragEnd = {
                            isDragging = false
                            onProgressChange(dragProgress)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragProgress = newProgress
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(fluidOnColor.copy(alpha = 0.2f))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .drawBehind {
                        // 前景用黑/白（跟随浅/深色）+ 混合模式，与底层流体背景融合，不用 primary 实色
                        drawRect(
                            color = fluidOnColor,
                            alpha = 0.6f,
                            blendMode = if (isDarkTheme) BlendMode.Overlay else BlendMode.Multiply
                        )
                        
                    }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(position),
                style = MaterialTheme.typography.labelSmall,
                color = fluidOnColorSecondary
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = fluidOnColorSecondary
            )
        }
    }
}

@Composable
private fun LyricSheet(
    lyrics: SyncedLyrics?,
    currentPosition: Long,
    isDarkTheme: Boolean,
    song: Song?,
    coverBitmap: ImageBitmap?,
    duration: Long,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
    onMoreClick: () -> Unit,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val listState = rememberLazyListState()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val lyricBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply
    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val statusBarPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()

    val bgColor = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // 顶部和底部渐变压暗，保证歌词可读，同时背景仍可见
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to bgColor.copy(alpha = 0.55f),
                            0.25f to Color.Transparent,
                            0.75f to Color.Transparent,
                            1.0f to bgColor.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        if (lyrics != null) {
            // 大歌词：通过包装层隔离进度状态，避免在主线程重组过程中直接写状态导致循环
            KaraokeLyricsViewWrapper(
                lyrics = lyrics,
                currentPosition = currentPosition,
                listState = listState,
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = statusBarPadding + 8.dp,
                        start = 6.dp,
                        end = 6.dp,
                        bottom = navBarPadding + 60.dp
                    ),
                onLineClicked = { line ->
                    onSeek(line.start.toLong())
                }
            )
        } else {
            // 无歌词提示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = statusBarPadding + 8.dp,
                        bottom = navBarPadding + 140.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无歌词",
                    style = MaterialTheme.typography.headlineSmall,
                    color = fluidOnColor.copy(alpha = 0.5f)
                )
            }
        }

        // 底部固定面板：小封面 + 歌曲信息 + 三点菜单 + 进度条（无背景，左右与歌词对齐 28.dp）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = navBarPadding + 32.dp)
                .padding(horizontal = 28.dp)
                .padding(vertical = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 小封面（与主封面共享元素过渡）
                val smallCoverModifier = with(sharedTransitionScope) {
                    Modifier
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "player_cover"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                }
                Box(
                    modifier = smallCoverModifier
                ) {
                    val bmp = coverBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SongCoverImage(
                            songId = song?.id ?: 0,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                val songInfoTargetModifier = with(sharedTransitionScope) {
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = "player_song_info"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
                Column(
                    modifier = songInfoTargetModifier.weight(1f)
                ) {
                    Text(
                        text = song?.title ?: "未在播放",
                        style = MaterialTheme.typography.titleMedium,
                        color = onSurface.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song?.let { "${it.artist} - ${it.album}" } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FluentIconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Lucide.EllipsisVertical,
                        contentDescription = "更多",
                        modifier = Modifier.size(24.dp),
                        tint = onSurface.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val progressTargetModifier = with(sharedTransitionScope) {
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "player_progress"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
            Column(
                modifier = progressTargetModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlayerProgressSlider(
                    position = currentPosition,
                    duration = duration,
                    onProgressChange = { fraction ->
                        onSeek((fraction * duration.coerceAtLeast(1L)).toLong())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun KaraokeLyricsViewWrapper(
    lyrics: SyncedLyrics,
    currentPosition: Long,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onLineClicked: (ISyncedLine) -> Unit = {}
) {
    val currentPosState = rememberUpdatedState(currentPosition)
    val lyricPosition = remember { mutableIntStateOf(currentPosition.toInt()) }
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val lyricBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply

    // 切歌/换歌词后先让 KaraokeLyricsView 完成初始布局，再开始跟随进度
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(lyrics) {
        ready = false
        delay(300)
        ready = true
    }

    LaunchedEffect(ready) {
        while (isActive && ready) {
            val newPos = currentPosState.value.toInt()
            // 在后台线程更新状态，避免在主线程重组过程中写状态导致循环无效化/ANR
            withContext(Dispatchers.Default) {
                lyricPosition.intValue = newPos
            }
            delay(60)
        }
    }

    KaraokeLyricsView(
        listState = listState,
        lyrics = lyrics,
        currentPosition = { lyricPosition.intValue },
        onLineClicked = onLineClicked,
        onLinePressed = {},
        modifier = modifier,
        textColor = fluidOnColor,
        blendMode = lyricBlendMode
    )
}

@Composable
private fun PlayerIcon(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDarkTheme = LocalAppDarkTheme.current
    val iconColor = if (isDarkTheme) {
        Color.White.copy(alpha = if (enabled) 0.6f else 0.25f)
    } else {
        Color.Black.copy(alpha = if (enabled) 0.5f else 0.22f)
    }
    val targetBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            blendMode = targetBlendMode
        },
        tint = iconColor
    )
}

private fun Modifier.fluidBlend(blendMode: BlendMode): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
    this.blendMode = blendMode
}

private fun ISyncedLine.lyricContent(): String = when (this) {
    is SyncedLine -> content
    is KaraokeLine -> syllables.joinToString("") { it.content }
    else -> ""
}

private fun ISyncedLine.lyricTranslation(): String? = when (this) {
    is SyncedLine -> translation
    is KaraokeLine -> translation
    else -> null
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

private fun shareCurrentSong(context: android.content.Context, song: cn.lemondrop.fhreborn.data.db.entity.Song?) {
    song ?: return
    val file = java.io.File(song.path)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        android.content.Intent.createChooser(intent, "分享音频")
    )
}

@Composable
private fun PlaceholderCoverContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "♪",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun PlayerCoverSection(
    currentSong: Song?,
    currentCoverBitmap: ImageBitmap?,
    coverSizeMultiplier: Float,
    isDarkTheme: Boolean,
    targetBlendMode: BlendMode,
    onLongClickCover: () -> Unit,
    onCoverBitmapLoaded: (ImageBitmap?) -> Unit,
    modifier: Modifier = Modifier,
    useSharedTransition: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val (baseCoverWidth, baseCoverHeight) = with(density) {
            val bmp = currentCoverBitmap
            if (bmp != null) {
                val maxWPx = maxWidth.toPx()
                val maxHPx = maxHeight.toPx()
                val aspect = bmp.width.toFloat() / bmp.height.toFloat()
                val fitW: Float
                val fitH: Float
                if (maxWPx / maxHPx > aspect) {
                    fitH = maxHPx
                    fitW = fitH * aspect
                } else {
                    fitW = maxWPx
                    fitH = fitW / aspect
                }
                fitW.toDp() to fitH.toDp()
            } else {
                val size = maxWidth.coerceAtMost(maxHeight)
                size to size
            }
        }

        val coverWidth by animateDpAsState(
            targetValue = baseCoverWidth * coverSizeMultiplier,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "coverWidth"
        )
        val coverHeight by animateDpAsState(
            targetValue = baseCoverHeight * coverSizeMultiplier,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "coverHeight"
        )

        val baseCoverModifier = Modifier
            .size(coverWidth, coverHeight)
            .shadow(20.dp, RoundedCornerShape(FluentLargeCorner))
            .clip(RoundedCornerShape(FluentLargeCorner))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = {
                    if (currentCoverBitmap != null) onLongClickCover()
                }
            )
        val coverModifier = if (useSharedTransition && sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "player_cover"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .then(baseCoverModifier)
            }
        } else {
            baseCoverModifier
        }
        Box(
            modifier = coverModifier,
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = currentSong?.id,
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "cover_crossfade"
            ) { songId ->
                var bitmap by remember(songId) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(songId) {
                    bitmap = null
                    songId?.let { id ->
                        withContext(Dispatchers.IO) {
                            bitmap = try {
                                val uri = Uri.parse("content://media/external/audio/media/$id/albumart")
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }

                // 把当前显示封面同步给查看器，避免长按后再加载
                LaunchedEffect(bitmap) {
                    if (songId == currentSong?.id) {
                        onCoverBitmapLoaded(bitmap)
                    }
                }

                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PlaceholderCoverContent()
                }
            }
        }
    }
}

@Composable
private fun PlayerSongInfoSection(
    currentSong: Song?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    useSharedTransition: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val fluidOnColorSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
    val fluidOnColorTertiary = if (isDarkTheme) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f)
    val sharedModifier = if (useSharedTransition && sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "player_song_info"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }
    Column(modifier = sharedModifier.then(modifier).fillMaxWidth()) {
        Text(
            text = currentSong?.title ?: "未在播放",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = fluidOnColorSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentSong?.let { "${it.artist} - ${it.album}" } ?: "选择一首歌曲开始",
            style = MaterialTheme.typography.bodyLarge,
            color = fluidOnColorTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlayerProgressSection(
    position: Long,
    duration: Long,
    viewModel: PlayerViewModel,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    useSharedTransition: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val fluidOnColorSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
    val sharedModifier = if (useSharedTransition && sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "player_progress"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }
    var isSeekDragging by remember { mutableStateOf(false) }
    var seekDragProgress by remember { mutableFloatStateOf(0f) }
    var seekSliderWidth by remember { mutableIntStateOf(0) }
    val seekBubbleText = remember(isSeekDragging, seekDragProgress) {
        if (isSeekDragging) formatDuration((seekDragProgress * duration.coerceAtLeast(1L)).toLong()) else ""
    }

    Box(modifier = sharedModifier.then(modifier).fillMaxWidth()) {
        PlayerProgressSlider(
            position = position,
            duration = duration,
            onProgressChange = { fraction ->
                viewModel.seekTo((fraction * duration.coerceAtLeast(1L)).toLong())
            },
            onDragStateChange = { dragging, progress ->
                isSeekDragging = dragging
                seekDragProgress = progress
            },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    seekSliderWidth = coordinates.size.width
                }
        )

        // 拖动进度时悬浮显示目标时间
        androidx.compose.animation.AnimatedVisibility(
            visible = isSeekDragging,
            enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.9f),
            exit = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.9f),
            modifier = Modifier.offset(
                y = (-28).dp,
                x = with(LocalDensity.current) {
                    ((seekDragProgress * seekSliderWidth) - (seekSliderWidth * 0.1f).coerceAtLeast(30f))
                        .toDp()
                }
            )
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isDarkTheme) Color.Black.copy(alpha = 0.7f)
                        else Color.White.copy(alpha = 0.9f)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = seekBubbleText,
                    style = MaterialTheme.typography.labelSmall,
                    color = fluidOnColorSecondary
                )
            }
        }
    }
}

@Composable
private fun PlayerControlsSection(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FluentIconButton(onClick = { viewModel.toggleShuffle() }) {
            PlayerIcon(
                imageVector = Lucide.Shuffle,
                contentDescription = "随机播放",
                modifier = Modifier.size(24.dp),
                enabled = isShuffle
            )
        }

        FluentIconButton(onClick = { viewModel.previous() }) {
            PlayerIcon(
                imageVector = Lucide.SkipBack,
                contentDescription = "上一首",
                modifier = Modifier.size(32.dp)
            )
        }

        FluentIconButton(onClick = { viewModel.playPause() }) {
            PlayerIcon(
                imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(48.dp)
            )
        }

        FluentIconButton(onClick = { viewModel.next() }) {
            PlayerIcon(
                imageVector = Lucide.SkipForward,
                contentDescription = "下一首",
                modifier = Modifier.size(32.dp)
            )
        }

        FluentIconButton(onClick = { viewModel.toggleRepeatMode() }) {
            PlayerIcon(
                imageVector = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Lucide.Repeat1
                    else -> Lucide.Repeat
                },
                contentDescription = "循环模式",
                modifier = Modifier.size(24.dp),
                enabled = repeatMode != Player.REPEAT_MODE_OFF
            )
        }
    }
}

@Composable
private fun PlayerBottomActionsSection(
    openQueue: () -> Unit,
    onShowMore: () -> Unit,
    isDarkTheme: Boolean
) {
    // 上划打开播放队列（整排底栏图标上方居中）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = { openQueue() }
            )
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(
            imageVector = Lucide.ChevronUp,
            contentDescription = "上划打开播放队列",
            modifier = Modifier.size(20.dp)
        )
    }

    // 底部操作栏
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FluentIconButton(onClick = { /* TODO: 音频输出 */ }) {
            PlayerIcon(
                imageVector = Lucide.Volume2,
                contentDescription = "音频输出",
                modifier = Modifier.size(24.dp)
            )
        }

        FluentIconButton(onClick = { /* TODO: 添加到歌单 */ }) {
            PlayerIcon(
                imageVector = Lucide.FolderPlus,
                contentDescription = "添加到歌单",
                modifier = Modifier.size(24.dp)
            )
        }

        FluentIconButton(onClick = { openQueue() }) {
            PlayerIcon(
                imageVector = Lucide.ListMusic,
                contentDescription = "播放队列",
                modifier = Modifier.size(24.dp)
            )
        }

        FluentIconButton(onClick = { /* TODO: 倍速 */ }) {
            PlayerIcon(
                imageVector = Lucide.Gauge,
                contentDescription = "倍速",
                modifier = Modifier.size(24.dp)
            )
        }

        FluentIconButton(onClick = { onShowMore() }) {
            PlayerIcon(
                imageVector = Lucide.EllipsisVertical,
                contentDescription = "更多",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PlayerLyricsPane(
    lyrics: SyncedLyrics?,
    currentPosition: Long,
    currentLyricIndex: Int,
    isDarkTheme: Boolean,
    onLineClicked: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    if (lyrics != null) {
        KaraokeLyricsViewWrapper(
            lyrics = lyrics,
            currentPosition = currentPosition,
            listState = listState,
            isDarkTheme = isDarkTheme,
            modifier = modifier.fillMaxSize(),
            onLineClicked = onLineClicked
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无歌词",
                style = MaterialTheme.typography.headlineSmall,
                color = fluidOnColor.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun PlayerLyricsPreviewSection(
    lyrics: SyncedLyrics?,
    currentLyricIndex: Int,
    lyricAlignCenter: Boolean,
    fluidOnColorSecondary: Color,
    fluidOnColorHint: Color,
    fluidOnColorVeryHint: Color,
    targetBlendMode: BlendMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lyricLines = lyrics?.lines ?: emptyList()
    val currentLine = lyricLines.getOrNull(currentLyricIndex.coerceAtLeast(0))
    val nextLine = lyricLines.getOrNull((currentLyricIndex + 1).coerceAtMost(lyricLines.lastIndex))
    val lyricTextAlign = if (lyricAlignCenter) TextAlign.Center else TextAlign.Start
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick() }
            ),
        horizontalAlignment = if (lyricAlignCenter) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (lyricLines.isNotEmpty() && currentLine != null) {
            Text(
                text = currentLine.lyricContent(),
                modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                style = MaterialTheme.typography.bodyLarge,
                color = fluidOnColorSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = lyricTextAlign
            )
            Spacer(modifier = Modifier.height(2.dp))
            val translation = currentLine.lyricTranslation()
            if (!translation.isNullOrEmpty()) {
                Text(
                    text = translation,
                    modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = fluidOnColorSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = lyricTextAlign
                )
            } else if (nextLine != null) {
                Text(
                    text = nextLine.lyricContent(),
                    modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = fluidOnColorSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = lyricTextAlign
                )
            }
        } else {
            Text(
                text = "暂无歌词",
                modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                style = MaterialTheme.typography.bodyLarge,
                color = fluidOnColorHint,
                maxLines = 1,
                textAlign = lyricTextAlign
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "点击导入歌词",
                modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                style = MaterialTheme.typography.bodyMedium,
                color = fluidOnColorVeryHint,
                maxLines = 1,
                textAlign = lyricTextAlign
            )
        }
    }
}
