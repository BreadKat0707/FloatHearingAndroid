@file:OptIn(ExperimentalSharedTransitionApi::class)

package cn.lemondrop.fhreborn.ui.screens.player

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import kotlin.math.min

@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit = {}
) {
    val viewModel = playerViewModel
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val lyricAlignCenter by settingsRepository.isLyricAlignCenter.collectAsState(initial = false)

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val timerRemaining by viewModel.timerRemaining.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    var showTimer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showCoverViewer by remember { mutableStateOf(false) }
    var currentCoverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val queueProgress = remember { Animatable(0f) }
    val isQueueOpen by remember { derivedStateOf { queueProgress.value > 0.5f } }
    val lyricsProgress by animateFloatAsState(
        targetValue = if (showLyrics) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "lyricsProgress"
    )

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
            // 播放器背景（根据设置选择：旋转流体 / AGSL 流体 / 封面模糊 / 默认颜色）
        PlayerBackground(
            songId = currentSong?.id,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            // 播放器主内容：随队列呼出向上滑走；打开歌词时淡出
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = -queueProgress.value * screenHeightPx
                        alpha = 1f - lyricsProgress
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
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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

                    val coverModifier = with(this@SharedTransitionLayout) {
                        Modifier
                            .sharedElementWithCallerManagedVisibility(
                                sharedContentState = rememberSharedContentState(key = "player_cover"),
                                visible = true
                            )
                            .size(coverWidth, coverHeight)
                            .shadow(20.dp, RoundedCornerShape(FluentLargeCorner))
                            .clip(RoundedCornerShape(FluentLargeCorner))
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = {
                                    currentCoverBitmap?.let { showCoverViewer = true }
                                }
                            )
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
                                    currentCoverBitmap = bitmap
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

                Spacer(modifier = Modifier.height(28.dp))

                // 歌曲信息
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = currentSong?.title ?: "未在播放",
                        modifier = Modifier.fluidBlend(targetBlendMode),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = fluidOnColorSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong?.let { "${it.artist} - ${it.album}" } ?: "选择一首歌曲开始",
                        modifier = Modifier.fluidBlend(targetBlendMode),
                        style = MaterialTheme.typography.bodyLarge,
                        color = fluidOnColorTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 歌词预览（两行）始终留占位
                val lyricLines = lyrics?.lines ?: emptyList()
                val currentLine = lyricLines.getOrNull(currentLyricIndex.coerceAtLeast(0))
                val nextLine = lyricLines.getOrNull((currentLyricIndex + 1).coerceAtMost(lyricLines.lastIndex))
                val lyricTextAlign = if (lyricAlignCenter) TextAlign.Center else TextAlign.Start
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showLyrics = true }
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
                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(8.dp))

                // 进度条
                val safeDuration = duration.coerceAtLeast(1L)
                val progressValue = (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

                CustomProgressBar(
                    progress = progressValue,
                    onProgressChange = { fraction ->
                        viewModel.seekTo((fraction * safeDuration).toLong())
                    },
                    blendMode = targetBlendMode,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(position),
                        modifier = Modifier.fluidBlend(targetBlendMode),
                        style = MaterialTheme.typography.labelSmall,
                        color = fluidOnColorTertiary
                    )
                    Text(
                        text = formatDuration(duration),
                        modifier = Modifier.fluidBlend(targetBlendMode),
                        style = MaterialTheme.typography.labelSmall,
                        color = fluidOnColorTertiary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 播放控制
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FluentIconButton(onClick = { viewModel.toggleShuffle() }) {
                        PlayerIcon(
                            imageVector = Lucide.Shuffle,
                            contentDescription = "随机播放",
                            modifier = Modifier.size(24.dp),
                            enabled = shuffleMode
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

                Spacer(modifier = Modifier.height(16.dp))

                // 底部操作栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                    FluentIconButton(onClick = { showMore = true }) {
                        PlayerIcon(
                            imageVector = Lucide.EllipsisVertical,
                            contentDescription = "更多",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 上划打开播放队列
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = { openQueue() }
                        )
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Lucide.ChevronUp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = fluidOnColorTertiary
                        )
                        Text(
                            text = "播放队列",
                            style = MaterialTheme.typography.labelMedium,
                            color = fluidOnColorTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // 播放队列：从底部滑入
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
        }

        // 定时弹窗
        val isEndOfSongTimer by viewModel.isEndOfSongTimer.collectAsState(initial = false)
        if (showTimer) {
            BackHandler { showTimer = false }
            TimerSheet(
                currentMinutes = (timerRemaining / 1000 / 60).toInt(),
                isEndOfSongTimer = isEndOfSongTimer,
                onDismiss = { showTimer = false },
                onSetTimer = { minutes ->
                    viewModel.setTimer(minutes)
                },
                onSetEndOfSongTimer = {
                    viewModel.setEndOfSongTimer()
                },
                onCancelTimer = {
                    viewModel.cancelTimer()
                }
            )
        }

        // 更多菜单弹窗
        if (showMore) {
            BackHandler { showMore = false }
            PlayerMoreSheet(
                onDismiss = { showMore = false },
                onTimerClick = { showTimer = true },
                onShareClick = { shareCurrentSong(context, currentSong) }
            )
        }

        // 歌词弹窗
        val currentLyrics = lyrics
        AnimatedVisibility(
            visible = showLyrics && currentLyrics != null,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200))
        ) {
            BackHandler(enabled = showLyrics) { showLyrics = false }
            LyricSheet(
                lyrics = currentLyrics ?: return@AnimatedVisibility,
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
                    showLyrics = false
                    showMore = true
                },
                animatedVisibilityScope = this,
                sharedTransitionScope = this@SharedTransitionLayout
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
private fun CustomProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    blendMode: BlendMode = BlendMode.Plus
) {
    var dragProgress by remember { mutableFloatStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }
    val displayProgress = if (isDragging) dragProgress else progress
    val isDarkTheme = LocalAppDarkTheme.current
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .fluidBlend(blendMode)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
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
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(fluidOnColor.copy(alpha = 0.2f))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(displayProgress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor.copy(alpha = 0.9f))
        )
    }
}

@Composable
private fun LyricSheet(
    lyrics: SyncedLyrics,
    currentPosition: Long,
    isDarkTheme: Boolean,
    song: Song?,
    coverBitmap: ImageBitmap?,
    duration: Long,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
    onMoreClick: () -> Unit,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null
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
                val smallCoverModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "player_cover"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                    }
                } else {
                    Modifier
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

                Column(
                    modifier = Modifier.weight(1f)
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

            val progress = if (duration > 0) currentPosition / duration.toFloat() else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(onSurface.copy(alpha = 0.2f))
                    .pointerInput(duration) {
                        detectHorizontalDragGestures(
                            onDragStart = {},
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeek((fraction * duration).toLong())
                            },
                            onDragEnd = {}
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary)
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
