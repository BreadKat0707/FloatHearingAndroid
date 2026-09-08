package cn.lemondrop.fhreborn.ui.screens.player.apple_music

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils

/**
 * GLSurfaceView wrapper that manages OpenGL texture lifecycle
 * and integrates with the AppleMusicGLRenderer.
 */
class AppleMusicGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val glRenderer: AppleMusicGLRenderer
    private var pendingBitmap: Bitmap? = null
    private var pendingPreviousBitmap: Bitmap? = null

    init {
        setEGLContextClientVersion(3)
        glRenderer = AppleMusicGLRenderer(context)
        setRenderer(glRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setArtwork(bitmap: Bitmap?) {
        if (bitmap == null) return
        // GL texture operations must happen on GL thread
        queueEvent {
            glRenderer.setArtwork(bitmap)
        }
    }

    fun setPlaying(playing: Boolean) {
        glRenderer.setPlaying(playing)
    }

    var speedMultiplier: Float
        get() = glRenderer.speedMultiplier
        set(value) { glRenderer.speedMultiplier = value }

    var crossfadeDurationMs: Int
        get() = glRenderer.crossfadeDurationMs
        set(value) { glRenderer.crossfadeDurationMs = value }

    var saturationMultiplier: Float
        get() = glRenderer.saturationMultiplier
        set(value) { glRenderer.saturationMultiplier = value }

    var renderScaleValue: Float
        get() = glRenderer.renderScaleValue
        set(value) { glRenderer.renderScaleValue = value }

    var bassPulse: Float
        get() = glRenderer.bassPulse
        set(value) { glRenderer.bassPulse = value }

    var bassPulseProcessor: BassPulseProcessor? = null
        set(value) {
            field?.release()
            field = value
        }

    fun release() {
        glRenderer.release()
        bassPulseProcessor?.release()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}
