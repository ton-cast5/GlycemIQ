package com.glycemiq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glycemiq.domain.model.ChartDataPoint
import com.glycemiq.ui.theme.DividerColor

@Composable
fun GlucoseChart(
    dataPoints: List<ChartDataPoint>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    val scrollState = rememberScrollState()
    val barWidth = 48.dp
    val chartHeight = 220.dp
    val minValue = 40f
    val maxValue = maxOf(dataPoints.maxOf { it.value } + 20f, 140f)

    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight + 60.dp)
                .horizontalScroll(scrollState)
        ) {
            val totalWidth = (dataPoints.size * 64).coerceAtLeast(300)
            Canvas(
                modifier = Modifier
                    .width(totalWidth.dp)
                    .height(chartHeight + 60.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 40f
                val barSpacing = canvasWidth / dataPoints.size

                val normalY = canvasHeight - ((90f - minValue) / (maxValue - minValue)) * canvasHeight
                val lowY = canvasHeight - ((70f - minValue) / (maxValue - minValue)) * canvasHeight

                drawLine(
                    color = DividerColor,
                    start = Offset(0f, normalY),
                    end = Offset(canvasWidth, normalY),
                    strokeWidth = 1f
                )
                drawLine(
                    color = DividerColor,
                    start = Offset(0f, lowY),
                    end = Offset(canvasWidth, lowY),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                dataPoints.forEachIndexed { index, point ->
                    val x = barSpacing * index + barSpacing / 2
                    val barHeight = ((point.value - minValue) / (maxValue - minValue)) * canvasHeight
                    val color = glucoseLevelColor(point.level)

                    drawLine(
                        color = color,
                        start = Offset(x, canvasHeight),
                        end = Offset(x, canvasHeight - barHeight),
                        strokeWidth = barWidth.toPx() * 0.6f,
                        cap = StrokeCap.Round
                    )

                    if (index > 0) {
                        val prevPoint = dataPoints[index - 1]
                        val prevX = barSpacing * (index - 1) + barSpacing / 2
                        val prevHeight = ((prevPoint.value - minValue) / (maxValue - minValue)) * canvasHeight
                        val path = Path().apply {
                            moveTo(prevX, canvasHeight - prevHeight)
                            lineTo(x, canvasHeight - barHeight)
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .width(totalWidth.dp)
                    .padding(top = chartHeight + 12.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dataPoints.forEach { point ->
                    Text(
                        text = point.label.take(8),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(56.dp),
                        maxLines = 2
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("Normal", glucoseLevelColor(com.glycemiq.domain.model.GlucoseLevel.NORMAL))
            LegendItem("Moderado", glucoseLevelColor(com.glycemiq.domain.model.GlucoseLevel.HIGH))
            LegendItem("Crítico", glucoseLevelColor(com.glycemiq.domain.model.GlucoseLevel.LOW))
        }
    }
}

@Composable
private fun LegendItem(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.width(16.dp).height(16.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
