package cn.lemondrop.fhreborn.player.effects

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageSwitcher
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.doOnLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Accord（Gramophone）风格的流体背景 View。
 *
 * 原理：封面整图模糊打底，左上 1/4 与右下 1/4 切片以不同速度绕各自中心旋转，
 * 外层容器反向缓转，叠加高饱和度与暗色遮罩，形成流体流动感。
 *
 * 移植自 AccordLegacy 的 BlendView，纯代码构建（无 XML / constraintlayout 依赖）。
 */
class BlendView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val imageViewTS: ImageSwitcher
    private val imageViewBE: ImageSwitcher
    private val imageViewBG: ImageView
    private val rotateFrame: FrameLayout

    private var isAnimationOngoing = true
    private var lastPlaying: Boolean? = null
    private var lastSongId: Long? = null
    private var overlayColor = 0x66000000.toInt()
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val VIEW_TRANSIT_DURATION: Long = 400
        const val FULL_BLUR_RADIUS: Float = 120F
        const val SHALLOW_BLUR_RADIUS: Float = 60F
        const val UPDATE_RUNNABLE_INTERVAL: Long = 34
        const val CYCLE: Int = 360
        const val SATURATION_FACTOR: Float = 1.5F
        const val PICTURE_SIZE: Int = 60
    }

    init {
        setBackgroundColor(android.graphics.Color.BLACK)

        // 背景：完整封面（普通 ImageView，避免 ImageSwitcher 在窗口尺寸变化时丢图）
        imageViewBG = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        addView(
            imageViewBG,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // 旋转容器
        rotateFrame = FrameLayout(context)
        addView(
            rotateFrame,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // 左上 1/4 与右下 1/4 切片。
        // 注意：不能用 MATCH_PARENT / 2（MATCH_PARENT = -1，-1/2 = 0），
        // 尺寸必须在布局完成后按父容器实际尺寸设置。
        imageViewTS = createImageSwitcher()
        rotateFrame.addView(
            imageViewTS,
            FrameLayout.LayoutParams(0, 0, Gravity.LEFT or Gravity.TOP)
        )
        imageViewBE = createImageSwitcher()
        rotateFrame.addView(
            imageViewBE,
            FrameLayout.LayoutParams(0, 0, Gravity.RIGHT or Gravity.BOTTOM)
        )

        doOnLayout {
            val w = (width / 2).coerceAtLeast(1)
            val h = (height / 2).coerceAtLeast(1)
            imageViewTS.layoutParams = FrameLayout.LayoutParams(w, h, Gravity.LEFT or Gravity.TOP)
            imageViewBE.layoutParams = FrameLayout.LayoutParams(w, h, Gravity.RIGHT or Gravity.BOTTOM)
        }
    }

    /**
     * 全屏 RenderEffect 模糊在软件渲染环境（模拟器/无 GPU 加速）下重渲染极慢，
     * 切歌时会导致主线程阻塞数秒。仅在硬件加速时启用模糊。
     */
    private fun applyBlurIfSupported() {
        val canBlur = isHardwareAccelerated
        setRenderEffect(
            if (canBlur) {
                RenderEffect.createBlurEffect(FULL_BLUR_RADIUS, FULL_BLUR_RADIUS, Shader.TileMode.MIRROR)
            } else {
                null
            }
        )
    }

    private fun createImageSwitcher(): ImageSwitcher {
        val animationIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in).apply {
            duration = VIEW_TRANSIT_DURATION
        }
        val animationOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out).apply {
            duration = VIEW_TRANSIT_DURATION
        }
        return ImageSwitcher(context).apply {
            setFactory {
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
            }
            inAnimation = animationIn
            outAnimation = animationOut
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.drawColor(overlayColor)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyBlurIfSupported()
    }

    /** 设置封面（内部按 songId 去重，相同则不重新解码） */
    fun setSong(songId: Long?) {
        if (songId == lastSongId) return
        lastSongId = songId

        if (songId == null) {
            clearImageViews()
            return
        }
        val uri = Uri.parse("content://media/external/audio/media/$songId/albumart")
        CoroutineScope(Dispatchers.IO).launch {
            val originalBitmap = getBitmapFromUri(uri)
            if (originalBitmap != null) {
                // 饱和度增强 + 切片裁剪都在 IO 线程完成，主线程只做 setImageDrawable
                val enhanced = enhanceBitmap(originalBitmap)
                val ts = cropTopLeftQuarter(enhanced)
                val be = cropBottomRightQuarter(enhanced)
                withContext(Dispatchers.Main) {
                    updateImageViews(enhanced, ts, be)
                }
            } else {
                withContext(Dispatchers.Main) {
                    clearImageViews()
                }
            }
        }
    }

    /** 播放/暂停控制（按状态去重） */
    fun setPlaying(playing: Boolean) {
        if (playing == lastPlaying) return
        lastPlaying = playing
        if (playing) startRotationAnimation() else stopRotationAnimation()
    }

    private fun updateImageViews(bitmap: Bitmap, topLeft: Bitmap, bottomRight: Bitmap) {
        imageViewTS.setImageDrawable(topLeft.toDrawable(resources))
        imageViewBE.setImageDrawable(bottomRight.toDrawable(resources))
        imageViewBG.setImageDrawable(bitmap.toDrawable(resources))
    }

    private fun clearImageViews() {
        imageViewTS.setImageDrawable(null)
        imageViewBE.setImageDrawable(null)
        imageViewBG.setImageDrawable(null)
    }

    fun animateBlurRadius(enlarge: Boolean, duration: Long) {
        val fromVal = if (enlarge) SHALLOW_BLUR_RADIUS else FULL_BLUR_RADIUS
        val toVal = if (enlarge) FULL_BLUR_RADIUS else SHALLOW_BLUR_RADIUS
        ValueAnimator.ofFloat(fromVal, toVal).apply {
            this.duration = duration
            addUpdateListener { animator ->
                val radius = animator.animatedValue as Float
                val renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
                post { this@BlendView.setRenderEffect(renderEffect) }
            }
            start()
        }
    }

    fun startRotationAnimation() {
        handler.removeCallbacks(rotationRunnable)
        isAnimationOngoing = true
        handler.postDelayed(rotationRunnable, UPDATE_RUNNABLE_INTERVAL)
    }

    fun stopRotationAnimation() {
        handler.removeCallbacks(rotationRunnable)
        isAnimationOngoing = false
    }

    private val rotationRunnable = object : Runnable {
        override fun run() {
            imageViewTS.rotation = (imageViewTS.rotation + 1.2f) % CYCLE
            imageViewBE.rotation = (imageViewBE.rotation + .67f) % CYCLE
            rotateFrame.rotation = (rotateFrame.rotation - .6f) % CYCLE
            if (isAnimationOngoing) {
                handler.postDelayed(this, UPDATE_RUNNABLE_INTERVAL)
            }
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        var inputStream: java.io.InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            options.inSampleSize = calculateInSampleSize(options)
            options.inJustDecodeBounds = false
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
        } catch (_: Exception) {
            null
        } finally {
            inputStream?.close()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val (height, width) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > PICTURE_SIZE || width > PICTURE_SIZE) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= PICTURE_SIZE && (halfWidth / inSampleSize) >= PICTURE_SIZE) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /** 提高饱和度，让流体色彩更浓郁 */
    private fun enhanceBitmap(bitmap: Bitmap): Bitmap {
        val enhancedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        enhancedBitmap.density = bitmap.density

        val enhancePaint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(SATURATION_FACTOR) }
        enhancePaint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        val canvas = Canvas(enhancedBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, enhancePaint)
        return enhancedBitmap
    }

    private fun cropTopLeftQuarter(bitmap: Bitmap): Bitmap {
        val quarterWidth = bitmap.width / 2
        val quarterHeight = bitmap.height / 2
        return Bitmap.createBitmap(bitmap, 0, 0, quarterWidth, quarterHeight)
    }

    private fun cropBottomRightQuarter(bitmap: Bitmap): Bitmap {
        val quarterWidth = bitmap.width / 2
        val quarterHeight = bitmap.height / 2
        return Bitmap.createBitmap(bitmap, quarterWidth, quarterHeight, quarterWidth, quarterHeight)
    }
}
