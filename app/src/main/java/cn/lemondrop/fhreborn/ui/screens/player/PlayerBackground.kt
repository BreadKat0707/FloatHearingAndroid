package cn.lemondrop.fhreborn.ui.screens.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.ui.graphics.asComposeRenderEffect
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

sealed class PlayerBackgroundType(val key: String) {
    data object RotatingFluid : PlayerBackgroundType("rotating_fluid")
    data object AgslFluid : PlayerBackgroundType("agsl_fluid")
    data object CoverBlur : PlayerBackgroundType("cover_blur")
    data object DefaultColor : PlayerBackgroundType("default_color")

    companion object {
        fun fromKey(key: String?): PlayerBackgroundType = when (key) {
            RotatingFluid.key -> RotatingFluid
            AgslFluid.key -> AgslFluid
            CoverBlur.key -> CoverBlur
            DefaultColor.key -> DefaultColor
            else -> RotatingFluid
        }
    }
}

@Composable
fun PlayerBackground(
    songId: Long?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AppSettingsRepository(context) }
    val bgKey by repository.getString("player_bg", PlayerBackgroundType.RotatingFluid.key)
        .collectAsState(initial = PlayerBackgroundType.RotatingFluid.key)
    val type = remember(bgKey) { PlayerBackgroundType.fromKey(bgKey) }

    // 跟随 app 主题（而不是系统）判断深浅
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    when (type) {
        PlayerBackgroundType.RotatingFluid -> FluidBackground(
            songId = songId,
            isPlaying = isPlaying,
            isDarkTheme = isDarkTheme,
            modifier = modifier
        )
        PlayerBackgroundType.AgslFluid -> AgslFluidBackground(
            songId = songId,
            isDarkTheme = isDarkTheme,
            modifier = modifier
        )
        PlayerBackgroundType.CoverBlur -> CoverBlurBackground(
            songId = songId,
            isDarkTheme = isDarkTheme,
            modifier = modifier
        )
        PlayerBackgroundType.DefaultColor -> DefaultPlayerBackground(
            isDarkTheme = isDarkTheme,
            modifier = modifier
        )
    }
}

@Composable
fun DefaultPlayerBackground(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color.Black else Color.White)
    )
}

@Composable
fun CoverBlurBackground(
    songId: Long?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var bitmap by remember(songId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(songId) {
        withContext(Dispatchers.IO) {
            bitmap = loadPlayerCoverBitmap(context, songId)
        }
    }

    val baseBg = if (isDarkTheme) Color.Black else Color.White
    val overlay = if (isDarkTheme) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { bmp ->
            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerSize = maxWidth.coerceAtLeast(maxHeight)
                val imageSize = containerSize * sqrt(2f) * 1.1f
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(imageSize)
                        .blur(80.dp)
                        .graphicsLayer { alpha = 0.85f }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlay)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslFluidBackgroundImpl(
    songId: Long?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var bitmap by remember(songId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(songId) {
        withContext(Dispatchers.IO) {
            bitmap = loadPlayerCoverBitmap(context, songId)
        }
    }

    val baseBg = if (isDarkTheme) Color.Black else Color.White
    val overlay = if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { bmp ->
            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }

                data class AgslState(val shader: RuntimeShader, val effect: androidx.compose.ui.graphics.RenderEffect)

                val state = remember(bmp, wPx, hPx) {
                    val bitmapShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    val matrix = android.graphics.Matrix()
                    val scale = maxOf(
                        wPx / bmp.width.coerceAtLeast(1).toFloat(),
                        hPx / bmp.height.coerceAtLeast(1).toFloat()
                    )
                    matrix.setScale(scale, scale)
                    val dx = (wPx - bmp.width * scale) * 0.5f
                    val dy = (hPx - bmp.height * scale) * 0.5f
                    matrix.postTranslate(dx, dy)
                    bitmapShader.setLocalMatrix(matrix)

                    val runtime = RuntimeShader(
                        """
                        uniform shader image;
                        uniform float time;
                        uniform vec2 resolution;

                        half4 main(float2 coord) {
                            vec2 uv = coord / resolution;
                            float wave1 = sin(uv.y * 10.0 + time * 1.5) * 0.02;
                            float wave2 = cos(uv.x * 8.0 + time * 1.2) * 0.015;
                            float wave3 = sin((uv.x + uv.y) * 6.0 + time * 0.7) * 0.01;
                            vec2 distorted = uv + vec2(wave1 + wave3, wave2 + wave3);
                            return image.eval(distorted * resolution);
                        }
                        """.trimIndent()
                    )
                    runtime.setInputShader("image", bitmapShader)
                    runtime.setFloatUniform("resolution", wPx, hPx)
                    AgslState(
                        shader = runtime,
                        effect = RenderEffect.createShaderEffect(runtime).asComposeRenderEffect()
                    )
                }

                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.renderEffect = state.effect
                        }
                        .blur(40.dp)
                )

                // 时间推进：直接改 shader uniform，不需要重组 RenderEffect
                LaunchedEffect(state.shader) {
                    var t = 0f
                    while (isActive) {
                        t += 0.016f
                        state.shader.setFloatUniform("time", t)
                        delay(16)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlay)
        )
    }
}

@Composable
fun AgslFluidBackground(
    songId: Long?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslFluidBackgroundImpl(songId, isDarkTheme, modifier)
    } else {
        // 低版本回退到旋转流体
        FluidBackground(songId, isPlaying = true, isDarkTheme = isDarkTheme, modifier = modifier)
    }
}

internal suspend fun loadPlayerCoverBitmap(
    context: Context,
    songId: Long?,
    targetSize: android.util.Size = android.util.Size(600, 600)
): Bitmap? {
    if (songId == null) return null
    return try {
        val uri = Uri.parse("content://media/external/audio/media/$songId/albumart")
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.let { bmp ->
                Bitmap.createScaledBitmap(
                    bmp,
                    targetSize.width,
                    targetSize.height,
                    true
                )
            }
        }
    } catch (e: Exception) {
        null
    }
}
