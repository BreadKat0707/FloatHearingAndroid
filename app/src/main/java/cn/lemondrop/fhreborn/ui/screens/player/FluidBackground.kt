package cn.lemondrop.fhreborn.ui.screens.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@Composable
fun FluidBackground(
    songId: Long?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val bgColor = if (isDarkTheme) Color.Black else Color.White
    val overlayColor = if (isDarkTheme) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.6f)
    val layerAlpha1 = if (isDarkTheme) 0.6f else 0.6f
    val layerAlpha2 = if (isDarkTheme) 0.4f else 0.4f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // 让图片旋转时始终覆盖整个容器：对角线要 >= 容器最大边
        val containerSize = maxWidth.coerceAtLeast(maxHeight)
        val imageSize = containerSize * sqrt(2f) * 1.15f

        // 切歌时使用 Crossfade 做淡入淡出过渡
        Crossfade(
            targetState = songId,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "fluid_bg"
        ) { currentSongId ->
            var bitmap by remember(currentSongId) { mutableStateOf<android.graphics.Bitmap?>(null) }

            LaunchedEffect(currentSongId) {
                withContext(Dispatchers.IO) {
                    bitmap = loadEnhancedAlbumArt(context, currentSongId, isDarkTheme, imageSize)
                }
            }

            bitmap?.let { bmp ->
                val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

                Box(modifier = Modifier.fillMaxSize()) {
                    // 全屏背景层 - 慢速同向旋转
                    FluidLayer(
                        bitmap = imageBitmap,
                        rotationDuration = 120000,
                        rotationDirection = 1f,
                        baseScale = 1f,
                        imageSize = imageSize,
                        blurRadius = 80.dp,
                        alpha = layerAlpha1,
                        isPlaying = isPlaying
                    )

                    // 中层 - 同向但更快，制造流动感
                    FluidLayer(
                        bitmap = imageBitmap,
                        rotationDuration = 90000,
                        rotationDirection = 1f,
                        baseScale = 1f,
                        imageSize = imageSize,
                        blurRadius = 60.dp,
                        alpha = layerAlpha2,
                        isPlaying = isPlaying
                    )
                }
            }
        }

        // 遮罩，确保内容可读
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )
    }
}

@Composable
private fun FluidLayer(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    rotationDuration: Int,
    rotationDirection: Float,
    baseScale: Float,
    imageSize: androidx.compose.ui.unit.Dp,
    blurRadius: androidx.compose.ui.unit.Dp,
    alpha: Float,
    isPlaying: Boolean
) {
    // 使用 Animatable 实现可暂停的旋转和轻微呼吸缩放
    val rotation = remember { Animatable(0f) }
    val animatedScale = remember { Animatable(baseScale) }

    LaunchedEffect(isPlaying, rotationDirection, rotationDuration) {
        if (isPlaying) {
            while (isActive) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f * rotationDirection,
                    animationSpec = tween(rotationDuration, easing = LinearEasing)
                )
            }
        }
    }

    LaunchedEffect(isPlaying, baseScale) {
        if (isPlaying) {
            while (isActive) {
                animatedScale.animateTo(
                    targetValue = baseScale * 1.03f,
                    animationSpec = tween(4000, easing = EaseInOutSine)
                )
                animatedScale.animateTo(
                    targetValue = baseScale * 0.97f,
                    animationSpec = tween(4000, easing = EaseInOutSine)
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(imageSize)
                .scale(animatedScale.value)
                .graphicsLayer {
                    rotationZ = rotation.value
                }
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(blurRadius)
                    } else Modifier
                )
                .alpha(alpha),
            contentScale = ContentScale.Crop
        )
    }
}

private suspend fun loadEnhancedAlbumArt(
    context: Context,
    songId: Long?,
    isDarkTheme: Boolean = true,
    targetSize: androidx.compose.ui.unit.Dp = 600.dp
): Bitmap? {
    if (songId == null) return null
    return try {
        val uri = Uri.parse("content://media/external/audio/media/$songId/albumart")
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4 // 降采样以提高性能
            }
            BitmapFactory.decodeStream(stream, null, options)?.let { bmp ->
                // 缩放到目标尺寸，确保旋转时不露出边缘
                val density = context.resources.displayMetrics.density
                val targetPx = (targetSize.value * density).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bmp, targetPx, targetPx, true)

                // 深色模式高饱和，浅色模式低饱和避免刺眼
                val sat = if (isDarkTheme) 1.8f else 1.6f
                val invSat = 1f - sat
                val r = 0.213f * invSat
                val g = 0.715f * invSat
                val b = 0.072f * invSat
                val matrix = floatArrayOf(
                    r + sat, g, b, 0f, 0f,
                    r, g + sat, b, 0f, 0f,
                    r, g, b + sat, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                val cm = android.graphics.ColorMatrix(matrix)
                val paint = android.graphics.Paint().apply {
                    colorFilter = ColorMatrixColorFilter(cm)
                }
                val result = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
                android.graphics.Canvas(result).drawBitmap(scaled, 0f, 0f, paint)
                result
            }
        }
    } catch (e: Exception) {
        null
    }
}
