package cn.lemondrop.fhreborn.ui.screens.player.apple_music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import cn.lemondrop.fhreborn.R
import kotlin.math.max

/**
 * Renders the Apple Music inspired fluid background using AGSL RuntimeShader.
 * Ported from Lyricify-Backgrounds (Apache 2.0).
 *
 * Core pipeline: three rotating artwork layers blended via AGSL shader.
 * External blur is applied via Compose Modifier.blur().
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class AppleMusicBackgroundRenderer(private val context: Context) {

    private var rotationShader: RuntimeShader? = null
    private var currentArtworkShader: BitmapShader? = null
    private var previousArtworkShader: BitmapShader? = null
    private var currentArtwork: Bitmap? = null
    private var previousArtwork: Bitmap? = null

    private val startTime = SystemClock.elapsedRealtime()
    private var transitionStartTime = 0L
    private var isTransitioning = false

    private var viewWidth = 0
    private var viewHeight = 0

    var isLightTheme = false
    var bassPulseScale = 1f

    private var imageScale = 1f

    fun setArtwork(bitmap: Bitmap?) {
        if (bitmap == null) return
        val current = currentArtwork
        if (current != null && current.width == bitmap.width && current.height == bitmap.height
            && current.sameAs(bitmap)) return

        previousArtwork = current
        previousArtworkShader = currentArtworkShader
        currentArtwork = bitmap
        currentArtworkShader = createArtworkShader(bitmap)

        isTransitioning = current != null
        transitionStartTime = SystemClock.elapsedRealtime()
        createShaders()
    }

    fun setSize(width: Int, height: Int) {
        if (width == viewWidth && height == viewHeight) return
        viewWidth = width
        viewHeight = height
        createShaders()
    }

    fun setImageScale(scale: Float) {
        imageScale = 1f + 0.33f * scale * scale * bassPulseScale
    }

    fun render(canvas: Canvas) {
        if (viewWidth <= 0 || viewHeight <= 0) return
        val shader = rotationShader ?: return
        val current = currentArtworkShader ?: return
        val prev = previousArtworkShader ?: current

        val time = (SystemClock.elapsedRealtime() - startTime) / 1000f
        val transitionMix = getTransitionMix()

        setupShaderInput(current, prev)

        shader.setInputShader("currentArtwork", current)
        shader.setInputShader("previousArtwork", prev)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("transitionMix", transitionMix)
        shader.setFloatUniform("viewScale", viewWidth.toFloat(), viewHeight.toFloat())
        shader.setFloatUniform("imageScale", imageScale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = this@AppleMusicBackgroundRenderer.rotationShader
        }
        canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), paint)
    }

    private fun getTransitionMix(): Float {
        if (!isTransitioning) return 1f
        val elapsed = (SystemClock.elapsedRealtime() - transitionStartTime) / 1000f
        return if (elapsed >= 0.5f) {
            isTransitioning = false
            previousArtwork = null
            previousArtworkShader = null
            1f
        } else {
            (elapsed / 0.5f).coerceIn(0f, 1f)
        }
    }

    private fun setupShaderInput(shader: BitmapShader, _dummy: BitmapShader) {
        val bmp = currentArtwork ?: return
        val scale = max(viewWidth.toFloat() / bmp.width, viewHeight.toFloat() / bmp.height)
        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(
            (viewWidth - bmp.width * scale) / 2f,
            (viewHeight - bmp.height * scale) / 2f
        )
        shader.setLocalMatrix(matrix)
    }

    private fun createArtworkShader(bitmap: Bitmap): BitmapShader {
        return BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    private fun createShaders() {
        try {
            val code = context.resources.openRawResource(R.raw.rotation_shader)
                .bufferedReader().use { it.readText() }
            rotationShader = RuntimeShader(code)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        rotationShader = null
        currentArtwork = null
        previousArtwork = null
        currentArtworkShader = null
        previousArtworkShader = null
    }
}
