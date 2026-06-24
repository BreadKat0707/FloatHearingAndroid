package cn.lemondrop.fhreborn.ui.screens.statistics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

data class ChartPoint(
    val label: String,
    val valueMs: Long
)

@Composable
fun PlayDurationLineChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val titleStyle = MaterialTheme.typography.labelMedium.copy(color = labelColor)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(16.dp)
    ) {
        if (data.isEmpty()) {
            drawNoData(textMeasurer, labelColor, size.width / 2, size.height / 2)
            return@Canvas
        }

        val leftPad = 40.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val topPad = 16.dp.toPx()
        val rightPad = 12.dp.toPx()

        val plotWidth = size.width - leftPad - rightPad
        val plotHeight = size.height - topPad - bottomPad

        val maxValue = data.maxOf { it.valueMs }.coerceAtLeast(1)
        val (unitLabel, unitDivisor) = when {
            maxValue >= 3_600_000L -> "小时" to 3_600_000.0
            maxValue >= 60_000L -> "分钟" to 60_000.0
            else -> "秒" to 1_000.0
        }
        val maxUnit = maxValue / unitDivisor

        val gridLines = 4
        for (i in 0..gridLines) {
            val ratio = i / gridLines.toFloat()
            val y = topPad + plotHeight * (1 - ratio)
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(size.width - rightPad, y),
                strokeWidth = 1.dp.toPx()
            )
            val valueText = String.format("%.1f", maxUnit * ratio)
            drawText(
                textMeasurer = textMeasurer,
                text = valueText,
                topLeft = Offset(0f, y - 8.dp.toPx()),
                style = textStyle
            )
        }

        val stepX = if (data.size > 1) plotWidth / (data.size - 1) else 0f
        val points = data.mapIndexed { index, point ->
            val x = leftPad + index * stepX
            val y = if (maxValue > 0) {
                topPad + plotHeight * (1 - point.valueMs / maxValue.toFloat())
            } else {
                topPad + plotHeight
            }
            ChartOffset(x, y, point)
        }

        val linePath = Path().apply {
            points.forEachIndexed { i, p ->
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, topPad + plotHeight)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, topPad + plotHeight)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                startY = topPad,
                endY = topPad + plotHeight
            )
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEach { p ->
            drawCircle(
                color = pointColor,
                radius = 4.dp.toPx(),
                center = Offset(p.x, p.y)
            )
        }

        val labelSkip = max(1, data.size / 6)
        points.forEachIndexed { index, p ->
            if (index % labelSkip == 0) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = p.point.label,
                    topLeft = Offset(p.x - 10.dp.toPx(), topPad + plotHeight + 6.dp.toPx()),
                    style = textStyle
                )
            }
        }

        if (title != null) {
            drawText(
                textMeasurer = textMeasurer,
                text = title,
                topLeft = Offset(leftPad, 4.dp.toPx()),
                style = titleStyle
            )
        }
    }
}

private data class ChartOffset(
    val x: Float,
    val y: Float,
    val point: ChartPoint
)

private fun DrawScope.drawNoData(
    textMeasurer: TextMeasurer,
    color: Color,
    centerX: Float,
    centerY: Float
) {
    val textLayout = textMeasurer.measure("暂无数据", TextStyle(color = color))
    drawText(
        textMeasurer = textMeasurer,
        text = "暂无数据",
        topLeft = Offset(
            centerX - textLayout.size.width / 2,
            centerY - textLayout.size.height / 2
        ),
        style = TextStyle(color = color)
    )
}

fun formatDurationShort(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
