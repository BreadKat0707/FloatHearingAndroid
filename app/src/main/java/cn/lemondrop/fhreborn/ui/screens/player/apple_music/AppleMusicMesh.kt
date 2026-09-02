package cn.lemondrop.fhreborn.ui.screens.player.apple_music

import kotlin.math.min
import kotlin.random.Random

/**
 * Generates deformable mesh grids for the Apple Music inspired background.
 * Ported from Lyricify-Backgrounds AppleMusicInspiredMesh.cs (Apache 2.0).
 *
 * Produces vertex position arrays for CPU-side Canvas mesh rendering.
 */
object AppleMusicMesh {

    const val PRESET_SLOT_COUNT = 5
    private const val MINIMUM_CONTROL_POINT_COUNT = 2
    private const val MAXIMUM_CONTROL_POINT_COUNT = 16
    private const val MAXIMUM_SUBDIVISION_LEVELS = 4
    private const val DEFAULT_PORTRAIT_CONTROL_POINT_COUNT = 6
    private const val DEFAULT_PORTRAIT_SUBDIVISION_LEVELS = 2
    private const val DEFAULT_LANDSCAPE_CONTROL_POINT_COUNT = 9
    private const val DEFAULT_LANDSCAPE_SUBDIVISION_LEVELS = 2

    fun selectPreset(): Int = resolvePortraitPreset(Random.nextInt(PRESET_SLOT_COUNT))

    fun resolvePortraitPreset(presetSlot: Int): Int = when {
        presetSlot <= 0 -> 0
        presetSlot == 1 -> 1
        presetSlot <= 3 -> 2
        else -> 3
    }

    fun selectLandscapePreset(): Int = Random.nextInt(PRESET_SLOT_COUNT)

    /**
     * Result of mesh generation containing vertex positions, UVs, and triangle indices.
     * [vertexPositions] is flat float array: [x0,y0, x1,y1, ...] in NDC [-1,1].
     * [texCoords] is flat float array: [u0,v0, u1,v1, ...] in [0,1].
     * [indices] is short array of triangle indices.
     */
    data class MeshResult(
        val vertexPositions: FloatArray,
        val texCoords: FloatArray,
        val indices: ShortArray,
        val gridSide: Int
    )

    fun create(
        presetIndex: Int,
        isVerticalLayout: Boolean,
        controlPointCount: Int? = null,
        subdivisionLevels: Int? = null
    ): MeshResult {
        val presets = if (isVerticalLayout) PORTRAIT_PRESETS else LANDSCAPE_PRESETS
        val clampedPresetIndex = presetIndex.coerceIn(0, presets.size - 1)
        val preset = presets[clampedPresetIndex]

        val sourceSide = if (isVerticalLayout)
            DEFAULT_PORTRAIT_CONTROL_POINT_COUNT
        else
            DEFAULT_LANDSCAPE_CONTROL_POINT_COUNT

        val defaultSubdiv = if (isVerticalLayout)
            DEFAULT_PORTRAIT_SUBDIVISION_LEVELS
        else
            DEFAULT_LANDSCAPE_SUBDIVISION_LEVELS

        val targetSide = (controlPointCount ?: sourceSide)
            .coerceIn(MINIMUM_CONTROL_POINT_COUNT, MAXIMUM_CONTROL_POINT_COUNT)
        val subdivLevels = (subdivisionLevels ?: defaultSubdiv)
            .coerceIn(0, MAXIMUM_SUBDIVISION_LEVELS)

        var from = preset.from.copyOf()
        var to = preset.to.copyOf()

        if (targetSide != sourceSide) {
            from = resampleGrid(from, sourceSide, targetSide)
            to = resampleGrid(to, sourceSide, targetSide)
        }

        for (level in 0 until subdivLevels) {
            from = subdivide(from, targetSide)
            to = subdivide(to, targetSide)
            // After subdivision, the side length doubles minus 1
        }

        // Calculate actual grid side after subdivisions
        var side = targetSide
        for (level in 0 until subdivLevels) {
            side = side * 2 - 1
        }

        return buildMeshResult(from, side)
    }

    private fun buildMeshResult(positions: FloatArray, side: Int): MeshResult {
        // positions is flat [x0,y0, x1,y1, ...] of side*side pairs
        val vertexCount = side * side
        val vertexPositions = FloatArray(vertexCount * 2)
        val texCoords = FloatArray(vertexCount * 2)

        for (row in 0 until side) {
            val v = 1f - row.toFloat() / (side - 1).coerceAtLeast(1)
            for (col in 0 until side) {
                val u = col.toFloat() / (side - 1).coerceAtLeast(1)
                val idx = row * side + col
                // Convert [0,1] to [-1,1] NDC
                vertexPositions[idx * 2] = positions[idx * 2] * 2f - 1f
                vertexPositions[idx * 2 + 1] = positions[idx * 2 + 1] * 2f - 1f
                texCoords[idx * 2] = u
                texCoords[idx * 2 + 1] = v
            }
        }

        val indexCount = (side - 1) * (side - 1) * 6
        val indices = ShortArray(indexCount)
        var index = 0
        for (row in 0 until side - 1) {
            for (col in 0 until side - 1) {
                val bl = (row * side + col).toShort()
                val br = (bl + 1).toShort()
                val tl = (bl + side).toShort()
                val tr = (tl + 1).toShort()
                indices[index++] = bl
                indices[index++] = tl
                indices[index++] = tr
                indices[index++] = tr
                indices[index++] = br
                indices[index++] = bl
            }
        }

        return MeshResult(vertexPositions, texCoords, indices, side)
    }

    // --- Grid operations ---

    private fun resampleGrid(source: FloatArray, sourceSide: Int, targetSide: Int): FloatArray {
        val result = FloatArray(targetSide * targetSide * 2)
        val denominator = targetSide - 1f

        for (row in 0 until targetSide) {
            val sourceY = row * (sourceSide - 1f) / denominator
            val firstRow = min(sourceY.toInt(), sourceSide - 1)
            val secondRow = min(firstRow + 1, sourceSide - 1)
            val rowMix = sourceY - firstRow

            for (col in 0 until targetSide) {
                val sourceX = col * (sourceSide - 1f) / denominator
                val firstCol = min(sourceX.toInt(), sourceSide - 1)
                val secondCol = min(firstCol + 1, sourceSide - 1)
                val colMix = sourceX - firstCol

                val idx00 = (firstRow * sourceSide + firstCol) * 2
                val idx01 = (firstRow * sourceSide + secondCol) * 2
                val idx10 = (secondRow * sourceSide + firstCol) * 2
                val idx11 = (secondRow * sourceSide + secondCol) * 2

                val outIdx = (row * targetSide + col) * 2
                for (c in 0..1) {
                    val first = lerp(source[idx00 + c], source[idx01 + c], colMix)
                    val second = lerp(source[idx10 + c], source[idx11 + c], colMix)
                    result[outIdx + c] = lerp(first, second, rowMix)
                }
            }
        }
        return result
    }

    /**
     * Catmull-Clark subdivision step on a structured grid.
     * Input side is N, output side is 2N-1.
     */
    private fun subdivide(source: FloatArray, side: Int): FloatArray {
        val newSide = side * 2 - 1
        val result = FloatArray(newSide * newSide * 2)

        // Face centers
        val faceCenters = FloatArray((side - 1) * (side - 1) * 2)
        for (row in 0 until side - 1) {
            for (col in 0 until side - 1) {
                val fcIdx = (row * (side - 1) + col) * 2
                val p00 = (row * side + col) * 2
                val p01 = (row * side + col + 1) * 2
                val p10 = ((row + 1) * side + col) * 2
                val p11 = ((row + 1) * side + col + 1) * 2
                for (c in 0..1) {
                    faceCenters[fcIdx + c] = (source[p00 + c] + source[p01 + c] +
                            source[p10 + c] + source[p11 + c]) / 4f
                }
            }
        }

        // Original vertices
        for (row in 0 until side) {
            for (col in 0 until side) {
                val bRow = row == 0 || row == side - 1
                val bCol = col == 0 || col == side - 1
                val outIdx = (row * 2 * newSide + col * 2) * 2
                val srcIdx = (row * side + col) * 2

                when {
                    bRow && bCol -> {
                        for (c in 0..1) result[outIdx + c] = source[srcIdx + c]
                    }
                    bRow -> {
                        for (c in 0..1) {
                            val left = source[(row * side + (col - 1)) * 2 + c]
                            val right = source[(row * side + (col + 1)) * 2 + c]
                            result[outIdx + c] = (left + source[srcIdx + c] * 6f + right) / 8f
                        }
                    }
                    bCol -> {
                        for (c in 0..1) {
                            val up = source[((row - 1) * side + col) * 2 + c]
                            val down = source[((row + 1) * side + col) * 2 + c]
                            result[outIdx + c] = (up + source[srcIdx + c] * 6f + down) / 8f
                        }
                    }
                    else -> {
                        for (c in 0..1) {
                            val faceAvg = (faceCenters[((row - 1) * (side - 1) + col - 1) * 2 + c] +
                                    faceCenters[((row - 1) * (side - 1) + col) * 2 + c] +
                                    faceCenters[(row * (side - 1) + col - 1) * 2 + c] +
                                    faceCenters[(row * (side - 1) + col) * 2 + c]) / 4f
                            val edgeAvg = (
                                (source[srcIdx + c] + source[((row - 1) * side + col) * 2 + c]) / 2f +
                                (source[srcIdx + c] + source[((row + 1) * side + col) * 2 + c]) / 2f +
                                (source[srcIdx + c] + source[(row * side + col - 1) * 2 + c]) / 2f +
                                (source[srcIdx + c] + source[(row * side + col + 1) * 2 + c]) / 2f
                            ) / 4f
                            result[outIdx + c] = (faceAvg + edgeAvg * 2f + source[srcIdx + c]) / 4f
                        }
                    }
                }
            }
        }

        // Horizontal edge midpoints
        for (row in 0 until side) {
            for (col in 0 until side - 1) {
                val outIdx = (row * 2 * newSide + col * 2 + 1) * 2
                for (c in 0..1) {
                    val p0 = (row * side + col) * 2 + c
                    val p1 = (row * side + col + 1) * 2 + c
                    if (row == 0 || row == side - 1) {
                        result[outIdx + c] = (source[p0] + source[p1]) / 2f
                    } else {
                        result[outIdx + c] = (source[p0] + source[p1] +
                                faceCenters[((row - 1) * (side - 1) + col) * 2 + c] +
                                faceCenters[(row * (side - 1) + col) * 2 + c]) / 4f
                    }
                }
            }
        }

        // Vertical edge midpoints
        for (row in 0 until side - 1) {
            for (col in 0 until side) {
                val outIdx = ((row * 2 + 1) * newSide + col * 2) * 2
                for (c in 0..1) {
                    val p0 = (row * side + col) * 2 + c
                    val p1 = ((row + 1) * side + col) * 2 + c
                    if (col == 0 || col == side - 1) {
                        result[outIdx + c] = (source[p0] + source[p1]) / 2f
                    } else {
                        result[outIdx + c] = (source[p0] + source[p1] +
                                faceCenters[(row * (side - 1) + col - 1) * 2 + c] +
                                faceCenters[(row * (side - 1) + col) * 2 + c]) / 4f
                    }
                }
            }
        }

        // Face centers
        for (row in 0 until side - 1) {
            for (col in 0 until side - 1) {
                val outIdx = ((row * 2 + 1) * newSide + col * 2 + 1) * 2
                val fcIdx = (row * (side - 1) + col) * 2
                for (c in 0..1) result[outIdx + c] = faceCenters[fcIdx + c]
            }
        }

        return result
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    // --- Preset data: flat FloatArray of (x,y) pairs ---

    private data class MeshPreset(val from: FloatArray, val to: FloatArray)

    private fun p(vararg values: Float): FloatArray = floatArrayOf(*values)

    // Portrait presets (6x6)
    private val PORTRAIT_PRESETS = listOf(
        MeshPreset(
            from = p(
                0f,0f, 0.2f,0f, 0.4f,0f, 0.6f,0f, 0.8f,0f, 1f,0f,
                0f,0.2f, -0.0933f,0.4f, 0.4f,0.2f, 0.6f,0.2f, 0.3653f,0.1335f, 1f,0.2f,
                0f,0.4f, 0.4232f,0.359f, 0.3429f,0.5349f, 0.6f,0.4f, 0.832f,0.4148f, 1f,0.4f,
                0f,0.6f, 0.2f,0.6f, 0.2293f,0.7775f, 0.7829f,0.5595f, 0.6514f,0.7302f, 1f,0.6f,
                0f,0.8f, 0.2f,0.8f, 0.28f,0.9195f, 0.4773f,0.8f, 0.8f,0.8f, 1f,0.8f,
                0f,1f, 0.6514f,1.1073f, 0.4f,1f, 1f,1.0317f, 1f,1.1302f, 1f,1f
            ),
            to = p(
                0f,0f, 0.2f,0f, 0.4f,0f, 0.6f,0f, 0.8f,0f, 1f,0f,
                0f,0.2f, -0.0933f,0.4f, 0.4f,0.2f, 0.6f,0.2f, 0.8587f,0.2234f, 1f,0.2f,
                0f,0.4f, 0.4526f,0.6053f, 0.3429f,0.5349f, 0.6f,0.4f, 0.832f,0.4148f, 1f,0.4f,
                0f,0.6f, 0.2f,0.6f, 0.2293f,0.7775f, 0.7829f,0.5595f, 0.6514f,0.7302f, 1f,0.6f,
                0f,0.8f, 0.2f,0.8f, 0.28f,0.9195f, 0.4773f,0.8f, 0.8f,0.8f, 1f,0.8f,
                0f,1f, 0.6514f,1.1073f, 0.4f,1f, 1f,1.0317f, 1f,1.1302f, 1f,1f
            )
        ),
        MeshPreset(
            from = p(
                0f,0f, 0.2f,0f, 0.4f,0f, 0.6f,0f, 0.8f,0f, 1f,0f,
                0f,0.2f, 0.3265f,0.3839f, 0.4f,0.2f, 0.462f,0.3424f, 0.683f,0.2797f, 1f,0.2f,
                0f,0.4f, 0.2f,0.4f, 0.4f,0.4f, 0.6f,0.4903f, 0.6574f,0.4903f, 1.1357f,0.4f,
                -0.1173f,0.4597f, 0.3771f,0.4384f, 0.6415f,0.5947f, 0.8254f,0.6935f, 0.9334f,0.5862f, 1f,0.6f,
                -0.0437f,0.6533f, 0.2f,0.6618f, 0.683f,0.7362f, 0.8139f,0.833f, 0.9104f,0.8085f, 1f,0.8f,
                0f,1f, 0.2f,1f, 0.4f,1f, 0.6f,1f, 0.8f,1f, 1f,1f
            ),
            to = p(
                0f,0f, 0.2f,0f, 0.4f,0f, 0.6f,0f, 0.8f,0f, 1f,0f,
                0f,0.2f, 0.2437f,0.4392f, 0.4f,0.2f, 0.462f,0.3424f, 0.683f,0.2797f, 1f,0.2f,
                0f,0.4f, 0.1494f,0.4787f, 0.4f,0.5063f, 0.6966f,0.516f, 0.8139f,0.4478f, 1.1357f,0.4f,
                -0.1173f,0.4597f, 0.2437f,0.6085f, 0.6414f,0.5756f, 0.8254f,0.6935f, 0.9334f,0.5862f, 1f,0.6f,
                -0.0437f,0.6533f, 0.2f,0.6618f, 0.683f,0.7362f, 0.8139f,0.833f, 0.9104f,0.8085f, 1f,0.8f,
                0f,1f, 0.2f,1f, 0.4f,1f, 0.6f,1f, 0.8f,1f, 1f,1f
            )
        ),
        MeshPreset(
            from = p(
                0f,0f, 0.2f,0f, 0.4f,0f, 0.7465f,-0.0935f, 0.9702f,-0.0872f, 1.5935f,-0.0308f,
                -0.1675f,0.2878f, 0.7185f,0.3087f, 0.5952f,0.0728f, 0.7823f,0.0815f, 0.9318f,0.301f, 1.1369f,0.3756f,
                0f,0.4f, 0.3295f,0.4607f, 0.7823f,0.3087f, 0.7465f,0.365f, 0.9514f,0.4305f, 1.1514f,0.4424f,
                0f,0.6f, 0.2f,0.6f, 0.3295f,0.4424f, 0.5703f,0.5f, 0.7887f,0.4847f, 1f,0.6f,
                0f,0.8f, 0.2414f,0.7926f, 0.0418f,0.7303f, 0.5952f,0.4688f, 0.9433f,0.6929f, 1f,0.8f,
                0f,1f, 0.2f,1f, 0.4f,1f, 0.6f,1f, 0.8f,1f, 1f,1f
            ),
            to = p(
                0f,0f, 0.2f,0f, 0.4f,0f, 0.7465f,-0.0935f, 0.9702f,-0.0872f, 1.5935f,-0.0308f,
                -0.1675f,0.2878f, 0.5414f,0.2825f, 0.5952f,0.0728f, 0.7823f,0.0815f, 0.9318f,0.301f, 1.1369f,0.3756f,
                0f,0.4f, 0.2881f,0.4479f, 0.7823f,0.3087f, 0.8363f,0.3661f, 0.9514f,0.4305f, 1.1514f,0.4424f,
                0f,0.6f, 0.177f,0.6f, 0.4f,0.4775f, 0.5703f,0.5f, 0.7887f,0.4847f, 1f,0.6f,
                0f,0.8f, 0.2414f,0.7926f, 0.1499f,0.7324f, 0.5952f,0.5623f, 0.9433f,0.6929f, 1f,0.8f,
                0f,1f, 0.2f,1f, 0.4f,1f, 0.6f,1f, 0.8f,1f, 1f,1f
            )
        ),
        MeshPreset(
            from = p(
                -0.2351f,-0.0967f, 0.2135f,-0.1414f, 0.9221f,-0.0908f, 0.9221f,-0.0685f, 1.3027f,0.0253f, 1.2351f,0.1786f,
                -0.3768f,0.1851f, 0.2f,0.2f, 0.6615f,0.3146f, 0.9543f,0f, 0.6969f,0.1911f, 1f,0.2f,
                0f,0.4f, 0.2f,0.4f, 0.0776f,0.2318f, 0.6f,0.4f, 0.6615f,0.3851f, 1f,0.4f,
                0f,0.6f, 0.1291f,0.6f, 0.4f,0.6f, 0.4f,0.4304f, 0.4264f,0.5792f, 1.2029f,0.8188f,
                -0.1192f,1f, 0.6f,0.8f, 0.4264f,0.8104f, 0.6f,0.8f, 0.8f,0.8f, 1f,0.8f,
                0f,1f, 0.0776f,1.0283f, 0.4f,1f, 0.6f,1f, 0.8f,1f, 1.1868f,1.0283f
            ),
            to = p(
                -0.2351f,-0.0967f, 0.2135f,-0.1414f, 0.9221f,-0.0908f, 0.9221f,-0.0685f, 1.3027f,0.0253f, 1.2351f,0.1786f,
                -0.3768f,0.1851f, 0.1839f,0.2f, 0.7034f,0.2952f, 0.9543f,0f, 0.7775f,0.3339f, 1f,0.2f,
                0f,0.4f, 0.0357f,0.5369f, 0.0776f,0.2318f, 0.6f,0.4f, 0.6615f,0.3851f, 1f,0.4f,
                0f,0.6f, 0.2f,0.6878f, 0.4f,0.6f, 0.5f,0.5896f, 0.6454f,0.6878f, 1.2029f,0.8188f,
                -0.1192f,1f, 0.6193f,0.9027f, 0.4264f,0.8104f, 0.6f,0.8f, 0.8f,0.8f, 1f,0.8f,
                0f,1f, 0.0776f,1.0283f, 0.4f,1f, 0.6f,1f, 0.8f,1f, 1.1868f,1.0283f
            )
        )
    )

    // Landscape presets (9x9, identity as "from")
    private fun identityGrid(n: Int): FloatArray {
        val result = FloatArray(n * n * 2)
        for (row in 0 until n) {
            for (col in 0 until n) {
                val idx = (row * n + col) * 2
                result[idx] = col.toFloat() / (n - 1)
                result[idx + 1] = row.toFloat() / (n - 1)
            }
        }
        return result
    }

    private val LANDSCAPE_IDENTITY = identityGrid(DEFAULT_LANDSCAPE_CONTROL_POINT_COUNT)

    private val LANDSCAPE_PRESETS = listOf(
        MeshPreset(LANDSCAPE_IDENTITY, p(
            -0.2292f,-0.0529f, -0.0402f,-0.127f, 0.1116f,-0.3122f, 0.0923f,-0.336f, 1.1205f,-0.164f, 1.0089f,-0.0635f, 1.1205f,-0.0529f, 1.1116f,-0.0899f, 1.1979f,-0.0741f,
            -0.2202f,0.2685f, 0.0238f,0.1435f, 0.0997f,0.0933f, 0.0774f,0.1091f, 0.75f,0.0933f, 0.7738f,0.1091f, 0.7991f,0.1435f, 1.2798f,0.088f, 1.1801f,0.1435f,
            -0.1667f,0.3611f, 0.0432f,0.25f, 0.1116f,0.25f, 0.0923f,0.2791f, 0.5387f,0.2791f, 0.5908f,0.3161f, 0.625f,0.3161f, 0.9896f,0.2791f, 1.0893f,0.2341f,
            -0.1176f,0.4544f, 0.0625f,0.3909f, 0.1503f,0.4418f, 0.1637f,0.4306f, 0.4836f,0.3909f, 0.6161f,0.4544f, 0.6458f,0.4544f, 0.7411f,0.3909f, 1.064f,0.338f,
            -0.0625f,0.5344f, 0.0997f,0.5159f, 0.2664f,0.5721f, 0.2589f,0.5721f, 0.4836f,0.5344f, 0.7113f,0.5344f, 0.7411f,0.5344f, 0.7991f,0.5159f, 1.0461f,0.5f,
            -0.0402f,0.6574f, 0.375f,0.713f, 0.3929f,0.6574f, 0.375f,0.625f, 0.5f,0.6038f, 0.7991f,0.5721f, 0.808f,0.625f, 0.875f,0.625f, 1.0461f,0.6435f,
            -0.0298f,0.7685f, 0.3586f,0.9041f, 0.4063f,0.8003f, 0.4568f,0.75f, 0.625f,0.6574f, 0.8616f,0.67f, 0.8408f,0.713f, 0.8943f,0.7288f, 1.1265f,0.8214f,
            -0.0402f,0.9041f, 0.2589f,1.0152f, 0.4747f,0.9676f, 0.4568f,0.8882f, 0.7411f,0.8538f, 0.8616f,0.8538f, 0.8408f,0.875f, 0.9211f,0.9438f, 1.1116f,0.9676f,
            -0.0625f,1.0979f, 0.0238f,1.2196f, 0.3304f,1.0688f, 0.375f,1f, 0.7887f,1.0556f, 0.8408f,1.0556f, 0.875f,1.0688f, 0.9435f,1.0688f, 1.0893f,1.2196f
        )),
        MeshPreset(LANDSCAPE_IDENTITY, p(
            -0.1726f,-0.1984f, 0.0551f,-0.2593f, 0.2158f,-0.2593f, 0.3839f,-0.1984f, 0.5119f,-0.1984f, 0.6473f,-0.1243f, 0.744f,-0.2698f, 1.0179f,-0.4259f, 1.2515f,-0.2698f,
            0f,0.0562f, 0.125f,0.1971f, 0.2381f,0.2679f, 0.375f,0.2917f, 0.5f,0.2202f, 0.625f,0.125f, 0.8467f,-0.1111f, 1f,-0.1528f, 1.1042f,0.0146f,
            -0.0193f,0.0357f, 0.125f,0.1766f, 0.25f,0.25f, 0.375f,0.3082f, 0.5f,0.25f, 0.625f,0.1766f, 0.7887f,0.0146f, 0.9598f,-0.0648f, 1.0551f,-0.0172f,
            0f,0.3353f, 0.125f,0.3896f, 0.2167f,0.3444f, 0.375f,0.3231f, 0.5119f,0.3772f, 0.625f,0.3353f, 0.7768f,0.1025f, 0.9464f,0.0562f, 1.0685f,0.1647f,
            -0.0521f,0.5192f, 0.125f,0.4471f, 0.2229f,0.3824f, 0.375f,0.3444f, 0.5119f,0.3933f, 0.6577f,0.4353f, 0.75f,0.4677f, 0.8601f,0.4353f, 1.1473f,0.2345f,
            -0.0402f,0.6442f, 0.125f,0.5192f, 0.2277f,0.4f, 0.375f,0.3664f, 0.5119f,0.4074f, 0.6577f,0.4677f, 0.75f,0.5f, 0.8527f,0.4471f, 1.128f,0.2345f,
            -0.0253f,0.7718f, 0.1116f,0.5675f, 0.2339f,0.4353f, 0.3708f,0.4219f, 0.5148f,0.4353f, 0.6726f,0.5f, 0.7649f,0.578f, 0.8601f,0.5357f, 1.1622f,0.3082f,
            -0.0253f,0.9041f, 0.0982f,0.7718f, 0.2381f,0.6872f, 0.375f,0.6442f, 0.5119f,0.6442f, 0.6577f,0.6872f, 0.8229f,0.7348f, 0.875f,0.875f, 1.2411f,1.1343f,
            -0.0521f,1.1005f, 0.0982f,1.0556f, 0.2277f,1.0556f, 0.3557f,1.0556f, 0.5f,1.1587f, 0.625f,1.1799f, 0.7649f,1.3677f, 0.8958f,1.4841f, 1.0685f,1.3519f
        )),
        MeshPreset(LANDSCAPE_IDENTITY, p(
            -0.064f,-0.1323f, 0.0893f,-0.1614f, 0.25f,-0.0608f, 0.5729f,-0.1614f, 0.6771f,-0.1614f, 0.7292f,-0.1614f, 0.7634f,-0.1217f, 0.875f,-0.0608f, 1.0164f,-0.0423f,
            -0.0714f,0.1091f, 0.2054f,0.0985f, 0.2292f,0.1091f, 0.375f,0.125f, 0.5357f,0.2077f, 0.6131f,0.25f, 0.6458f,0.125f, 0.75f,-0.0284f, 1.0164f,0.1091f,
            -0.0565f,0.25f, 0.1696f,0.2077f, 0.1771f,0.2262f, 0.2143f,0.2262f, 0.375f,0.1759f, 0.625f,0.2937f, 0.6369f,0.3161f, 0.6652f,0.2262f, 1.0104f,0.2262f,
            -0.0565f,0.375f, 0.0997f,0.375f, 0.125f,0.375f, 0.1771f,0.3882f, 0.3616f,0.2077f, 0.6131f,0.3406f, 0.6548f,0.3406f, 0.7068f,0.375f, 1.0313f,0.3538f,
            -0.1429f,0.6058f, 0.1414f,0.5f, 0.1563f,0.5f, 0.1949f,0.5f, 0.5193f,0.2937f, 0.6964f,0.4517f, 0.7158f,0.4517f, 0.7902f,0.4841f, 1.0461f,0.4841f,
            -0.0714f,0.6753f, 0.2054f,0.625f, 0.2054f,0.625f, 0.2292f,0.625f, 0.6131f,0.375f, 0.7158f,0.5298f, 0.7634f,0.5456f, 0.8557f,0.625f, 1.0789f,0.625f,
            -0.0565f,0.8108f, 0.2143f,0.75f, 0.2292f,0.75f, 0.25f,0.7315f, 0.6652f,0.6865f, 0.7634f,0.6462f, 0.8095f,0.7077f, 0.8914f,0.75f, 1.0923f,0.75f,
            -0.0565f,0.9306f, 0.2054f,0.9067f, 0.2054f,0.9306f, 0.2292f,0.9411f, 0.625f,0.875f, 0.7634f,0.7718f, 0.8557f,0.8108f, 0.939f,0.8538f, 1.0789f,0.9306f,
            0f,1f, -0.0714f,1.2169f, 0.125f,1.4021f, 0.25f,1.0794f, 0.625f,1.0794f, 0.7902f,1.0794f, 0.875f,1.0794f, 0.9509f,1.0582f, 1.0104f,1.0794f
        )),
        MeshPreset(LANDSCAPE_IDENTITY, p(
            -0.2292f,-0.3968f, 0.0699f,-0.3439f, 0.2217f,-0.1799f, 0.3512f,-0.1376f, 0.6533f,-0.2407f, 0.6845f,-0.164f, 0.7753f,-0.3148f, 0.9494f,-0.3677f, 1.381f,-0.5476f,
            -0.1711f,0.0827f, -0.0387f,-0.1263f, 0.25f,0.125f, 0.2887f,0.125f, 0.5f,0.125f, 0.5565f,0.125f, 0.7827f,-0.0787f, 0.9182f,-0.1799f, 1.2039f,-0.0628f,
            -0.1057f,0.2209f, 0.0268f,0.1918f, 0.25f,0.25f, 0.2679f,0.2844f, 0.2887f,0.2685f, 0.3958f,0.2844f, 0.6771f,0.125f, 0.9702f,0.0542f, 1.2113f,0.1091f,
            -0.1176f,0.2983f, 0.1101f,0.33f, 0.25f,0.42f, 0.317f,0.42f, 0.3571f,0.42f, 0.3958f,0.42f, 0.6369f,0.2983f, 0.9107f,0.2844f, 1.2113f,0.33f,
            -0.1533f,0.375f, 0.1533f,0.375f, 0.2292f,0.42f, 0.375f,0.5f, 0.4301f,0.5377f, 0.4583f,0.5377f, 0.6845f,0.4735f, 0.811f,0.4735f, 1.1369f,0.463f,
            -0.0938f,0.5728f, 0.1533f,0.4054f, 0.3363f,0.5f, 0.4167f,0.5377f, 0.5f,0.625f, 0.5476f,0.6991f, 0.7887f,0.6118f, 0.8378f,0.588f, 1.1563f,0.5608f,
            -0.0789f,0.75f, 0.25f,0.5608f, 0.3958f,0.5608f, 0.4732f,0.625f, 0.5402f,0.75f, 0.5967f,0.7976f, 0.8839f,0.7361f, 0.8839f,0.7811f, 1.1563f,0.6389f,
            -0.1057f,0.9226f, 0.125f,0.875f, 0.2976f,0.875f, 0.4464f,0.8882f, 0.5908f,0.9041f, 0.625f,0.875f, 0.9702f,0.9041f, 1.0268f,1f, 1.1726f,1.0443f,
            -0.0387f,1.1138f, 0.0878f,1.1349f, 0.2292f,1.1138f, 0.4301f,1.1852f, 0.625f,1.2804f, 0.625f,1.3254f, 1.0372f,1.2328f, 0.9568f,1.2328f, 1.0938f,1.2328f
        )),
        MeshPreset(LANDSCAPE_IDENTITY, p(
            -0.0952f,-0.1561f, 0.0997f,-0.1561f, 0.2396f,-0.0847f, 0.3586f,-0.0608f, 0.4926f,-0.0608f, 0.6086f,-0.1561f, 0.7426f,-0.1772f, 0.8616f,-0.1561f, 1.064f,-0.2275f,
            -0.0521f,0.0933f, -0.0521f,0.3062f, 0.2708f,0.375f, 0.375f,0.3485f, 0.4926f,0.3062f, 0.625f,0.2335f, 0.75f,0.125f, 0.875f,-0.0337f, 1.064f,-0.1772f,
            -0.125f,0.3611f, 0.0789f,0.5f, 0.2827f,0.4147f, 0.3824f,0.375f, 0.4926f,0.33f, 0.625f,0.25f, 0.75f,0.1812f, 0.8988f,0.0146f, 1.0804f,-0.123f,
            -0.0952f,0.5119f, 0.1518f,0.5344f, 0.2827f,0.4683f, 0.3824f,0.4147f, 0.4926f,0.375f, 0.625f,0.2851f, 0.7589f,0.2163f, 0.8988f,0.2335f, 1.119f,0.4147f,
            -0.0521f,0.625f, 0.2827f,0.5344f, 0.2961f,0.5985f, 0.3646f,0.5119f, 0.4926f,0.3995f, 0.6324f,0.3062f, 0.7589f,0.2619f, 0.9286f,0.3062f, 1.1071f,0.4683f,
            -0.1399f,0.6938f, 0.2902f,0.5721f, 0.2604f,0.6759f, 0.3586f,0.5344f, 0.5f,0.4286f, 0.6414f,0.33f, 0.8006f,0.3062f, 0.9673f,0.375f, 1.119f,0.5985f,
            -0.0521f,0.7897f, 0.192f,0.8294f, 0.25f,0.75f, 0.2708f,0.6759f, 0.5f,0.5119f, 0.7738f,0.5589f, 0.8988f,0.7315f, 0.9286f,0.713f, 1.119f,0.75f,
            -0.0685f,0.9438f, 0.1518f,0.9438f, 0.1979f,0.875f, 0.25f,0.957f, 0.5f,0.7718f, 0.7738f,0.7315f, 0.936f,0.9755f, 1f,1.0205f, 1.1726f,0.957f,
            -0.0283f,1.0873f, 0.0997f,1.1561f, 0.125f,1.1799f, 0.2604f,1.1243f, 0.4926f,1.0688f, 0.8006f,1.0873f, 0.9167f,1.1376f, 1.0313f,1.2698f, 1.1548f,1.3228f
        ))
    )
}
