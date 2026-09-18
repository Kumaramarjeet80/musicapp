package com.ammu.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ammu.player.ui.theme.AccentCyan
import com.ammu.player.ui.theme.PureBlack
import com.ammu.player.ui.theme.SurfaceBorder
import com.ammu.player.ui.theme.SurfaceCard

@Composable
fun VinylDiscView(
    albumArtUri: String?,
    isPlaying: Boolean,
    isVinylMode: Boolean,
    onToggleVinylMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 18000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.stop()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onToggleVinylMode()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isVinylMode) {
            // Ambient Vinyl Glow
            Box(
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(AccentCyan.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // Vinyl Record Surface
            Box(
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .rotate(rotation.value)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(PureBlack),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Concentric Grooves & Light Sheen
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f

                    // Micro-groove rings
                    for (r in 35 until 90 step 4) {
                        val radius = maxRadius * (r / 100f)
                        drawCircle(
                            color = Color(0xFF1E1E28),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.2f)
                        )
                    }

                    // Vinyl specular sheen reflection lines
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 35f
                    )
                }

                // Center Album Label
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.42f)
                        .clip(CircleShape)
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    if (!albumArtUri.isNullOrBlank()) {
                        AsyncImage(
                            model = albumArtUri,
                            contentDescription = "Album Artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Center Spindle Hole
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(PureBlack)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFF4A4A58),
                                radius = size.width / 2f,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }
            }
        } else {
            // Modern Rounded Corner Card Mode
            Box(
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                if (!albumArtUri.isNullOrBlank()) {
                    AsyncImage(
                        model = albumArtUri,
                        contentDescription = "Album Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SurfaceCard, Color(0xFF1B1B26))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }
        }
    }
}
