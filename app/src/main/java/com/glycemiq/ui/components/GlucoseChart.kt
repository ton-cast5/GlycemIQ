package com.glycemiq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glycemiq.domain.model.ChartDataPoint
import com.glycemiq.domain.model.GlucoseLevel
import com.glycemiq.ui.theme.ClinicalBlue
import com.glycemiq.ui.theme.DividerColor

@Composable
fun GlucoseChart(
    dataPoints: List<ChartDataPoint>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    val scrollState = rememberScrollState()
    val chartHeight = 200.dp
    val minValue = 40f
    val maxValue = maxOf(dataPoints.maxOf { it.value } + 30f, 150f)
    val gridColor = DividerColor.copy(alpha = 0.5f)
    val lineColor = ClinicalBlue.copy(alpha = 0.7f)
    val areaGradient = Brush.verticalGradient(
        colors = listOf(ClinicalBlue.copy(alpha = 0.25f), Color.Transparent)
    )
    var selectedIndex by remember(dataPoints) { mutableIntStateOf(-1) }
    val activeIndex = selectedIndex.coerceIn(0, dataPoints.lastIndex)
    val selectedPoint = dataPoints.getOrNull(activeIndex)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("Normal", glucoseLevelColor(GlucoseLevel.NORMAL))
            LegendItem("Moderado", glucoseLevelColor(GlucoseLevel.HIGH))
            LegendItem("Crítico", glucoseLevelColor(GlucoseLevel.LOW))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedPoint != null) {
                        glucoseLevelContainerColor(selectedPoint.level)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        text = selectedPoint?.let {
                            "${it.value.toInt()} mg/dL · ${it.label} · ${it.level.label}"
                        } ?: "Toca o desliza sobre una barra para ver el valor",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selectedPoint != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = selectedPoint?.let { glucoseLevelColor(it.level) }
                            ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight + 48.dp)
                        .horizontalScroll(scrollState)
                ) {
                    val pointWidth = 72
                    val totalWidth = (dataPoints.size * pointWidth).coerceAtLeast(280)

                    Canvas(
                        modifier = Modifier
                            .width(totalWidth.dp)
                            .height(chartHeight + 48.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .pointerInput(dataPoints) {
                                fun indexAt(x: Float): Int {
                                    val canvasWidth = size.width.toFloat()
                                    val spacing = canvasWidth / dataPoints.size.coerceAtLeast(1)
                                    val adjustedX = x.coerceIn(0f, canvasWidth)
                                    return (adjustedX / spacing)
                                        .toInt()
                                        .coerceIn(0, dataPoints.lastIndex)
                                }

                                detectTapGestures { offset ->
                                    selectedIndex = indexAt(offset.x)
                                }
                                detectDragGestures(
                                    onDragStart = { offset -> selectedIndex = indexAt(offset.x) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        selectedIndex = indexAt(change.position.x)
                                    }
                                )
                            }
                    ) {
                        val labelHeight = 36f
                        val canvasHeight = size.height - labelHeight
                        val canvasWidth = size.width
                        val spacing = canvasWidth / dataPoints.size.coerceAtLeast(1)

                        for (i in 0..4) {
                            val y = canvasHeight * i / 4f
                            drawLine(gridColor, Offset(0f, y), Offset(canvasWidth, y), strokeWidth = 1f)
                        }

                        val normalY = canvasHeight - ((90f - minValue) / (maxValue - minValue)) * canvasHeight
                        val lowY = canvasHeight - ((70f - minValue) / (maxValue - minValue)) * canvasHeight
                        drawLine(Color(0xFF4CAF50).copy(alpha = 0.4f), Offset(0f, normalY), Offset(canvasWidth, normalY), 1.5f)
                        drawLine(Color(0xFFE53935).copy(alpha = 0.4f), Offset(0f, lowY), Offset(canvasWidth, lowY), 1.5f)

                        val points = dataPoints.mapIndexed { index, point ->
                            val x = spacing * index + spacing / 2
                            val y = canvasHeight - ((point.value - minValue) / (maxValue - minValue)) * canvasHeight
                            Offset(x, y) to point
                        }

                        if (points.size > 1) {
                            val areaPath = Path().apply {
                                moveTo(points.first().first.x, canvasHeight)
                                points.forEach { (offset, _) -> lineTo(offset.x, offset.y) }
                                lineTo(points.last().first.x, canvasHeight)
                                close()
                            }
                            drawPath(areaPath, areaGradient)

                            val linePath = Path().apply {
                                points.forEachIndexed { i, (offset, _) ->
                                    if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                                }
                            }
                            drawPath(linePath, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
                        }

                        points.forEachIndexed { index, (offset, point) ->
                            val isSelected = index == activeIndex
                            val barAlpha = if (isSelected) 0.75f else 0.35f
                            val barWidth = when {
                                isSelected -> 18f
                                points.size == 1 -> 24f
                                else -> 12f
                            }
                            drawLine(
                                color = glucoseLevelColor(point.level).copy(alpha = barAlpha),
                                start = Offset(offset.x, canvasHeight),
                                end = Offset(offset.x, offset.y),
                                strokeWidth = barWidth,
                                cap = StrokeCap.Round
                            )
                            val outerRadius = if (isSelected) 11f else 8f
                            val innerRadius = if (isSelected) 8f else 6f
                            drawCircle(color = Color.White, radius = outerRadius, center = offset)
                            drawCircle(color = glucoseLevelColor(point.level), radius = innerRadius, center = offset)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .width(totalWidth.dp)
                            .padding(top = chartHeight + 4.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dataPoints.forEachIndexed { index, point ->
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(64.dp),
                                maxLines = 2,
                                color = if (index == activeIndex) {
                                    glucoseLevelColor(point.level)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (index == activeIndex) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .background(color, RoundedCornerShape(5.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
