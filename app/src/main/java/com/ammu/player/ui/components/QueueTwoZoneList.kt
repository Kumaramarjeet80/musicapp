package com.ammu.player.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammu.player.data.model.TrackEntity
import com.ammu.player.ui.theme.AccentCyan
import com.ammu.player.ui.theme.AccentRed
import com.ammu.player.ui.theme.SurfaceBorder
import com.ammu.player.ui.theme.SurfaceCard
import com.ammu.player.ui.theme.SurfaceElevated
import com.ammu.player.ui.theme.TextPrimary
import com.ammu.player.ui.theme.TextSecondary
import com.ammu.player.ui.theme.TextTertiary
import com.ammu.player.ui.theme.Typography

@Composable
fun QueueTwoZoneList(
    queue: List<TrackEntity>,
    currentIndex: Int,
    onTrackSelected: (TrackEntity) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(queue, key = { _, item -> item.id }) { index, track ->
            val isCurrent = index == currentIndex
            val isDragging = draggingIndex == index
            val isDropTarget = dropTargetIndex == index

            val elevation by animateDpAsState(
                targetValue = if (isDragging) 12.dp else 0.dp,
                label = "elevation"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // Red inline drop-indicator target line
                if (isDropTarget && !isDragging) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(AccentRed)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation, RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isDragging -> SurfaceElevated
                                isCurrent -> AccentCyan.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ZONE A: Title / Info + Drag Reorder Zone
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(queue.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = index
                                        dropTargetIndex = index
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val rowHeight = 60f
                                        val deltaItems = (dragAmount.y / rowHeight).toInt()
                                        val target = (index + deltaItems).coerceIn(0, queue.size - 1)
                                        dropTargetIndex = target
                                    },
                                    onDragEnd = {
                                        val from = draggingIndex
                                        val to = dropTargetIndex
                                        if (from != null && to != null && from != to) {
                                            onReorder(from, to)
                                        }
                                        draggingIndex = null
                                        dropTargetIndex = null
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dropTargetIndex = null
                                    }
                                )
                            }
                            .clickable { onTrackSelected(track) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Reorder handle",
                            tint = if (isDragging) AccentCyan else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.displayTitle(),
                                style = Typography.bodyLarge.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                ),
                                color = if (isCurrent) AccentCyan else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artist} • ${formatDuration(track.durationMs)}",
                                style = Typography.bodyMedium.copy(fontSize = 11.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Vertical Divider separating Zone A and Zone B
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(SurfaceBorder)
                    )

                    // ZONE B: Scroll-Only Action Buttons (Shift Up, Shift Down, Remove)
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                if (index > 0) onReorder(index, index - 1)
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropUp,
                                contentDescription = "Shift Up",
                                tint = if (index > 0) TextSecondary else TextTertiary.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (index < queue.size - 1) onReorder(index, index + 1)
                            },
                            enabled = index < queue.size - 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Shift Down",
                                tint = if (index < queue.size - 1) TextSecondary else TextTertiary.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove track",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
