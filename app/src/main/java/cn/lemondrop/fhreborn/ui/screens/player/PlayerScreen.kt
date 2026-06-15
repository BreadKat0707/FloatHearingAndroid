package cn.lemondrop.fhreborn.ui.screens.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import cn.lemondrop.fhreborn.data.repository.SettingsRepository
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import cn.lemondrop.fhreborn.data.lyrics.LyricLine
import cn.lemondrop.fhreborn.ui.screens.library.SongCoverImage
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import cn.lemondrop.fhreborn.ui.theme.FluentLargeCorner
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Repeat1
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Volume2
import kotlinx.coroutines.launch

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

    var showQueue by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 300.dp.toPx() }
    val snapThreshold = with(density) { 150.dp.toPx() }
    val screenHeightPx = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
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

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // 流体背景上的前景色（深色用白，浅色用黑）
    val isDarkTheme = isSystemInDarkTheme()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val fluidOnColorSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
    val fluidOnColorTertiary = if (isDarkTheme) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.4f)
    val fluidOnColorHint = if (isDarkTheme) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)
    val fluidOnColorVeryHint = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)
    val targetBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffsetY.toInt()) }
            .clip(RoundedCornerShape(cornerRadius))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        if (offsetY.value > snapThreshold) {
                            scope.launch {
                                offsetY.animateTo(screenHeightPx, animationSpec = tween(250))
                                onBack()
                            }
                        } else {
                            scope.launch {
                                offsetY.animateTo(0f, animationSpec = tween(250))
                            }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        // 只允许向下滑动
                        if (dragAmount > 0f || offsetY.value > 0f) {
                            val newValue = (offsetY.value + dragAmount).coerceAtLeast(0f)
                            scope.launch { offsetY.snapTo(newValue) }
                        }
                    }
                )
            }
    ) {
        // 流体背景（始终填满全屏，不随内容缩小）
        FluidBackground(
            songId = currentSong?.id,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .padding(
                    top = statusBarPadding.calculateTopPadding(),
                    bottom = navBarPadding.calculateBottomPadding()
                )
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部拖动条（纯视觉，不可点击）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
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

            // 封面区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                currentSong?.let { song ->
                    SongCoverImage(
                        songId = song.id,
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .aspectRatio(1f)
                            .shadow(20.dp, RoundedCornerShape(FluentLargeCorner))
                            .clip(RoundedCornerShape(FluentLargeCorner))
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .shadow(20.dp, RoundedCornerShape(FluentLargeCorner))
                        .clip(RoundedCornerShape(FluentLargeCorner)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .fluidBlend(targetBlendMode)
                            .background(fluidOnColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "♪",
                            modifier = Modifier.fluidBlend(targetBlendMode),
                            style = MaterialTheme.typography.displayLarge,
                            color = fluidOnColor.copy(alpha = 0.5f)
                        )
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
                    color = fluidOnColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentSong?.let { "${it.artist} - ${it.album}" } ?: "选择一首歌曲开始",
                    modifier = Modifier.fluidBlend(targetBlendMode),
                    style = MaterialTheme.typography.bodyLarge,
                    color = fluidOnColorSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 歌词预览（两行）始终留占位
            val currentLine = lyrics.getOrNull(currentLyricIndex.coerceAtLeast(0))
            val nextLine = lyrics.getOrNull((currentLyricIndex + 1).coerceAtMost(lyrics.lastIndex))
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
                if (lyrics.isNotEmpty() && currentLine != null) {
                    Text(
                        text = currentLine.content,
                        modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                        style = MaterialTheme.typography.bodyLarge,
                        color = fluidOnColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = lyricTextAlign
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (currentLine.translation.isNotEmpty()) {
                        Text(
                            text = currentLine.translation,
                            modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = fluidOnColorTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = lyricTextAlign
                        )
                    } else if (nextLine != null) {
                        Text(
                            text = nextLine.content,
                            modifier = Modifier.fillMaxWidth().fluidBlend(targetBlendMode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = fluidOnColorTertiary,
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
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FluentIconButton(onClick = { viewModel.toggleShuffle() }) {
                    PlayerIcon(
                        imageVector = Lucide.Shuffle,
                        contentDescription = "随机播放",
                        modifier = Modifier.size(22.dp)
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 底部操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FluentIconButton(onClick = { /* TODO: 音频输出 */ }) {
                    PlayerIcon(
                        imageVector = Lucide.Volume2,
                        contentDescription = "音频输出",
                        modifier = Modifier.size(22.dp)
                    )
                }

                FluentIconButton(onClick = { /* TODO: 添加到歌单 */ }) {
                    PlayerIcon(
                        imageVector = Lucide.FolderPlus,
                        contentDescription = "添加到歌单",
                        modifier = Modifier.size(22.dp)
                    )
                }

                FluentIconButton(onClick = { showQueue = true }) {
                    PlayerIcon(
                        imageVector = Lucide.ListMusic,
                        contentDescription = "播放队列",
                        modifier = Modifier.size(22.dp)
                    )
                }

                FluentIconButton(onClick = { /* TODO: 倍速 */ }) {
                    PlayerIcon(
                        imageVector = Lucide.Gauge,
                        contentDescription = "倍速",
                        modifier = Modifier.size(22.dp)
                    )
                }

                FluentIconButton(onClick = { /* TODO: 分享 */ }) {
                    PlayerIcon(
                        imageVector = Lucide.Share2,
                        contentDescription = "分享",
                        modifier = Modifier.size(22.dp)
                    )
                }

                FluentIconButton(onClick = { showMore = true }) {
                    PlayerIcon(
                        imageVector = Lucide.EllipsisVertical,
                        contentDescription = "更多",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 队列弹窗
        if (showQueue) {
            QueueSheet(
                queue = queue,
                currentIndex = currentIndex,
                onDismiss = { showQueue = false },
                onItemClick = { index ->
                    viewModel.seekTo(index)
                    showQueue = false
                },
                onRemove = { index ->
                    viewModel.removeFromQueue(index)
                }
            )
        }

        // 定时弹窗
        val isEndOfSongTimer by viewModel.isEndOfSongTimer.collectAsState(initial = false)
        if (showTimer) {
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
            PlayerMoreSheet(
                onDismiss = { showMore = false },
                onTimerClick = { showTimer = true }
            )
        }

        // 歌词弹窗
        if (showLyrics) {
            LyricSheet(
                lyrics = lyrics,
                currentIndex = currentLyricIndex,
                lyricAlignCenter = lyricAlignCenter,
                currentPosition = position,
                onDismiss = { showLyrics = false },
                onSeek = { time ->
                    viewModel.seekTo(time)
                }
            )
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
    val isDarkTheme = isSystemInDarkTheme()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black

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
                .background(fluidOnColor.copy(alpha = 0.85f))
        )
    }
}

@Composable
private fun LyricSheet(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    lyricAlignCenter: Boolean,
    currentPosition: Long,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val lyricTextAlign = if (lyricAlignCenter) TextAlign.Center else TextAlign.Start
    val columnHAlign = if (lyricAlignCenter) Alignment.CenterHorizontally else Alignment.Start
    val isDarkTheme = isSystemInDarkTheme()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val fluidOnColorSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.65f)
    val fluidOnColorTertiary = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.25f)
    val lyricBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.animateScrollToItem(
                index = currentIndex.coerceAtMost(lyrics.lastIndex),
                scrollOffset = -240
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 80.dp, horizontal = 32.dp),
            horizontalAlignment = columnHAlign
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == currentIndex
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onSeek(line.startTime)
                            }
                        ),
                    horizontalAlignment = columnHAlign
                ) {
                    // 逐字高亮歌词（仅当前行且有时戳）
                    if (isCurrent && line.wordTimestamps.isNotEmpty()) {
                        KaraokeText(
                            line = line,
                            currentPosition = currentPosition,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = lyricTextAlign,
                            blendMode = lyricBlendMode
                        )
                    } else {
                        Text(
                            text = line.content,
                            modifier = Modifier.fluidBlend(lyricBlendMode),
                            style = if (isCurrent) {
                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            } else MaterialTheme.typography.bodyLarge,
                            color = if (isCurrent) fluidOnColor else fluidOnColorTertiary,
                            textAlign = lyricTextAlign
                        )
                    }
                    if (line.translation.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = line.translation,
                            modifier = Modifier.fluidBlend(lyricBlendMode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrent) fluidOnColorSecondary else fluidOnColorTertiary,
                            textAlign = lyricTextAlign
                        )
                    }
                }
            }
        }
    }
}

/**
 * 逐字 Karaoke 高亮渲染
 */
@Composable
private fun KaraokeText(
    line: LyricLine,
    currentPosition: Long,
    textStyle: androidx.compose.ui.text.TextStyle,
    textAlign: TextAlign,
    blendMode: BlendMode = BlendMode.Plus
) {
    val isDarkTheme = isSystemInDarkTheme()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val annotatedString = remember(line, currentPosition, isDarkTheme) {
        buildAnnotatedString {
            val words = line.wordTimestamps
            var charIndex = 0
            words.forEach { word ->
                val text = line.content.substring(charIndex, word.charEndIndex.coerceAtMost(line.content.length))
                val isSung = currentPosition >= word.endTime
                val isSinging = currentPosition >= word.startTime && currentPosition < word.endTime

                val color = when {
                    isSung -> fluidOnColor
                    isSinging -> {
                        // 正在唱的字做渐变效果：根据进度计算 alpha
                        val progress = (currentPosition - word.startTime).toFloat() / (word.endTime - word.startTime).toFloat()
                        fluidOnColor.copy(alpha = 0.4f + progress * 0.6f)
                    }
                    else -> fluidOnColor.copy(alpha = 0.4f)
                }

                withStyle(style = SpanStyle(color = color)) {
                    append(text)
                }
                charIndex = word.charEndIndex
            }
            // 剩余文本（没有逐字时戳的部分）
            if (charIndex < line.content.length) {
                withStyle(style = SpanStyle(color = fluidOnColor.copy(alpha = 0.4f))) {
                    append(line.content.substring(charIndex))
                }
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = Modifier.fluidBlend(blendMode),
        style = textStyle,
        textAlign = textAlign
    )
}

@Composable
private fun PlayerIcon(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val iconColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.6f)
    } else {
        Color.Black.copy(alpha = 0.5f)
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

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}
