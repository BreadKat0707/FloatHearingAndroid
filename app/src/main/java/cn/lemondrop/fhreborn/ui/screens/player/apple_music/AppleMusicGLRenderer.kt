package cn.lemondrop.fhreborn.ui.screens.player.apple_music

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import cn.lemondrop.fhreborn.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES renderer for the Apple Music inspired fluid background.
 * Full GPU pipeline: rotation → Gaussian blur → color treatment → mesh warp → output.
 *
 * Ported from Lyricify-Backgrounds HLSL (Apache 2.0).
 */
class AppleMusicGLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // Settings (updated from UI thread)
    @Volatile var speedMultiplier = 1f
    @Volatile var crossfadeDurationMs = 600
    @Volatile var saturationMultiplier = 1f
    @Volatile var renderScaleValue = 0.5f
    @Volatile var playingState = true
    @Volatile var bassPulse = 0f

    private var width = 0
    private var height = 0
    private var startTime = 0L
    private var frozenTime = 0f
    private var pauseStartTime = 0L

    // Textures
    private var currentTexture = 0
    private var previousTexture = 0

    // Shader programs
    private var rotationProgram = 0
    private var blurProgram = 0
    private var outputProgram = 0
    private var meshProgram = 0

    // FBOs for multi-pass rendering
    private var fboA = 0
    private var fboATexture = 0
    private var fboB = 0
    private var fboBTexture = 0
    private var fboWidth = 0
    private var fboHeight = 0

    // Fullscreen quad
    private var quadVAO = 0
    private var quadVBO = 0
    private var quadEBO = 0

    // Mesh grid
    private var meshVAO = 0
    private var meshVBO = 0
    private var meshEBO = 0
    private var meshIndexCount = 0
    private var meshResult: AppleMusicMesh.MeshResult? = null

    // Artwork crossfade
    private var transitionStartTime = 0L
    private var isTransitioning = false
    private var currentBitmap: Bitmap? = null
    private var previousBitmap: Bitmap? = null
    private var pendingArtwork: Bitmap? = null
    private var pendingPrevious: Bitmap? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        startTime = System.nanoTime() / 1_000_000_000L

        // Compile shaders
        rotationProgram = createProgram(
            loadRawShader(R.raw.applemusic_vertex),
            loadRawShader(R.raw.applemusic_rotation_frag)
        )
        blurProgram = createProgram(
            loadRawShader(R.raw.applemusic_vertex),
            loadRawShader(R.raw.applemusic_blur_frag)
        )
        outputProgram = createProgram(
            loadRawShader(R.raw.applemusic_vertex),
            loadRawShader(R.raw.applemusic_output_frag)
        )
        meshProgram = createProgram(
            loadRawShader(R.raw.applemusic_mesh_vertex),
            loadRawShader(R.raw.applemusic_mesh_frag)
        )

        // Setup fullscreen quad VAO
        setupQuad()
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w
        height = h

        // Recreate FBOs at scaled resolution
        val scale = renderScaleValue.coerceIn(0.25f, 1f)
        fboWidth = (w * scale).toInt().coerceAtLeast(2)
        fboHeight = (h * scale).toInt().coerceAtLeast(2)
        setupFBOs()
        setupMesh()
    }

    override fun onDrawFrame(gl: GL10?) {
        // Upload pending artwork on GL thread
        pendingArtwork?.let { bmp ->
            pendingPrevious?.let { prev ->
                if (previousTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(previousTexture), 0)
                previousTexture = uploadTexture(prev)
            } ?: run {
                if (previousTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(previousTexture), 0)
                previousTexture = 0
            }
            if (currentTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(currentTexture), 0)
            currentTexture = uploadTexture(bmp)
            pendingArtwork = null
            pendingPrevious = null
            android.util.Log.i("AppleMusicGL", "Texture uploaded: $currentTexture (${bmp.width}x${bmp.height})")
        }

        if (currentTexture == 0) {
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            return
        }

        // If rotation program failed to compile, draw texture directly as fallback
        if (rotationProgram == 0) {
            android.util.Log.w("AppleMusicGL", "Rotation program=0, using fallback")
            drawFallback()
            return
        }

        val time = getTime()
        val transitionMix = getTransitionMix()
        val imageScale = 1f + bassPulse * 0.15f

        // Pass 1: Rotation → FBO A
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboA)
        GLES30.glViewport(0, 0, fboWidth, fboHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(rotationProgram)
        setTextureUniform(rotationProgram, "currentTexture", currentTexture, 0)
        setTextureUniform(rotationProgram, "previousTexture", previousTexture, 1)
        GLES30.glUniform1f(getUniform(rotationProgram, "time"), time)
        GLES30.glUniform1f(getUniform(rotationProgram, "transitionMix"), transitionMix)
        GLES30.glUniform2f(getUniform(rotationProgram, "resolution"), fboWidth.toFloat(), fboHeight.toFloat())
        GLES30.glUniform1f(getUniform(rotationProgram, "speedMul"), speedMultiplier)
        GLES30.glUniform1f(getUniform(rotationProgram, "imageScale"), imageScale)
        drawQuad()

        // Pass 2: Horizontal blur → FBO B
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboB)
        GLES30.glViewport(0, 0, fboWidth, fboHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(blurProgram)
        setTextureUniform(blurProgram, "texture", fboATexture, 0)
        GLES30.glUniform2f(getUniform(blurProgram, "direction"), 1f / fboWidth, 0f)
        GLES30.glUniform1f(getUniform(blurProgram, "blurRadius"), 40f * renderScaleValue)
        drawQuad()

        // Pass 3: Vertical blur → FBO A
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboA)
        GLES30.glViewport(0, 0, fboWidth, fboHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(blurProgram)
        setTextureUniform(blurProgram, "texture", fboBTexture, 0)
        GLES30.glUniform2f(getUniform(blurProgram, "direction"), 0f, 1f / fboHeight)
        GLES30.glUniform1f(getUniform(blurProgram, "blurRadius"), 40f * renderScaleValue)
        drawQuad()

        // Pass 4: Output (saturation + scrim + dither) → FBO B
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboB)
        GLES30.glViewport(0, 0, fboWidth, fboHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(outputProgram)
        setTextureUniform(outputProgram, "texture", fboATexture, 0)
        GLES30.glUniform1f(getUniform(outputProgram, "saturationMul"), saturationMultiplier)
        GLES30.glUniform2f(getUniform(outputProgram, "resolution"), fboWidth.toFloat(), fboHeight.toFloat())
        drawQuad()

        // Pass 5: Mesh warp → screen
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(meshProgram)
        setTextureUniform(meshProgram, "texture", fboBTexture, 0)
        GLES30.glUniform1f(getUniform(meshProgram, "time"), time)
        GLES30.glUniform2f(getUniform(meshProgram, "resolution"), width.toFloat(), height.toFloat())
        drawMesh()
    }

    fun setArtwork(bitmap: Bitmap?) {
        if (bitmap == null) return
        val copy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false) ?: return
        pendingPrevious = currentBitmap
        pendingArtwork = copy
        isTransitioning = pendingPrevious != null
        transitionStartTime = System.nanoTime() / 1_000_000_000L
    }

    fun setPlaying(playing: Boolean) {
        if (playing == playingState) return
        if (playing) {
            val pausedDuration = System.nanoTime() / 1_000_000_000L - pauseStartTime
            frozenTime += pausedDuration
        } else {
            pauseStartTime = System.nanoTime() / 1_000_000_000L
        }
        playingState = playing
    }

    fun release() {
        currentBitmap?.recycle()
        previousBitmap?.recycle()
        currentBitmap = null
        previousBitmap = null
    }

    // --- Private helpers ---

    private fun getTime(): Float {
        val raw = (System.nanoTime() / 1_000_000_000L - startTime).toFloat()
        return if (playingState) raw - frozenTime else frozenTime
    }

    private fun getTransitionMix(): Float {
        if (!isTransitioning) return 1f
        val elapsed = (System.nanoTime() / 1_000_000_000L - transitionStartTime).toFloat()
        val duration = crossfadeDurationMs / 1000f
        return if (elapsed >= duration) {
            isTransitioning = false
            previousBitmap?.recycle()
            previousBitmap = null
            1f
        } else {
            (elapsed / duration).coerceIn(0f, 1f)
        }
    }

    private fun setupQuad() {
        val vertices = floatArrayOf(
            -1f, -1f, 0f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f,
             1f, -1f, 1f, 0f
        )
        val indices = shortArrayOf(0, 1, 2, 2, 3, 0)

        val vaoBuf = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoBuf, 0)
        quadVAO = vaoBuf[0]

        val bufs = IntArray(2)
        GLES30.glGenBuffers(2, bufs, 0)
        quadVBO = bufs[0]
        quadEBO = bufs[1]

        GLES30.glBindVertexArray(quadVAO)

        val vBuf = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vBuf.put(vertices).position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, vBuf, GLES30.GL_STATIC_DRAW)

        val iBuf = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        iBuf.put(indices).position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, quadEBO)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, iBuf, GLES30.GL_STATIC_DRAW)

        // position: vec2 at offset 0
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        // texcoord: vec2 at offset 8
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)

        GLES30.glBindVertexArray(0)
    }

    private fun setupMesh() {
        val result = AppleMusicMesh.create(
            presetIndex = AppleMusicMesh.selectPreset(),
            isVerticalLayout = true
        )
        meshResult = result
        meshIndexCount = result.indices.size

        val vertices = FloatArray(result.vertexPositions.size + result.texCoords.size / 2 * 2)
        // Interleave position + texcoord
        val vertexCount = result.vertexPositions.size / 2
        val interleaved = FloatArray(vertexCount * 4)
        for (i in 0 until vertexCount) {
            interleaved[i * 4] = result.vertexPositions[i * 2]
            interleaved[i * 4 + 1] = result.vertexPositions[i * 2 + 1]
            interleaved[i * 4 + 2] = result.texCoords[i * 2]
            interleaved[i * 4 + 3] = result.texCoords[i * 2 + 1]
        }

        val vaoBuf = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoBuf, 0)
        meshVAO = vaoBuf[0]

        val bufs = IntArray(2)
        GLES30.glGenBuffers(2, bufs, 0)
        meshVBO = bufs[0]
        meshEBO = bufs[1]

        GLES30.glBindVertexArray(meshVAO)

        val vBuf = ByteBuffer.allocateDirect(interleaved.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vBuf.put(interleaved).position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, meshVBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, interleaved.size * 4, vBuf, GLES30.GL_STATIC_DRAW)

        val iBuf = ByteBuffer.allocateDirect(result.indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        iBuf.put(result.indices).position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, meshEBO)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, result.indices.size * 2, iBuf, GLES30.GL_STATIC_DRAW)

        // position: vec2 at offset 0
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        // texcoord: vec2 at offset 8
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)

        GLES30.glBindVertexArray(0)
    }

    private fun setupFBOs() {
        // Delete old FBOs
        if (fboA != 0) {
            GLES30.glDeleteFramebuffers(2, intArrayOf(fboA, fboB), 0)
            GLES30.glDeleteTextures(2, intArrayOf(fboATexture, fboBTexture), 0)
        }
        fboA = createFBO(fboWidth, fboHeight).also { fboATexture = it.second }.first
        fboB = createFBO(fboWidth, fboHeight).also { fboBTexture = it.second }.first
    }

    private fun createFBO(w: Int, h: Int): Pair<Int, Int> {
        val texBuf = IntArray(1)
        GLES30.glGenTextures(1, texBuf, 0)
        val tex = texBuf[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, w, h, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        val fboBuf = IntArray(1)
        GLES30.glGenFramebuffers(1, fboBuf, 0)
        val fbo = fboBuf[0]
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, tex, 0)

        return Pair(fbo, tex)
    }

    private fun uploadTexture(bitmap: Bitmap): Int {
        val texBuf = IntArray(1)
        GLES30.glGenTextures(1, texBuf, 0)
        val tex = texBuf[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        val err = GLES30.glGetError()
        if (err != 0) android.util.Log.e("AppleMusicGL", "texImage2D GL error: $err")
        return tex
    }

    private fun drawQuad() {
        GLES30.glBindVertexArray(quadVAO)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun drawMesh() {
        GLES30.glBindVertexArray(meshVAO)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, meshIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun setTextureUniform(program: Int, name: String, textureId: Int, unit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(getUniform(program, name), unit)
    }

    private fun getUniform(program: Int, name: String): Int {
        return GLES30.glGetUniformLocation(program, name)
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (vs == 0 || fs == 0) return 0
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            android.util.Log.e("AppleMusicGL", "Program link failed: ${GLES30.glGetProgramInfoLog(program)}")
            GLES30.glDeleteProgram(program)
            return 0
        }
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            android.util.Log.e("AppleMusicGL", "Shader compile failed (${if (type == GLES30.GL_VERTEX_SHADER) "VS" else "FS"}): $log")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun loadRawShader(resId: Int): String {
        return context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
    }

    /**
     * Fallback: just draw the current texture directly without any effects.
     * Used when shaders fail to compile.
     */
    private var fallbackProgram = 0

    private fun drawFallback() {
        if (fallbackProgram == 0) {
            val vs = """#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUV;
out vec2 vUV;
void main() { vUV = aUV; gl_Position = vec4(aPos, 0.0, 1.0); }"""
            val fs = """#version 300 es
precision highp float;
uniform sampler2D tex;
in vec2 vUV;
out vec4 fc;
void main() { fc = texture(tex, vUV); }"""
            fallbackProgram = createProgram(vs, fs)
        }
        if (fallbackProgram == 0) return

        GLES30.glViewport(0, 0, width, height)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(fallbackProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, currentTexture)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(fallbackProgram, "tex"), 0)
        drawQuad()
    }
}
