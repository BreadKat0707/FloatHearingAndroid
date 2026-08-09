package cn.lemondrop.fhreborn.ui.components

// 基于 top.yukonga.miuix.kmp.basic.ColorPalette（Apache-2.0）改造：
// 去掉 alpha 通道（删除了透明度滑杆与 alpha 状态），选色始终输出不透明颜色。

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import top.yukonga.miuix.kmp.color.api.toHsv
import top.yukonga.miuix.kmp.color.space.Hsv
import top.yukonga.miuix.kmp.squircle.squircleClip
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 无 alpha 通道的 HSV 网格调色板（基于 miuix ColorPalette 改造）。
 * 点击/拖动网格选色，输出始终为不透明颜色。
 *
 * @param color 当前颜色（用于定位指示器）
 * @param onColorChanged 选色回调（不透明）
 * @param modifier 样式
 * @param rows 网格行数
 * @param hueColumns 色相列数
 * @param includeGrayColumn 是否包含灰色列
 * @param showPreview 是否显示预览条
 * @param cornerRadius 圆角
 * @param indicatorRadius 指示器半径
 */
@Composable
fun FhColorPalette(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
    rows: Int = 7,
    hueColumns: Int = 12,
    includeGrayColumn: Boolean = true,
    showPreview: Boolean = true,
    cornerRadius: Dp = 16.dp,
    indicatorRadius: Dp = 10.dp,
) {
    val totalColumns = hueColumns + if (includeGrayColumn) 1 else 0

    val rowSV = remember(rows) { buildRowSV(rows) }
    val grayV = remember(rows) { buildGrayV(rows) }

    var selectedRow by remember { mutableIntStateOf(0) }
    var selectedCol by remember { mutableIntStateOf(0) }

    LaunchedEffect(color, rows, hueColumns, includeGrayColumn) {
        val hsvInit = color.toHsv()
        val h = hsvInit.h
        val s = hsvInit.s / 100f
        val v = hsvInit.v / 100f

        val isGray = includeGrayColumn && s < 0.05f
        val col = if (isGray) {
            totalColumns - 1
        } else {
            val k = (h % 360f) / 360f * hueColumns
            k.roundToInt().coerceIn(0, hueColumns - 1)
        }
        val row = if (isGray) {
            indexOfNearestGrayV(v, grayV)
        } else {
            indexOfNearestRowSV(s, v, rowSV)
        }
        selectedCol = col
        selectedRow = row
    }

    val onColorChangedState = rememberUpdatedState(onColorChanged)

    Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        // 颜色预览（始终不透明）
        if (showPreview) {
            val previewColor = cellColor(selectedCol, selectedRow, rowSV, grayV, hueColumns, includeGrayColumn)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .squircleClip(13.dp)
                    .background(previewColor),
            )
        }

        PaletteCanvas(
            rows = rows,
            hueColumns = hueColumns,
            includeGrayColumn = includeGrayColumn,
            cornerRadius = cornerRadius,
            indicatorRadius = indicatorRadius,
            selectedRow = selectedRow,
            selectedCol = selectedCol,
            rowSV = rowSV,
            grayV = grayV,
            onSelect = { r, c ->
                selectedRow = r
                selectedCol = c
                // 无 alpha：直接输出不透明色
                onColorChangedState.value(cellColor(c, r, rowSV, grayV, hueColumns, includeGrayColumn))
            },
        )
    }
}

@Composable
private fun PaletteCanvas(
    rows: Int,
    hueColumns: Int,
    includeGrayColumn: Boolean,
    cornerRadius: Dp,
    indicatorRadius: Dp,
    selectedRow: Int,
    selectedCol: Int,
    rowSV: List<Pair<Float, Float>>,
    grayV: List<Float>,
    onSelect: (row: Int, col: Int) -> Unit,
) {
    val onSelectState = rememberUpdatedState(onSelect)
    val totalColumns = hueColumns + if (includeGrayColumn) 1 else 0
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .squircleClip(cornerRadius)
            .onGloballyPositioned { sizePx = it.size }
            .pointerInput(rows, hueColumns, includeGrayColumn, isRtl) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (sizePx.width == 0 || sizePx.height == 0) return@awaitEachGesture
                    val (r0, c0) = pointToCell(down.position, sizePx, rows, totalColumns, isRtl)
                    onSelectState.value(r0, c0)

                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == pointerId } ?: event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val (r, c) = pointToCell(change.position, sizePx, rows, totalColumns, isRtl)
                        onSelectState.value(r, c)
                        change.consume()
                    }
                }
            }
            .fillMaxWidth()
            .height(180.dp),
    ) {
        PaletteGrid(
            rows = rows,
            hueColumns = hueColumns,
            includeGrayColumn = includeGrayColumn,
            rowSV = rowSV,
            grayV = grayV,
            isRtl = isRtl,
        )

        if (sizePx.width > 0 && sizePx.height > 0) {
            val w = sizePx.width
            val h = sizePx.height
            val colEdges = IntArray(totalColumns + 1) { i -> (i * w) / totalColumns }
            val rowEdges = IntArray(rows + 1) { i -> (i * h) / rows }
            val start = colEdges[selectedCol]
            val end = colEdges[selectedCol + 1]
            val top = rowEdges[selectedRow]
            val bottom = rowEdges[selectedRow + 1]
            val cxPx = (start + end) / 2f
            val cyPx = (top + bottom) / 2f

            val indicatorSize = indicatorRadius * 2
            val density = LocalDensity.current
            val halfIndicatorPx = with(density) { (indicatorSize / 2).roundToPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (cxPx - halfIndicatorPx).toInt(),
                            y = (cyPx - halfIndicatorPx).toInt(),
                        )
                    }
                    .size(indicatorSize)
                    .drawWithCache {
                        val strokeWidth = 6.dp.toPx()
                        val halfStroke = strokeWidth / 2f
                        val glowSpread = 2.dp.toPx()
                        val glowColor = Color.Black.copy(alpha = 0.25f)

                        val ringCenterRadius = (size.minDimension / 2f) - halfStroke
                        val gradientRadius = ringCenterRadius + halfStroke + glowSpread

                        val glowBrush = Brush.radialGradient(
                            colorStops = listOf(
                                ((ringCenterRadius - halfStroke - glowSpread).coerceAtLeast(0f) / gradientRadius) to Color.Transparent,
                                ((ringCenterRadius - halfStroke) / gradientRadius) to glowColor,
                                ((ringCenterRadius + halfStroke) / gradientRadius) to glowColor,
                                ((ringCenterRadius + halfStroke + glowSpread) / gradientRadius) to Color.Transparent,
                            ).toTypedArray(),
                            radius = gradientRadius,
                        )

                        onDrawBehind {
                            drawCircle(
                                brush = glowBrush,
                                radius = gradientRadius,
                            )

                            drawCircle(
                                color = Color.White,
                                radius = ringCenterRadius,
                                style = Stroke(width = strokeWidth),
                            )
                        }
                    },
            )
        }
    }
}

@Composable
private fun PaletteGrid(
    rows: Int,
    hueColumns: Int,
    includeGrayColumn: Boolean,
    rowSV: List<Pair<Float, Float>>,
    grayV: List<Float>,
    isRtl: Boolean,
) {
    val totalColumns = hueColumns + if (includeGrayColumn) 1 else 0
    val precomputedColors = remember(rows, hueColumns, includeGrayColumn) {
        Array(rows) { r ->
            Array(totalColumns) { c ->
                cellColor(c, r, rowSV, grayV, hueColumns, includeGrayColumn)
            }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width.toInt()
        val h = size.height.toInt()

        val colEdges = IntArray(totalColumns + 1) { i -> (i * w) / totalColumns }
        val rowEdges = IntArray(rows + 1) { i -> (i * h) / rows }

        for (r in 0 until rows) {
            val top = rowEdges[r].toFloat()
            val bottom = rowEdges[r + 1].toFloat()
            val cellH = bottom - top
            for (c in 0 until totalColumns) {
                val start = colEdges[c].toFloat()
                val end = colEdges[c + 1].toFloat()
                val cellW = end - start
                val color = precomputedColors[r][c]
                val left = if (isRtl) size.width - end else start
                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(cellW, cellH),
                )
            }
        }
    }
}

private fun buildRowSV(rows: Int): List<Pair<Float, Float>> {
    if (rows <= 1) return listOf(1f to 1f)

    if (rows == 7) {
        val sArr = floatArrayOf(0.10f, 0.35f, 0.70f, 1.00f, 1.00f, 1.00f, 1.00f)
        val vArr = floatArrayOf(1.00f, 1.00f, 1.00f, 0.85f, 0.65f, 0.45f, 0.20f)
        return List(7) { i -> sArr[i] to vArr[i] }
    }

    val topBrightCut = minOf(0.34f, 2f / (rows - 1f))
    return List(rows) { i ->
        val t = i / (rows - 1f)
        val sRamp = (t / 0.35f).coerceIn(0f, 1f)
        val s = (0.10f + 0.90f * sRamp).coerceIn(0f, 1f)
        val v = if (t <= topBrightCut) {
            1f
        } else {
            val k = ((t - topBrightCut) / (1f - topBrightCut)).coerceIn(0f, 1f)
            lerp(1f, 0.20f, k)
        }
        s to v
    }
}

private fun buildGrayV(rows: Int): List<Float> {
    if (rows <= 1) return listOf(1f)
    return List(rows) { i -> 1f - (i / (rows - 1f)) }
}

private fun cellColor(
    col: Int,
    row: Int,
    rowSV: List<Pair<Float, Float>>,
    grayV: List<Float>,
    hueColumns: Int,
    includeGrayColumn: Boolean,
): Color {
    val totalColumns = hueColumns + if (includeGrayColumn) 1 else 0
    val (s, v) = rowSV[row]
    return if (includeGrayColumn && col == totalColumns - 1) {
        Hsv(0f, 0f, grayV[row] * 100f).toColor()
    } else {
        val step = 360f / hueColumns
        val h = (col * step) % 360f
        Hsv(h, s * 100f, v * 100f).toColor()
    }
}

private fun pointToCell(pos: Offset, size: IntSize, rows: Int, totalColumns: Int, isRtl: Boolean): Pair<Int, Int> {
    val x = pos.x.coerceIn(0f, size.width.toFloat() - 1)
    val y = pos.y.coerceIn(0f, size.height.toFloat() - 1)
    var col = ((x / size.width) * totalColumns).toInt().coerceIn(0, totalColumns - 1)
    if (isRtl) col = totalColumns - 1 - col
    val row = ((y / size.height) * rows).toInt().coerceIn(0, rows - 1)
    return row to col
}

private fun indexOfNearestGrayV(targetV: Float, grayV: List<Float>): Int {
    var idx = 0
    var minVal = Float.POSITIVE_INFINITY
    var i = 0
    val n = grayV.size
    while (i < n) {
        val diff = targetV - grayV[i]
        val v = diff * diff
        if (v < minVal) {
            minVal = v
            idx = i
        }
        i++
    }
    return idx
}

private fun indexOfNearestRowSV(targetS: Float, targetV: Float, rowSV: List<Pair<Float, Float>>): Int {
    var idx = 0
    var minVal = Float.POSITIVE_INFINITY
    var i = 0
    val n = rowSV.size
    while (i < n) {
        val s = rowSV[i].first
        val v = rowSV[i].second
        val ds = targetS - s
        val dv = targetV - v
        val d = ds * ds + dv * dv
        if (d < minVal) {
            minVal = d
            idx = i
        }
        i++
    }
    return idx
}
