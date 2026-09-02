package cn.lemondrop.fhreborn.ui.screens.player.apple_music

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sin

/**
 * Renders the mesh warp (pinch/liquid) deformation effect.
 * API 34+: uses android.graphics.Mesh for GPU-accelerated mesh rendering.
 * API 31-33: falls back to drawing deformed quads via Canvas.
 *
 * Ported from Lyricify-Backgrounds AppleMusicInspiredMesh.cs (Apache 2.0).
 */
class AppleMusicMeshRenderer {

    private var currentPresetIndex = -1
    private var currentIsVertical = true
    private var meshResult: AppleMusicMesh.MeshResult? = null

    /**
     * Set the mesh preset. Call when orientation or preset changes.
     */
    fun setPreset(presetIndex: Int, isVertical: Boolean) {
        if (presetIndex == currentPresetIndex && isVertical == currentIsVertical) return
        currentPresetIndex = presetIndex
        currentIsVertical = isVertical
        meshResult = AppleMusicMesh.create(
            presetIndex = presetIndex,
            isVerticalLayout = isVertical
        )
    }

    /**
     * Draw the mesh-deformed image onto the canvas.
     * @param source the blurred/rotated background bitmap to deform
     * @param canvas target canvas
     * @param time animation time in seconds
     * @param width canvas width
     * @param height canvas height
     */
    fun draw(
        source: Bitmap,
        canvas: Canvas,
        time: Float,
        width: Int,
        height: Int,
        paint: Paint
    ) {
        val result = meshResult ?: return

        // Compute mesh warp phase (same as original HLSL PinchVertex)
        val pi = PI.toFloat()
        val meshWarpTimeScale = 5f
        val phase = (acos(sin(time * pi / meshWarpTimeScale)) / pi).toFloat()
        val mixValue = phase * phase * (3f - 2f * phase) // smoothstep

        // Interpolate between identity grid and deformed grid
        val gridSide = result.gridSide
        val positions = FloatArray(result.vertexPositions.size)
        for (i in positions.indices step 2) {
            val idx = i / 2
            val row = idx / gridSide
            val col = idx % gridSide
            val identityX = col.toFloat() / (gridSide - 1).coerceAtLeast(1) * 2f - 1f
            val identityY = row.toFloat() / (gridSide - 1).coerceAtLeast(1) * 2f - 1f
            positions[i] = identityX + (result.vertexPositions[i] - identityX) * mixValue
            positions[i + 1] = identityY + (result.vertexPositions[i + 1] - identityY) * mixValue
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            drawWithMeshApi(source, canvas, positions, result, width, height, paint)
        } else {
            drawWithCanvasFallback(source, canvas, positions, result, width, height, paint)
        }
    }

    /**
     * API 34+: Use android.graphics.Mesh for GPU-accelerated rendering.
     * NOTE: Mesh API not available in current compile SDK, uses canvas fallback.
     */
    private fun drawWithMeshApi(
        source: Bitmap,
        canvas: Canvas,
        positions: FloatArray,
        result: AppleMusicMesh.MeshResult,
        width: Int,
        height: Int,
        paint: Paint
    ) {
        drawWithCanvasFallback(source, canvas, positions, result, width, height, paint)
    }

    /**
     * API 31-33: Fallback — draw deformed grid as individual quads.
     */
    private fun drawWithCanvasFallback(
        source: Bitmap,
        canvas: Canvas,
        positions: FloatArray,
        result: AppleMusicMesh.MeshResult,
        width: Int,
        height: Int,
        paint: Paint
    ) {
        // Simple fallback: just draw the source with some distortion
        // by sampling regions of the bitmap at offset positions
        val gridSide = result.gridSide
        val cellW = width.toFloat() / (gridSide - 1)
        val cellH = height.toFloat() / (gridSide - 1)

        // Convert NDC positions to pixel positions
        val pixelPositions = FloatArray(positions.size)
        for (i in positions.indices step 2) {
            pixelPositions[i] = (positions[i] * 0.5f + 0.5f) * width
            pixelPositions[i + 1] = (0.5f - positions[i + 1] * 0.5f) * height
        }

        // Draw each quad of the mesh
        for (row in 0 until gridSide - 1) {
            for (col in 0 until gridSide - 1) {
                val bl = row * gridSide + col
                val br = bl + 1
                val tl = bl + gridSide
                val tr = tl + 1

                // Source rect (from UV coords)
                val srcLeft = result.texCoords[bl * 2] * source.width
                val srcTop = result.texCoords[bl * 2 + 1] * source.height
                val srcRight = result.texCoords[tr * 2] * source.width
                val srcBottom = result.texCoords[tr * 2 + 1] * source.height
                val srcRect = android.graphics.RectF(srcLeft, srcTop, srcRight, srcBottom)

                // Destination quad
                val path = android.graphics.Path()
                path.moveTo(pixelPositions[bl * 2], pixelPositions[bl * 2 + 1])
                path.lineTo(pixelPositions[tl * 2], pixelPositions[tl * 2 + 1])
                path.lineTo(pixelPositions[tr * 2], pixelPositions[tr * 2 + 1])
                path.lineTo(pixelPositions[br * 2], pixelPositions[br * 2 + 1])
                path.close()

                canvas.save()
                canvas.clipPath(path)
                canvas.drawBitmap(
                    source,
                    android.graphics.Rect(
                        srcLeft.toInt(), srcTop.toInt(),
                        srcRight.toInt(), srcBottom.toInt()
                    ),
                    android.graphics.RectF(
                        pixelPositions[bl * 2], pixelPositions[bl * 2 + 1],
                        pixelPositions[tr * 2], pixelPositions[tr * 2 + 1]
                    ),
                    paint
                )
                canvas.restore()
            }
        }
    }
}
