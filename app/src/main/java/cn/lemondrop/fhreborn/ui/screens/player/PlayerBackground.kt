package cn.lemondrop.fhreborn.ui.screens.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lemondrop.fhreborn.data.repository.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
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

    val baseBg = if (isDarkTheme) Color.Black else Color.White
    val overlay = if (isDarkTheme) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = songId,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "cover_blur_bg"
        ) { currentSongId ->
            var bitmap by remember(currentSongId) { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(currentSongId) {
                withContext(Dispatchers.IO) {
                    bitmap = loadPlayerCoverBitmap(context, currentSongId)
                }
            }

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

    val baseBg = if (isDarkTheme) Color.Black else Color.White
    val overlay = if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = songId,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "agsl_fluid_bg"
        ) { currentSongId ->
            var bitmap by remember(currentSongId) { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(currentSongId) {
                withContext(Dispatchers.IO) {
                    bitmap = loadPlayerCoverBitmap(context, currentSongId)
                }
            }

            bitmap?.let { bmp ->
                AndroidView(
                    factory = { AgslFluidView(it) },
                    update = { it.setBitmap(bmp) },
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(60.dp)
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslFluidView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var runtimeShader: RuntimeShader? = null
    private var bitmap: Bitmap? = null
    private val choreographer = Choreographer.getInstance()
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            invalidate()
            choreographer.postFrameCallback(this)
        }
    }
    private val startTime = SystemClock.elapsedRealtime()

    private val shaderCode = """
        uniform shader image;
        uniform float time;
        uniform vec2 resolution;

        // 简单伪噪声
        float noise(vec2 p) {
            return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 coord) {
            vec2 uv = coord / resolution;
            vec2 center = uv - 0.5;
            float len = length(center);
            float angle = atan(center.y, center.x);

            // 旋转波纹
            float swirl = sin(len * 18.0 - time * 2.2) * 0.035;
            float wave = sin(angle * 6.0 + time * 1.3) * 0.025;
            vec2 distorted = uv + vec2(
                cos(angle) * swirl - sin(angle) * wave,
                sin(angle) * swirl + cos(angle) * wave
            );

            // 横向/纵向流动
            distorted.x += sin(distorted.y * 14.0 + time * 1.6) * 0.02;
            distorted.y += cos(distorted.x * 12.0 + time * 1.1) * 0.02;

            // 轻微湍流
            distorted += (noise(distorted * 8.0 + time) - 0.5) * 0.012;

            return image.eval(distorted * resolution);
        }
    """.trimIndent()

    fun setBitmap(bmp: Bitmap) {
        if (bitmap == bmp) return
        bitmap = bmp
        runtimeShader = null
        requestLayout()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShader()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val shader = runtimeShader ?: return
        val seconds = (SystemClock.elapsedRealtime() - startTime) / 1000f
        shader.setFloatUniform("time", seconds)
        shader.setFloatUniform("resolution", width.toFloat(), height.toFloat())
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        choreographer.removeFrameCallback(frameCallback)
    }

    private fun updateShader() {
        val bmp = bitmap ?: return
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        val bitmapShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = android.graphics.Matrix()
        val scale = maxOf(w / bmp.width, h / bmp.height)
        matrix.setScale(scale, scale)
        val dx = (w - bmp.width * scale) * 0.5f
        val dy = (h - bmp.height * scale) * 0.5f
        matrix.postTranslate(dx, dy)
        bitmapShader.setLocalMatrix(matrix)

        val runtime = RuntimeShader(shaderCode)
        runtime.setInputShader("image", bitmapShader)
        runtime.setFloatUniform("resolution", w, h)
        runtimeShader = runtime
        paint.shader = runtime
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
