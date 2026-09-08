@file:OptIn(ExperimentalSharedTransitionApi::class)

package cn.lemondrop.fhreborn.ui.screens.player

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.app.Application
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lemondrop.fhreborn.ui.theme.LocalAppDarkTheme
import cn.lemondrop.fhreborn.LocalPredictiveBackEnabled
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
// Window size class removed — using manual screenWidthDp check instead
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
import androidx.compose.runtime.withFrameNanos
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
import cn.lemondrop.fhreborn.ui.components.AddToPlaylistSheet
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.Player
import cn.lemondrop.fhreborn.data.db.entity.Song
import cn.lemondrop.fhreborn.ui.components.SongCoverImage
import cn.lemondrop.fhreborn.ui.components.SongInfoActionSheet
import top.yukonga.miuix.kmp.basic.IconButton
import cn.lemondrop.fhreborn.util.ArtistSplitter
import cn.lemondrop.fhreborn.ui.viewmodel.PlaylistViewModel
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Plus
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
    onBack: () -> Unit = {},
    onNavigateToAlbum: (String, String?) -> Unit = { _, _ -> },
    onNavigateToArtist: (String) -> Unit = {},
    onOpenSettingsCategory: (String) -> Unit = {}
) {
    val viewModel = playerViewModel
    val context = LocalContext.current
    val playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModel.Factory(context.applicationContext as Application)
    )
    val settingsRepository = remember { SettingsRepository(context) }
    val lyricAlignCenter by settingsRepository.isLyricAlignCenter.collectAsState(initial = false)
    val artistSeparators by settingsRepository.artistSeparators.collectAsState(initial = ArtistSplitter.DEFAULT_SEPARATORS)

    val appSettingsRepository = remember { AppSettingsRepository(context) }
    val keepScreenOn by appSettingsRepository.wakeLock.collectAsState(initial = true)

    // 播放器封面设置：圆角 + 圆形旋转封面
    val coverCornerRadius by appSettingsRepository.playerCoverCornerRadius.collectAsState(initial = 12)
    val coverRotating by appSettingsRepository.playerCoverRotating.collectAsState(initial = false)
    val coverShadowY by appSettingsRepository.playerCoverShadowY.collectAsState(initial = 16)
    val coverShadowAlpha by appSettingsRepository.playerCoverShadowAlpha.collectAsState(initial = 40)
    val coverShadowBlur by appSettingsRepository.playerCoverShadowBlur.collectAsState(initial = 20)
    val coverPauseScale by appSettingsRepository.playerCoverPauseScale.collectAsState(initial = 92)

    // Accompanist Lyric 设置
    val acclMainTextSize by appSettingsRepository.acclLyricMainTextSizeSp.collectAsState(initial = 34)
    val acclAccompanimentTextSize by appSettingsRepository.acclLyricAccompanimentTextSizeSp.collectAsState(initial = 20)
    val acclPhoneticTextSize by appSettingsRepository.acclLyricPhoneticTextSizeSp.collectAsState(initial = 13)
    val acclMainFontWeight by appSettingsRepository.acclLyricMainFontWeight.collectAsState(initial = 700)
    val acclAccompanimentFontWeight by appSettingsRepository.acclLyricAccompanimentFontWeight.collectAsState(initial = 700)
    val acclPhoneticFontWeight by appSettingsRepository.acclLyricPhoneticFontWeight.collectAsState(initial = 400)
    val acclShowTranslation by appSettingsRepository.acclLyricShowTranslation.collectAsState(initial = true)
    val acclShowPhonetic by appSettingsRepository.acclLyricShowPhonetic.collectAsState(initial = true)
    val acclUseBlur by appSettingsRepository.acclLyricUseBlurEffect.collectAsState(initial = true)
    val acclBlurDelta by appSettingsRepository.acclLyricBlurDelta.collectAsState(initial = 3)
    val acclTextAlign by appSettingsRepository.acclLyricTextAlign.collectAsState(initial = "center")
    val acclLinePositionPercent by appSettingsRepository.acclLyricLinePositionPercent.collectAsState(initial = 35)

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
    val lyricSource by viewModel.lyricSource.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    // 横屏 / 大屏（Expanded 宽度，≥840dp）时启用左右双栏布局；竖屏保持单栏
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isExpandedWidth = screenWidthDp >= 840
    val isCompactLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && !isExpandedWidth
    val isTwoPane = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || isExpandedWidth

    // 预测返回手势全局开关：所有 PredictiveBackHandler 的 enabled 都要参与此值
    val predictiveBackEnabled = LocalPredictiveBackEnabled.current

    // 刘海/挖孔屏安全区：竖屏主要影响左右（挖孔在角上），顶部已由 statusBarPadding 处理
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val cutoutHorizontalPadding = PaddingValues(
        start = cutoutPadding.calculateLeftPadding(LayoutDirection.Ltr),
        end = cutoutPadding.calculateRightPadding(LayoutDirection.Ltr)
    )

    var showLyrics by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showCoverViewer by remember { mutableStateOf(false) }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var currentCoverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // 切歌时先清空共享封面，避免歌词页/播放器页短暂显示上一首封面
    LaunchedEffect(currentSong?.id) {
        android.util.Log.i("SongSwitch", "song changed: ${currentSong?.id} at ${System.currentTimeMillis()}")
        currentCoverBitmap = null
    }

    var showSongProperties by remember { mutableStateOf(false) }
    var showLyricInfo by remember { mutableStateOf(false) }

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

    // 队列开合动画 Job：新动画启动前取消旧的，避免快速手势/返回打断时
    // queueProgress 停在中间值导致主内容半上滑、底部工具栏从缝隙露出
    var queueAnimJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun openQueue() {
        queueAnimJob?.cancel()
        queueAnimJob = scope.launch {
            queueProgress.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
        }
    }
    fun closeQueue() {
        queueAnimJob?.cancel()
        queueAnimJob = scope.launch {
            queueProgress.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
        }
    }

    val offsetY = remember { Animatable(screenHeightPx) }

    // 进入动画：从屏幕底部滑入
    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, animationSpec = tween(350, easing = FastOutSlowInEasing))
    }

    // 播放器页返回：预测返回手势驱动页面下滑收起
    PredictiveBackHandler(
        enabled = predictiveBackEnabled && !showLyrics && !isQueueOpen && !showMore && !showCoverViewer && !showLyricInfo && !showSongProperties
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

    // 普通返回（预测性返回关闭时）：无覆盖层时直接关闭播放器。
    // 注意必须放在各覆盖层 BackHandler 之前组合，让覆盖层优先响应。
    BackHandler(
        enabled = !predictiveBackEnabled && !showLyrics && !isQueueOpen && !showMore && !showCoverViewer && !showLyricInfo && !showSongProperties
    ) {
        scope.launch {
            offsetY.animateTo(screenHeightPx, tween(250))
            onBack()
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

            // 播放器背景（根据设置选择：AGSL 流体 / 封面模糊 / 默认颜色）
        PlayerBackground(
            songId = currentSong?.id,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        // 歌词返回手势：驱动 showLyrics 关闭，退出形变交给 AnimatedContent
        PredictiveBackHandler(enabled = predictiveBackEnabled && showLyrics) { progress ->
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

        // 普通返回（预测性返回关闭时）：歌词全屏页先收回歌词
        BackHandler(enabled = !predictiveBackEnabled && showLyrics) {
            showLyrics = false
            scope.launch { lyricsBackProgress.snapTo(0f) }
        }

        if (isTwoPane) {
            // 横屏 / 大屏：左右双栏。左栏播放控件，右栏歌词；竖屏走下方单栏分支。
            // 左栏列宽锚定封面（未收缩时）的适配宽度：信息/播控/进度条与封面严格对齐；
            // 封面暂停收缩（×0.92）只作用于封面自身，不影响列宽，避免整列布局跳动。
            var leftCoverWidth by remember { mutableStateOf<Dp?>(null) }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(cutoutHorizontalPadding)
                    // 大屏双栏左右留白 40dp，不与屏幕边缘贴合；手机横屏紧凑布局维持 16dp
                    .padding(horizontal = if (isCompactLandscape) 16.dp else 40.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = -queueProgress.value * screenHeightPx
                    }
            ) {
                val coverSizeMultiplier by animateFloatAsState(
                    targetValue = if (isCompactLandscape) 1f else if (isPlaying) 1f else coverPauseScale / 100f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "coverSizeMultiplierTwoPane"
                )
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                        .padding(
                            top = statusBarPadding.calculateTopPadding() + 12.dp,
                            bottom = navBarPadding + 24.dp
                        )
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 大屏分支：信息/播控/进度条/底部栏宽度 = 封面实际渲染宽度，整列居中严格对齐。
                    // 封面未测量前为 null，各 section 维持默认 fillMaxWidth（首帧不闪）
                    val leftColWidth = leftCoverWidth?.let { Modifier.width(it) } ?: Modifier

                    // 手机横屏：小封面+歌曲信息横向排列（类似竖屏歌词页底部面板），下方给控制按钮留足空间
                    if (isCompactLandscape) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
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
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    cornerRadiusDp = coverCornerRadius,
                                    rotating = coverRotating,
                                    isPlaying = isPlaying,
                                    useSharedTransition = false
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                PlayerSongInfoSection(
                                    currentSong = currentSong,
                                    isDarkTheme = isDarkTheme,
                                    useSharedTransition = false,
                                    modifier = Modifier.weight(1f, fill = false),
                                    onInfoClick = { showSongInfoSheet = true }
                                )
                            }

                            PlayerProgressSection(
                                position = position,
                                duration = duration,
                                viewModel = viewModel,
                                isDarkTheme = isDarkTheme,
                                useSharedTransition = false,
                                modifier = Modifier.fillMaxWidth()
                            )

                            PlayerControlsSection(
                                isPlaying = isPlaying,
                                isShuffle = shuffleMode,
                                repeatMode = repeatMode,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth()
                            )

                            PlayerBottomActionsSection(
                                openQueue = { openQueue() },
                                onShowMore = { showMore = true },
                                onAddToPlaylistClick = { showAddToPlaylist = true },
                                isDarkTheme = isDarkTheme
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
                            onCoverWidthMeasured = { leftCoverWidth = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            cornerRadiusDp = coverCornerRadius,
                            rotating = coverRotating,
                            isPlaying = isPlaying,
                            useSharedTransition = false,
                            shadowY = coverShadowY,
                            shadowAlphaPercent = coverShadowAlpha,
                            shadowBlur = coverShadowBlur
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        PlayerSongInfoSection(
                            currentSong = currentSong,
                            isDarkTheme = isDarkTheme,
                            useSharedTransition = false,
                            modifier = leftColWidth,
                            onInfoClick = { showSongInfoSheet = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PlayerProgressSection(
                        position = position,
                        duration = duration,
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        useSharedTransition = false,
                        modifier = leftColWidth
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PlayerControlsSection(
                        isPlaying = isPlaying,
                        isShuffle = shuffleMode,
                        repeatMode = repeatMode,
                        viewModel = viewModel,
                        modifier = leftColWidth
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PlayerBottomActionsSection(
                        openQueue = { openQueue() },
                        onShowMore = { showMore = true },
                        onAddToPlaylistClick = { showAddToPlaylist = true },
                        isDarkTheme = isDarkTheme,
                        modifier = leftColWidth
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                        .padding(
                            top = statusBarPadding.calculateTopPadding() + 12.dp,
                            bottom = navBarPadding + 12.dp,
                            start = 12.dp,
                            end = 12.dp
                        )
                ) {
                    PlayerLyricsPane(
                        lyrics = lyrics,
                        currentPosition = position,
                        currentLyricIndex = currentLyricIndex,
                        isPlaying = isPlaying,
                        isDarkTheme = isDarkTheme,
                        acclLyricConfig = AcclLyricConfig(
                            mainTextSizeSp = acclMainTextSize,
                            accompanimentTextSizeSp = acclAccompanimentTextSize,
                            phoneticTextSizeSp = acclPhoneticTextSize,
                            mainFontWeight = acclMainFontWeight,
                            accompanimentFontWeight = acclAccompanimentFontWeight,
                            phoneticFontWeight = acclPhoneticFontWeight,
                            showTranslation = acclShowTranslation,
                            showPhonetic = acclShowPhonetic,
                            useBlur = acclUseBlur,
                            blurDelta = acclBlurDelta,
                            textAlign = acclTextAlign,
                            linePositionPercent = acclLinePositionPercent
                        ),
                        onLineClicked = { line -> viewModel.seekTo(line.start.toLong()) },
                        // 队列全屏覆盖歌词时暂停逐字刷新，降低无效重绘
                        refreshEnabled = !isQueueOpen
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
                    isPlaying = isPlaying,
                    isDarkTheme = isDarkTheme,
                    song = currentSong,
                    duration = duration,
                    viewModel = viewModel,
                    onDismiss = { showLyrics = false },
                    onSeek = { time ->
                        viewModel.seekTo(time)
                    },
                    onInfoClick = { showSongInfoSheet = true },
                    onMoreClick = {
                        // 在歌词页上直接弹出更多菜单，不退出歌词页
                        showMore = true
                    },
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = sharedTransitionScope,
                    // 队列全屏覆盖歌词时暂停逐字刷新，降低无效重绘
                    refreshEnabled = !isQueueOpen
                )
            } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(cutoutHorizontalPadding),
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

                // 封面区域：有封面时按原图宽高比适配（支持不规则封面），无封面时占位为方形；
                // 圆形旋转封面裁切为方形显示
                val coverSizeMultiplier by animateFloatAsState(
                    targetValue = if (isPlaying) 1f else coverPauseScale / 100f,
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
                    cornerRadiusDp = coverCornerRadius,
                    rotating = coverRotating,
                    isPlaying = isPlaying,
                    useSharedTransition = true,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedContent,
                    shadowY = coverShadowY,
                    shadowAlphaPercent = coverShadowAlpha,
                    shadowBlur = coverShadowBlur
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 歌曲信息（与歌词页底部面板共享元素过渡）
                PlayerSongInfoSection(
                    currentSong = currentSong,
                    isDarkTheme = isDarkTheme,
                    useSharedTransition = true,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedContent,
                    onInfoClick = { showSongInfoSheet = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    onAddToPlaylistClick = { showAddToPlaylist = true },
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
        PredictiveBackHandler(enabled = predictiveBackEnabled && isQueueOpen) { progress ->
            try {
                progress.collect { event ->
                    queueProgress.snapTo((1f - event.progress).coerceIn(0f, 1f))
                }
                closeQueue()
            } catch (_: CancellationException) {
                scope.launch { queueProgress.animateTo(1f, tween(200)) }
            }
        }

        // 普通返回（预测性返回关闭时）：队列页先收回队列
        BackHandler(enabled = !predictiveBackEnabled && isQueueOpen) {
            closeQueue()
        }
        if (queueProgress.value > 0f) {
            PlayerQueueScreen(
                queue = queue,
                currentIndex = currentIndex,
                playerViewModel = playerViewModel,
                playlistViewModel = playlistViewModel,
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
                song = currentSong,
                artistSeparators = artistSeparators,
                onDismiss = { showMore = false },
                onAddToPlaylistClick = {
                    showMore = false
                    showAddToPlaylist = true
                },
                onSpeedClick = { /* TODO: 倍速 */ },
                onTimerClick = { viewModel.showScheduledPause() },
                onAudioOutputClick = { /* TODO: 输出与音效 */ },
                onThoughtsClick = { /* TODO: 想法 */ },
                onLyricSettingsClick = { onOpenSettingsCategory("lyrics") },
                onLyricInfoClick = { showLyricInfo = true },
                onViewAlbumClick = {
                    currentSong?.let { song ->
                        onNavigateToAlbum(song.album, song.albumArtist ?: song.artist)
                    }
                },
                onViewArtistClick = {
                    currentSong?.let { song ->
                        val artists = ArtistSplitter.split(song.artist, artistSeparators)
                        if (artists.size == 1) {
                            onNavigateToArtist(artists.first())
                        } else if (artists.isNotEmpty()) {
                            // 多个艺术家时显示选择面板
                            showSongInfoSheet = true
                        }
                    }
                },
                onGoToFolderClick = { /* TODO: 转至文件夹 */ },
                onShareClick = { cn.lemondrop.fhreborn.util.SongFileUtils.shareSong(context, currentSong) },
                onPropertiesClick = { showSongProperties = true },
                onOpenWithClick = { cn.lemondrop.fhreborn.util.SongFileUtils.openWithOtherApp(context, currentSong) },
                onHideClick = { /* TODO: 隐藏音乐 */ },
                onDeleteClick = { /* TODO: 删除文件 */ }
            )
        }

        // 封面大图查看器
        if (showCoverViewer && currentCoverBitmap != null) {
            BackHandler { showCoverViewer = false }
            CoverViewer(
                bitmap = currentCoverBitmap!!,
                title = currentSong?.title ?: "cover",
                onDismiss = { showCoverViewer = false },
                // 封面大图查看器不做旋转，完整显示原图
                rotating = false,
                isPlaying = isPlaying
            )
        }

        // 歌曲信息动作面板（点击艺术家/专辑）
        SongInfoActionSheet(
            visible = showSongInfoSheet,
            artists = currentSong?.let { ArtistSplitter.split(it.artist, artistSeparators) } ?: emptyList(),
            album = currentSong?.album ?: "",
            onDismiss = { showSongInfoSheet = false },
            onArtistClick = { artist ->
                showSongInfoSheet = false
                onNavigateToArtist(artist)
            },
            onAlbumClick = {
                showSongInfoSheet = false
                currentSong?.let { song ->
                    onNavigateToAlbum(song.album, song.albumArtist ?: song.artist)
                }
            }
        )

        // 歌曲属性弹窗
        if (showSongProperties) {
            BackHandler { showSongProperties = false }
            cn.lemondrop.fhreborn.util.SongFileUtils.SongPropertiesDialog(
                song = currentSong,
                onDismiss = { showSongProperties = false }
            )
        }

        // 歌词信息底部面板
        if (showLyricInfo) {
            BackHandler { showLyricInfo = false }
            LyricInfoSheet(
                lyricSource = lyricSource,
                onDismiss = { showLyricInfo = false }
            )
        }

        // 加入歌单弹窗
        if (showAddToPlaylist) {
            BackHandler { showAddToPlaylist = false }
            currentSong?.let { song ->
                AddToPlaylistSheet(
                    songIds = listOf(song.id),
                    viewModel = playlistViewModel,
                    onDismiss = { showAddToPlaylist = false }
                )
            }
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
                style = MiuixTheme.textStyles.footnote2,
                color = fluidOnColorSecondary
            )
            Text(
                text = formatDuration(duration),
                style = MiuixTheme.textStyles.footnote2,
                color = fluidOnColorSecondary
            )
        }
    }
}

@Composable
private fun LyricSheet(
    lyrics: SyncedLyrics?,
    currentPosition: Long,
    isPlaying: Boolean,
    isDarkTheme: Boolean,
    song: Song?,
    duration: Long,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
    onInfoClick: () -> Unit,
    onMoreClick: () -> Unit,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    refreshEnabled: Boolean = true
) {
    val context = LocalContext.current
    val appSettingsRepository = remember { AppSettingsRepository(context) }
    val density = LocalDensity.current

    val listState = rememberLazyListState()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val lyricBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply
    val navBarPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val statusBarPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()

    val acclLyricConfig = rememberAcclLyricConfig(appSettingsRepository)

    // 逐字歌词开关联动 ViewModel：切换后当前歌词即时降级/恢复
    LaunchedEffect(acclLyricConfig.wordLevel) {
        viewModel.setWordLevelEnabled(acclLyricConfig.wordLevel)
    }

    // 封面播放/暂停图标显隐：播放状态刚切换时显示暂停图标5秒；暂停时始终显示播放图标
    var showPauseIcon by remember { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            showPauseIcon = true
            delay(5000L)
            showPauseIcon = false
        } else {
            showPauseIcon = false
        }
    }

    val bgColor = MiuixTheme.colorScheme.background
    val onSurface = MiuixTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                isPlaying = isPlaying,
                listState = listState,
                isDarkTheme = isDarkTheme,
                acclLyricConfig = acclLyricConfig,
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
                },
                refreshEnabled = refreshEnabled
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
                    style = MiuixTheme.textStyles.headline2,
                    color = fluidOnColor.copy(alpha = 0.5f)
                )
            }
        }

        // 底部固定面板：与播放器主屏统一使用 PlayerSongInfoSection + PlayerProgressSection
        val dragThreshold = with(density) { 80.dp.toPx() }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = navBarPadding + 32.dp)
                .padding(horizontal = 28.dp)
                .padding(vertical = 16.dp)
                .pointerInput(Unit) {
                    var accumulatedDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulatedDrag = 0f },
                        onDragEnd = { accumulatedDrag = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        if (accumulatedDrag < -dragThreshold) {
                            viewModel.next()
                            accumulatedDrag = 0f
                        } else if (accumulatedDrag > dragThreshold) {
                            viewModel.previous()
                            accumulatedDrag = 0f
                        }
                    }
                }
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.playPause() }
                        )
                ) {
                    Crossfade(
                        targetState = song,
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                        label = "lyric_sheet_cover_crossfade"
                    ) { targetSong ->
                        targetSong?.let {
                            SongCoverImage(
                                song = it,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 播放/暂停状态图标覆盖层
                    val showPlayIcon = !isPlaying
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showPlayIcon || showPauseIcon,
                        enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
                        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showPlayIcon) Lucide.Play else Lucide.Pause,
                                contentDescription = if (showPlayIcon) "播放" else "暂停",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                PlayerSongInfoSection(
                    currentSong = song,
                    isDarkTheme = isDarkTheme,
                    useSharedTransition = true,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onTitleClick = onDismiss,
                    onInfoClick = onInfoClick,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Lucide.EllipsisVertical,
                        contentDescription = "更多",
                        modifier = Modifier.size(24.dp),
                        tint = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PlayerProgressSection(
                position = currentPosition,
                duration = duration,
                viewModel = null,
                isDarkTheme = isDarkTheme,
                useSharedTransition = true,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class AcclLyricConfig(
    val mainTextSizeSp: Int = 34,
    val accompanimentTextSizeSp: Int = 20,
    val phoneticTextSizeSp: Int = 13,
    val mainFontWeight: Int = 700,
    val accompanimentFontWeight: Int = 700,
    val phoneticFontWeight: Int = 400,
    val showTranslation: Boolean = true,
    val showPhonetic: Boolean = true,
    val wordLevel: Boolean = true,
    val useBlur: Boolean = true,
    val blurDelta: Int = 3,
    val textAlign: String = "center",
    val glowEffect: Boolean = true,
    val breathingDotsSize: Int = 16,
    val translationTextSizeSp: Int = 14,
    val translationFontWeight: Int = 400,
    val linePositionPercent: Int = 35
)

@Composable
private fun rememberAcclLyricConfig(repository: AppSettingsRepository): AcclLyricConfig {
    val mainTextSize by repository.acclLyricMainTextSizeSp.collectAsState(initial = 34)
    val accompanimentTextSize by repository.acclLyricAccompanimentTextSizeSp.collectAsState(initial = 20)
    val phoneticTextSize by repository.acclLyricPhoneticTextSizeSp.collectAsState(initial = 13)
    val mainFontWeight by repository.acclLyricMainFontWeight.collectAsState(initial = 700)
    val accompanimentFontWeight by repository.acclLyricAccompanimentFontWeight.collectAsState(initial = 700)
    val phoneticFontWeight by repository.acclLyricPhoneticFontWeight.collectAsState(initial = 400)
    val showTranslation by repository.acclLyricShowTranslation.collectAsState(initial = true)
    val showPhonetic by repository.acclLyricShowPhonetic.collectAsState(initial = true)
    val wordLevel by repository.acclLyricWordLevel.collectAsState(initial = true)
    val useBlur by repository.acclLyricUseBlurEffect.collectAsState(initial = true)
    val blurDelta by repository.acclLyricBlurDelta.collectAsState(initial = 3)
    val textAlign by repository.acclLyricTextAlign.collectAsState(initial = "center")
    val glowEffect by repository.acclLyricGlowEffect.collectAsState(initial = true)
    val breathingDotsSize by repository.acclLyricBreathingDotsSize.collectAsState(initial = 16)
    val translationTextSize by repository.acclLyricTranslationTextSizeSp.collectAsState(initial = 14)
    val translationFontWeight by repository.acclLyricTranslationFontWeight.collectAsState(initial = 400)
    val linePositionPercent by repository.acclLyricLinePositionPercent.collectAsState(initial = 35)

    return remember(
        mainTextSize, accompanimentTextSize, phoneticTextSize,
        mainFontWeight, accompanimentFontWeight, phoneticFontWeight,
        showTranslation, showPhonetic, wordLevel, useBlur, blurDelta, textAlign,
        glowEffect, breathingDotsSize, translationTextSize, translationFontWeight,
        linePositionPercent
    ) {
        AcclLyricConfig(
            mainTextSizeSp = mainTextSize,
            accompanimentTextSizeSp = accompanimentTextSize,
            phoneticTextSizeSp = phoneticTextSize,
            mainFontWeight = mainFontWeight,
            accompanimentFontWeight = accompanimentFontWeight,
            phoneticFontWeight = phoneticFontWeight,
            showTranslation = showTranslation,
            showPhonetic = showPhonetic,
            wordLevel = wordLevel,
            useBlur = useBlur,
            blurDelta = blurDelta,
            textAlign = textAlign,
            glowEffect = glowEffect,
            breathingDotsSize = breathingDotsSize,
            translationTextSizeSp = translationTextSize,
            translationFontWeight = translationFontWeight,
            linePositionPercent = linePositionPercent
        )
    }
}

@Composable
private fun KaraokeLyricsViewWrapper(
    lyrics: SyncedLyrics,
    currentPosition: Long,
    isPlaying: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isDarkTheme: Boolean,
    acclLyricConfig: AcclLyricConfig,
    modifier: Modifier = Modifier,
    onLineClicked: (ISyncedLine) -> Unit = {},
    refreshEnabled: Boolean = true
) {
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    // 混合模式：发光效果开关关闭时用普通绘制（SrcOver）
    val lyricBlendMode = when {
        !acclLyricConfig.glowEffect -> BlendMode.SrcOver
        isDarkTheme -> BlendMode.Plus
        else -> BlendMode.Multiply
    }
    val breathingDotsDefaults = remember(isDarkTheme, acclLyricConfig.breathingDotsSize) {
        com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeBreathingDotsDefaults(
            size = acclLyricConfig.breathingDotsSize.dp,
            breathingDotsColor = fluidOnColor
        )
    }

    val normalLineTextStyle = remember(acclLyricConfig) {
        TextStyle(
            fontSize = acclLyricConfig.mainTextSizeSp.sp,
            fontWeight = FontWeight(acclLyricConfig.mainFontWeight.coerceIn(1, 1000)),
            textMotion = TextMotion.Animated
        )
    }
    val accompanimentLineTextStyle = remember(acclLyricConfig) {
        TextStyle(
            fontSize = acclLyricConfig.accompanimentTextSizeSp.sp,
            fontWeight = FontWeight(acclLyricConfig.accompanimentFontWeight.coerceIn(1, 1000)),
            textMotion = TextMotion.Animated
        )
    }
    val phoneticTextStyle = remember(acclLyricConfig) {
        TextStyle(
            fontSize = acclLyricConfig.phoneticTextSizeSp.sp,
            fontWeight = FontWeight(acclLyricConfig.phoneticFontWeight.coerceIn(1, 1000)),
            textMotion = TextMotion.Animated
        )
    }

    // 逐字时间（毫秒）：必须是 Compose State——KaraokeLyricsView 内部用
    // derivedStateOf { currentTimeProvider() } 计算当前行/逐字进度/伴唱可见性，
    // derivedStateOf 只响应 State 变化，普通变量只算一次（歌词会完全不动）。
    //
    // 频率权衡：每次写入都触发库内部重算 + LazyColumn 重测（Lookahead 双重测量），
    // 25ms（40fps）在流畅度与负载间取平衡；若仍卡顿再考虑降到 33/50ms。
    var localPositionMs by remember { mutableIntStateOf(currentPosition.toInt().coerceAtLeast(0)) }

    LaunchedEffect(currentPosition) {
        val diff = kotlin.math.abs(localPositionMs - currentPosition.toInt())
        if (diff > 500) {
            localPositionMs = currentPosition.toInt().coerceAtLeast(0)
        }
    }

    LaunchedEffect(isPlaying, refreshEnabled) {
        if (!isPlaying || !refreshEnabled) return@LaunchedEffect
        var lastFrameNs = System.nanoTime()
        var lastPublishedMs = localPositionMs.toLong()
        while (isActive) {
            withFrameNanos { frameNs ->
                val deltaMs = ((frameNs - lastFrameNs) / 1_000_000).toInt().coerceAtLeast(0)
                lastFrameNs = frameNs
                lastPublishedMs += deltaMs
                // 帧对齐 + 40fps 限频（25ms）
                if (lastPublishedMs - localPositionMs >= 25) {
                    localPositionMs = lastPublishedMs.toInt()
                }
            }
        }
    }

    // 歌词视图常驻：不再用 ready 门控销毁重建。
    // 之前每次切歌都会销毁 KaraokeLyricsView、300ms 后全量重建
    // （LazyColumn 首布局 + 可见行组合 + Crossfade + Lookahead 双重测量），
    // 一次性大开销压在主线程 → 切歌 ANR。常驻后歌词变化由库内部平滑处理。
    //
    // 翻译行没有独立样式参数（库写死继承 LocalTextStyle），
    // 用 CompositionLocal 提供翻译行字号 + 字重。
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // 当前行索引：优先取时间命中的行，否则取下一句（前奏/间奏），与库内部逻辑一致
    val currentLineIndex = remember(lyrics, currentPosition) {
        val idx = lyrics.lines.indexOfFirst { currentPosition >= it.start && currentPosition < it.end }
        if (idx != -1) idx
        else {
            val next = lyrics.lines.indexOfFirst { it.start > currentPosition }
            if (next != -1) next else lyrics.lines.lastIndex
        }
    }

    // 当前行高估算（主行 + 翻译行 + 行距），用于“行中心对齐到视口百分比位置”的 offset 换算
    val lineHeightDp = remember(
        textMeasurer, lyrics, currentLineIndex, normalLineTextStyle,
        acclLyricConfig.showTranslation, acclLyricConfig.translationTextSizeSp,
        acclLyricConfig.translationFontWeight
    ) {
        var heightPx = 0f
        lyrics.lines.getOrNull(currentLineIndex)?.let { line ->
            val mainText = when (line) {
                is SyncedLine -> line.content
                is KaraokeLine -> line.syllables.joinToString("") { it.content }
                else -> ""
            }
            if (mainText.isNotBlank()) {
                heightPx += textMeasurer.measure(
                    mainText,
                    normalLineTextStyle.copy(textDirection = TextDirection.Content)
                ).size.height.toFloat()
            }
            if (acclLyricConfig.showTranslation) {
                val translation = line.lyricTranslation()
                if (!translation.isNullOrBlank()) {
                    val translationStyle = TextStyle(
                        fontSize = acclLyricConfig.translationTextSizeSp.sp,
                        fontWeight = FontWeight(acclLyricConfig.translationFontWeight.coerceIn(1, 1000))
                    )
                    heightPx += with(density) { 6.dp.toPx() } // 主行与翻译行间距
                    heightPx += textMeasurer.measure(
                        translation,
                        translationStyle.copy(textDirection = TextDirection.Content)
                    ).size.height.toFloat()
                }
            }
        }
        with(density) { heightPx.toDp() }
    }

    BoxWithConstraints(modifier = modifier) {
        // 当前行位置（百分比）：库的滚动目标是「当前行顶边停在 offset + keepAliveZone 处」，
        // 要让行中心落在视口高度的 p% 处：offset = H*p% - keepAliveZone - 行高/2。
        // 顶部存在渐隐区，百分比过小时物理上无法满足，clamp 到 0 即可（回退为顶对齐）。
        val keepAliveZone = 60.dp
        val offset = (maxHeight * (acclLyricConfig.linePositionPercent / 100f) - keepAliveZone - lineHeightDp / 2)
            .coerceAtLeast(0.dp)

        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides TextStyle(
                fontSize = acclLyricConfig.translationTextSizeSp.sp,
                fontWeight = FontWeight(acclLyricConfig.translationFontWeight.coerceIn(1, 1000))
            )
        ) {
            KaraokeLyricsView(
                listState = listState,
                lyrics = lyrics,
                currentPosition = { localPositionMs },
                onLineClicked = onLineClicked,
                onLinePressed = {},
                // clearAndSetSemantics：砍掉整棵歌词树的语义节点。
                // 切歌/滚动时 Compose 会为语义树做全局排序遍历（SemanticsSortKt），
                // 几百行歌词 × 频繁滚动导致大量分配，堆 192MB 被打满 OOM。
                // 歌词行不需要无障碍朗读，此优化不影响显示。
                modifier = Modifier.fillMaxSize().clearAndSetSemantics { },
                textColor = fluidOnColor,
                blendMode = lyricBlendMode,
                breathingDotsDefaults = breathingDotsDefaults,
                normalLineTextStyle = normalLineTextStyle,
                accompanimentLineTextStyle = accompanimentLineTextStyle,
                phoneticTextStyle = phoneticTextStyle,
                showTranslation = acclLyricConfig.showTranslation,
                showPhonetic = acclLyricConfig.showPhonetic,
                useBlurEffect = acclLyricConfig.useBlur,
                blurDelta = acclLyricConfig.blurDelta.toFloat(),
                offset = offset,
                keepAliveZone = keepAliveZone
            )
        }
    }
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

@Composable
private fun PlaceholderCoverContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "♪",
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
    onCoverWidthMeasured: (Dp) -> Unit = {},
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = 12,
    rotating: Boolean = false,
    isPlaying: Boolean = false,
    useSharedTransition: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    shadowY: Int = 16,
    shadowAlphaPercent: Int = 40,
    shadowBlur: Int = 20
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // 上报封面未收缩时的实际渲染宽度（不乘 coverSizeMultiplier）：
    // 大屏双栏用它作为左栏信息/播控列的宽度锚点，暂停收缩只影响封面自身，不引起整列布局跳动。
    // onSizeChanged 保证窗口缩放/容器尺寸变化时必定重新上报（比仅依赖重组合更可靠）；
    // bitmap 异步加载完成后 displayWidth 变化，由下方 LaunchedEffect 兜底再报一次。
    BoxWithConstraints(
        modifier = modifier.onSizeChanged { size ->
            val bmp = currentCoverBitmap
            val maxWPx = size.width.toFloat()
            val maxHPx = size.height.toFloat()
            val fitW: Float
            if (bmp != null) {
                // 不规则封面：按宽高比适配容器，完整显示原图
                val aspect = bmp.width.toFloat() / bmp.height.toFloat()
                fitW = if (maxWPx / maxHPx > aspect) maxHPx * aspect else maxWPx
            } else {
                // 无封面占位：固定方形
                fitW = minOf(maxWPx, maxHPx)
            }
            val displayW = if (rotating) minOf(fitW, maxHPx) else fitW
            onCoverWidthMeasured(with(density) { displayW.toDp() })
        },
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

        // 圆形旋转封面：固定为正方形（非正方形封面裁切为方形显示）
        val displayWidth = if (rotating) baseCoverWidth.coerceAtMost(baseCoverHeight) else baseCoverWidth
        val displayHeight = if (rotating) baseCoverWidth.coerceAtMost(baseCoverHeight) else baseCoverHeight

        // 上报封面未收缩时的实际渲染宽度（不乘 coverSizeMultiplier）：
        // 大屏双栏用它作为左栏信息/播控列的宽度锚点，暂停收缩只影响封面自身，不引起整列布局跳动
        LaunchedEffect(displayWidth) {
            onCoverWidthMeasured(displayWidth)
        }

        val coverWidth by animateDpAsState(
            targetValue = displayWidth * coverSizeMultiplier,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "coverWidth"
        )
        val coverHeight by animateDpAsState(
            targetValue = displayHeight * coverSizeMultiplier,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "coverHeight"
        )

        val coverShape = if (rotating) CircleShape else RoundedCornerShape(cornerRadiusDp.dp)

        // 圆形旋转封面：播放时匀速旋转（约 20s/圈），暂停时冻结
        var rotation by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(rotating, isPlaying) {
            if (!rotating || !isPlaying) return@LaunchedEffect
            var lastNs = System.nanoTime()
            while (isActive) {
                withFrameNanos { now ->
                    val deltaMs = (now - lastNs) / 1_000_000f
                    lastNs = now
                    rotation = (rotation + deltaMs * 0.018f) % 360f
                }
            }
        }

        val shadowColor = Color.Black.copy(alpha = shadowAlphaPercent / 100f)
        val baseCoverModifier = Modifier
            .size(coverWidth, coverHeight)
            .graphicsLayer {
                if (rotating) rotationZ = rotation
            }
            .drawBehind {
                // 手动画投影：用 Canvas 圆角矩形 + BlurMaskFilter
                if (shadowBlur > 0 && shadowAlphaPercent > 0) {
                    val paint = android.graphics.Paint().apply {
                        color = shadowColor.hashCode()
                        maskFilter = android.graphics.BlurMaskFilter(
                            shadowBlur.dp.toPx(),
                            android.graphics.BlurMaskFilter.Blur.NORMAL
                        )
                        isAntiAlias = true
                    }
                    val cornerRadius = if (rotating) size.minDimension / 2f else cornerRadiusDp.dp.toPx()
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawRoundRect(
                            0f, shadowY.dp.toPx(),
                            size.width, size.height + shadowY.dp.toPx(),
                            cornerRadius, cornerRadius,
                            paint
                        )
                    }
                }
            }
            .clip(coverShape)
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
                targetState = currentSong,
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "cover_crossfade"
            ) { targetSong ->
                var bitmap by remember(targetSong) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(targetSong) {
                    bitmap = null
                    targetSong?.let { song ->
                        withContext(Dispatchers.IO) {
                            bitmap = try {
                                if (song.source == cn.lemondrop.fhreborn.data.db.entity.Song.SOURCE_DIRECTORY) {
                                    val retriever = android.media.MediaMetadataRetriever()
                                    try {
                                        retriever.setDataSource(song.path)
                                        val bytes = retriever.embeddedPicture
                                        bytes?.let {
                                            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                                        }
                                    } finally {
                                        retriever.release()
                                    }
                                } else {
                                    val uri = Uri.parse("content://media/external/audio/media/${song.id}/albumart")
                                    context.contentResolver.openInputStream(uri)?.use { stream ->
                                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                                    }
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }

                // 把当前显示封面同步给查看器，避免长按后再加载
                LaunchedEffect(bitmap) {
                    if (targetSong?.id == currentSong?.id) {
                        onCoverBitmapLoaded(bitmap)
                    }
                }

                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = if (rotating) ContentScale.Crop else ContentScale.Fit,
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
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    onTitleClick: () -> Unit = {},
    onInfoClick: () -> Unit = {}
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
    Column(modifier = sharedModifier.fillMaxWidth().then(modifier)) {
        Text(
            text = currentSong?.title ?: "未在播放",
            style = MiuixTheme.textStyles.headline2.copy(fontWeight = FontWeight.Bold),
            color = fluidOnColorSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    blendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTitleClick
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = currentSong?.let { "${it.artist} - ${it.album}" } ?: "选择一首歌曲开始",
            style = MiuixTheme.textStyles.body1,
            color = fluidOnColorTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onInfoClick
            )
        )
    }
}

@Composable
private fun PlayerProgressSection(
    position: Long,
    duration: Long,
    viewModel: PlayerViewModel?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    useSharedTransition: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    onSeek: ((Long) -> Unit)? = null
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

    Box(modifier = sharedModifier.fillMaxWidth().then(modifier)) {
        PlayerProgressSlider(
            position = position,
            duration = duration,
            onProgressChange = { fraction ->
                val seekTo = (fraction * duration.coerceAtLeast(1L)).toLong()
                onSeek?.invoke(seekTo) ?: viewModel?.seekTo(seekTo)
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
                    style = MiuixTheme.textStyles.footnote2,
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
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.toggleShuffle() }) {
            PlayerIcon(
                imageVector = Lucide.Shuffle,
                contentDescription = "随机播放",
                modifier = Modifier.size(24.dp),
                enabled = isShuffle
            )
        }

        IconButton(onClick = { viewModel.previous() }) {
            PlayerIcon(
                imageVector = Lucide.SkipBack,
                contentDescription = "上一首",
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = { viewModel.playPause() }) {
            PlayerIcon(
                imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(48.dp)
            )
        }

        IconButton(onClick = { viewModel.next() }) {
            PlayerIcon(
                imageVector = Lucide.SkipForward,
                contentDescription = "下一首",
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = { viewModel.toggleRepeatMode() }) {
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
    onAddToPlaylistClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    // 上划打开播放队列（整排底栏图标上方居中）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
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
        IconButton(onClick = { /* TODO: 音频输出 */ }) {
            PlayerIcon(
                imageVector = Lucide.Volume2,
                contentDescription = "音频输出",
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = onAddToPlaylistClick) {
            PlayerIcon(
                imageVector = Lucide.Plus,
                contentDescription = "添加到歌单",
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = { openQueue() }) {
            PlayerIcon(
                imageVector = Lucide.ListMusic,
                contentDescription = "播放队列",
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = { /* TODO: 倍速 */ }) {
            PlayerIcon(
                imageVector = Lucide.Gauge,
                contentDescription = "倍速",
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = { onShowMore() }) {
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
    isPlaying: Boolean,
    isDarkTheme: Boolean,
    acclLyricConfig: AcclLyricConfig,
    onLineClicked: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier,
    refreshEnabled: Boolean = true
) {
    val listState = rememberLazyListState()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    if (lyrics != null) {
        KaraokeLyricsViewWrapper(
            lyrics = lyrics,
            currentPosition = currentPosition,
            isPlaying = isPlaying,
            listState = listState,
            isDarkTheme = isDarkTheme,
            acclLyricConfig = acclLyricConfig,
            modifier = modifier.fillMaxSize(),
            onLineClicked = onLineClicked,
            refreshEnabled = refreshEnabled
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无歌词",
                style = MiuixTheme.textStyles.headline2,
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
    // currentLyricIndex=-1（前奏，还没到第一句）时与第一行对齐，
    // 下一句取其后一行，避免把第一行再显示一遍
    val currentIndex = currentLyricIndex.coerceAtLeast(0)
    val currentLine = lyricLines.getOrNull(currentIndex)
    val nextLine = lyricLines.getOrNull(currentIndex + 1)
    val lyricTextAlign = if (lyricAlignCenter) TextAlign.Center else TextAlign.Start
    // 当前句实际占几行：折行后（两行）不再显示第二行的翻译/下一句
    var currentLineCount by remember { mutableIntStateOf(1) }
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
            // 第一行：当前句，一行放不下自动折行，上限两行；两行同字号
            Text(
                text = currentLine.lyricContent(),
                modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                style = MiuixTheme.textStyles.body1,
                color = fluidOnColorSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = lyricTextAlign,
                onTextLayout = { currentLineCount = it.lineCount }
            )
            // 第二行：当前句未折行时显示翻译（有则翻译，无则下一句）；
            // 都没有时保留空行占位，保持两行高度稳定
            if (currentLineCount <= 1) {
                Spacer(modifier = Modifier.height(2.dp))
                val translation = currentLine.lyricTranslation()
                val secondLine = if (!translation.isNullOrBlank()) translation
                else nextLine?.lyricContent()
                Text(
                    text = secondLine?.takeIf { it.isNotBlank() } ?: " ",
                    modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                    style = MiuixTheme.textStyles.body1,
                    color = if (secondLine.isNullOrBlank()) fluidOnColorVeryHint
                    else fluidOnColorSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = lyricTextAlign
                )
            }
        } else {
            Text(
                text = "Beyond words.",
                modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                style = MiuixTheme.textStyles.body1,
                color = fluidOnColorHint,
                maxLines = 1,
                textAlign = lyricTextAlign
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "无言以和。",
                modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                style = MiuixTheme.textStyles.body1,
                color = fluidOnColorVeryHint,
                maxLines = 1,
                textAlign = lyricTextAlign
            )
        }
    }
}

