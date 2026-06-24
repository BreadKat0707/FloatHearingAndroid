package cn.lemondrop.fhreborn.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.ui.screens.player.FluidBackground
import cn.lemondrop.fhreborn.ui.theme.FluentIconButton
import cn.lemondrop.fhreborn.ui.theme.FluentLargeCorner
import cn.lemondrop.fhreborn.ui.viewmodel.PlayerViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.SkipForward
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SongCoverImage(
    songId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(songId) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(songId) {
        withContext(Dispatchers.IO) {
            bitmap = try {
                val uri = Uri.parse("content://media/external/audio/media/$songId/albumart")
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♪",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun MiniPlayBar(
    playerViewModel: PlayerViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    val fluidOnColor = if (isDarkTheme) Color.White else Color.Black
    val fluidOnColorSecondary = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
    val targetBlendMode = if (isDarkTheme) BlendMode.Plus else BlendMode.Multiply

    val safeDuration = duration.coerceAtLeast(1L)
    val progressValue = (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(1.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFAFAFAF).copy(alpha = 0.5f),
                shape = RoundedCornerShape(FluentLargeCorner)
            )
            .clip(RoundedCornerShape(FluentLargeCorner))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(FluentLargeCorner))
        ) {
            FluidBackground(
                songId = currentSong?.id,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
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
                        .background(fluidOnColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♪", color = fluidOnColorSecondary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "Float Hearing",
                        color = fluidOnColor,
                        maxLines = 1
                    )
                    Text(
                        text = currentSong?.artist ?: "Make some sounds",
                        style = MaterialTheme.typography.bodySmall,
                        color = fluidOnColorSecondary,
                        maxLines = 1
                    )
                }

                currentSong?.let {
                    FluentIconButton(onClick = { playerViewModel.playPause() }) {
                        Icon(
                            imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    blendMode = targetBlendMode
                                },
                            tint = fluidOnColor.copy(alpha = 0.6f)
                        )
                    }
                    FluentIconButton(onClick = { playerViewModel.next() }) {
                        Icon(
                            imageVector = Lucide.SkipForward,
                            contentDescription = "下一首",
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    blendMode = targetBlendMode
                                },
                            tint = fluidOnColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomStart)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressValue)
                        .fillMaxSize()
                        .background(fluidOnColor.copy(alpha = 0.5f))
                )
            }
        }
    }
}
