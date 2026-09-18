package com.ammu.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ammu.player.data.model.TimestampEntity
import com.ammu.player.ui.theme.AccentCyan
import com.ammu.player.ui.theme.AccentPink
import com.ammu.player.ui.theme.SurfaceBorder
import com.ammu.player.ui.theme.TextSecondary
import com.ammu.player.ui.theme.Typography

@Composable
fun WaveformScrubber(
    positionMs: Long,
    durationMs: Long,
    timestamps: List<TimestampEntity>,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableStateOf(0L) }

    val effectivePos = if (isDragging) dragPositionMs else positionMs
    val fraction = if (durationMs > 0) (effectivePos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val targetFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val targetMs = (targetFraction * durationMs).toLong()
                        onSeek(targetMs)
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val targetFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            dragPositionMs = (targetFraction * durationMs).toLong()
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val currentPx = fraction * size.width
                            val newPx = (currentPx + dragAmount).coerceIn(0f, size.width.toFloat())
                            val newFraction = (newPx / size.width).coerceIn(0f, 1f)
                            dragPositionMs = (newFraction * durationMs).toLong()
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek(dragPositionMs)
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val midY = canvasHeight / 2f

                // Inactive track line
                drawRoundRect(
                    color = SurfaceBorder,
                    topLeft = Offset(0f, midY - 3f),
                    size = Size(canvasWidth, 6f),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Active glowing progress track
                val activeWidth = fraction * canvasWidth
                if (activeWidth > 0) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(AccentPink, AccentCyan),
                            startX = 0f,
                            endX = activeWidth
                        ),
                        topLeft = Offset(0f, midY - 3f),
                        size = Size(activeWidth, 6f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }

                // Draw timestamp tick pips
                if (durationMs > 0) {
                    for (marker in timestamps) {
                        val pipFraction = (marker.timestampMs.toFloat() / durationMs).coerceIn(0f, 1f)
                        val pipX = pipFraction * canvasWidth
                        val pipColor = try {
                            Color(android.graphics.Color.parseColor(marker.colorHex))
                        } catch (e: Exception) {
                            AccentCyan
                        }

                        // Vertical pip line
                        drawLine(
                            color = pipColor,
                            start = Offset(pipX, midY - 14f),
                            end = Offset(pipX, midY + 14f),
                            strokeWidth = 3.5f
                        )
                        // Top pip diamond/dot
                        drawCircle(
                            color = pipColor,
                            radius = 4f,
                            center = Offset(pipX, midY - 14f)
                        )
                    }
                }

                // Scrubber Thumb
                val thumbX = fraction * canvasWidth
                drawCircle(
                    color = AccentCyan,
                    radius = if (isDragging) 10f else 7f,
                    center = Offset(thumbX, midY)
                )
                drawCircle(
                    color = Color.White,
                    radius = if (isDragging) 4.5f else 3f,
                    center = Offset(thumbX, midY)
                )
            }
        }

        // Time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(effectivePos),
                style = Typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = formatDuration(durationMs),
                style = Typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
