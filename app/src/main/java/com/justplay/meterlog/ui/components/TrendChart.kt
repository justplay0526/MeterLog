package com.justplay.meterlog.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justplay.meterlog.data.ReadingWithUsage
import com.justplay.meterlog.data.toLocalDate
import com.justplay.meterlog.ui.format.formatNumber
import java.time.format.DateTimeFormatter
import kotlin.math.floor

@Composable
fun TrendChart(readings: List<ReadingWithUsage>, modifier: Modifier = Modifier) {
    val monthFormatter = DateTimeFormatter.ofPattern("M月")
    val usagePoints = readings
        .reversed()
        .mapNotNull { reading ->
            reading.usageFromPrevious?.let { usage ->
                UsageChartPoint(
                    value = usage,
                    monthLabel = reading.reading.recordedAt?.toLocalDate()?.format(monthFormatter) ?: "-"
                )
            }
        }
    val validUsagePoints = usagePoints.filter { it.value >= 0.0 }

    if (validUsagePoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("至少需要一筆有效用量才能顯示圖表", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(184.dp)
    ) {
        val labelHeight = 28.dp.toPx()
        val plotTop = 16.dp.toPx()
        val plotBottom = size.height - labelHeight
        val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)
        val edgePadding = 14.dp.toPx()
        val axisLabelWidth = 42.dp.toPx()
        val plotLeft = axisLabelWidth + 6.dp.toPx()
        val plotRight = size.width - edgePadding
        val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)

        val values = validUsagePoints.map { it.value }
        val axis = calculateUsageAxis(values)
        val axisMin = axis.min
        val axisMax = axis.max
        val range = axisMax - axisMin
        val stepX = if (usagePoints.size == 1) 0f else plotWidth / usagePoints.lastIndex

        val axisPaint = Paint().apply {
            color = labelColor.toArgb()
            textAlign = Paint.Align.RIGHT
            textSize = 10.sp.toPx()
            isAntiAlias = true
        }

        axis.values.forEach { axisValue ->
            val y = plotBottom - (((axisValue - axisMin) / range).toFloat() * plotHeight)
            drawLine(
                color = guideColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx()
            )
            drawIntoCanvas { canvas ->
                val centeredTextY = y - (axisPaint.descent() + axisPaint.ascent()) / 2f
                canvas.nativeCanvas.drawText(
                    axisValue.formatNumber(),
                    axisLabelWidth,
                    centeredTextY,
                    axisPaint
                )
            }
        }

        val points = usagePoints.mapIndexed { index, point ->
            if (point.value < 0.0) {
                null
            } else {
                val x = if (usagePoints.size == 1) size.width / 2f else plotLeft + stepX * index
                val y = plotBottom - (((point.value - axisMin) / range).toFloat() * plotHeight)
                Offset(x, y)
            }
        }
        points.filterNotNull().forEach { point ->
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = point)
        }
        points.fold(mutableListOf<Offset>()) { segment, point ->
            if (point == null) {
                drawLineSegment(segment, lineColor)
                segment.clear()
            } else {
                segment += point
            }
            segment
        }.also { segment ->
            drawLineSegment(segment, lineColor)
        }

        val labelPaint = Paint().apply {
            color = labelColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = 10.sp.toPx()
            isAntiAlias = true
        }
        drawIntoCanvas { canvas ->
            val labelY = size.height - 6.dp.toPx()
            usagePoints.forEachIndexed { index, point ->
                val x = if (usagePoints.size == 1) size.width / 2f else plotLeft + stepX * index
                canvas.nativeCanvas.drawText(point.monthLabel, x, labelY, labelPaint)
            }
        }
    }
}

private data class UsageChartPoint(
    val value: Double,
    val monthLabel: String
)

private data class ChartAxis(
    val min: Double,
    val max: Double,
    val values: List<Double>
)

private const val AxisTickCount = 6

private fun calculateUsageAxis(values: List<Double>): ChartAxis {
    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: 0.0
    var magnitude = 1.0
    var stepIndex = 0

    while (true) {
        val step = when (stepIndex % 5) {
            0 -> 5.0 * magnitude
            1 -> 10.0 * magnitude
            2 -> 25.0 * magnitude
            3 -> 50.0 * magnitude
            else -> 100.0 * magnitude
        }
        val axisMin = floor(minValue / step) * step
        val axisMax = axisMin + step * (AxisTickCount - 1)

        if (axisMax >= maxValue) {
            val axisValues = (0 until AxisTickCount).map { index -> axisMax - step * index }
            return ChartAxis(
                min = axisMin,
                max = axisMax,
                values = axisValues
            )
        }

        stepIndex += 1
        if (stepIndex % 5 == 0) {
            magnitude *= 100
        }
    }
}

private fun DrawScope.drawLineSegment(
    points: List<Offset>,
    lineColor: Color
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )
}
