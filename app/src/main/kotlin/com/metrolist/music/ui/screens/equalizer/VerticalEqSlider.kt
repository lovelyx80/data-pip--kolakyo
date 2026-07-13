package com.metrolist.music.ui.screens.equalizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VerticalEqSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    sublabel: String = "",
    minDb: Float = -12f,
    maxDb: Float = 12f,
) {
    val animValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 150),
        label = "eq_slider"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .width(44.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = String.format("%.0f", animValue),
            style = MaterialTheme.typography.labelSmall,
            color = primaryColor,
        )

        Box(
            modifier = Modifier
                .width(36.dp)
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .width(36.dp)
                    .height(200.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val fraction = 1f - (offset.y / size.height.toFloat())
                            val newValue = minDb + fraction * (maxDb - minDb)
                            onValueChange(newValue.coerceIn(minDb, maxDb))
                        }
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, _ ->
                            val fraction = 1f - (change.position.y / size.height.toFloat())
                            val newValue = minDb + fraction * (maxDb - minDb)
                            onValueChange(newValue.coerceIn(minDb, maxDb))
                        }
                    }
            ) {
                val barHeight = size.height
                val barWidth = size.width

                drawRoundRect(
                    color = surfaceColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )

                val centerY = size.height / 2f
                drawLine(
                    color = onSurfaceVariant.copy(alpha = 0.3f),
                    start = Offset(2.dp.toPx(), centerY),
                    end = Offset(size.width - 2.dp.toPx(), centerY),
                    strokeWidth = 1f,
                )

                val fraction = (animValue - minDb) / (maxDb - minDb)
                val barTop = barHeight * (1f - fraction)
                val barFillHeight = barHeight - barTop
                val barLeft = 2.dp.toPx()
                val effectiveBarWidth = barWidth - 4.dp.toPx()

                if (animValue >= 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.4f),
                                primaryColor,
                            ),
                            startY = barTop,
                            endY = barTop + barFillHeight,
                        ),
                        topLeft = Offset(barLeft, barTop),
                        size = Size(effectiveBarWidth, barFillHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                } else {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.5f),
                                primaryColor.copy(alpha = 0.2f),
                            ),
                            startY = centerY,
                            endY = centerY - barFillHeight,
                        ),
                        topLeft = Offset(barLeft, centerY),
                        size = Size(effectiveBarWidth, -barFillHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }

                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = Offset(barWidth / 2f, barTop),
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVariant,
        )
        if (sublabel.isNotEmpty()) {
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
