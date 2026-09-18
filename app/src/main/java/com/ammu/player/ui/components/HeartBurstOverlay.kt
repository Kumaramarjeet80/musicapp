package com.ammu.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammu.player.ui.theme.AccentPink
import com.ammu.player.ui.theme.AccentRed
import com.ammu.player.ui.theme.SurfaceCard
import com.ammu.player.ui.theme.TextPrimary
import com.ammu.player.ui.theme.Typography
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class HeartParticle(
    val angle: Double,
    val speed: Float,
    val color: Color,
    val size: Float
)

@Composable
fun HeartBurstOverlay(
    triggerKey: Long,
    isFavorite: Boolean,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (triggerKey == 0L) return

    val progress = remember(triggerKey) { Animatable(0f) }
    val particles = remember(triggerKey) {
        val count = 24
        val colors = if (isFavorite) {
            listOf(AccentPink, AccentRed, Color(0xFFFF80AB), Color(0xFFFF4081))
        } else {
            listOf(Color(0xFF78909C), Color(0xFFB0BEC5), Color(0xFF546E7A))
        }
        List(count) {
            HeartParticle(
                angle = Random.nextDouble(0.0, 2 * Math.PI),
                speed = Random.nextFloat() * 180f + 60f,
                color = colors[it % colors.size],
                size = Random.nextFloat() * 8f + 6f
            )
        }
    }

    LaunchedEffect(triggerKey) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
        onAnimationEnd()
    }

    val emotionalText = if (isFavorite) {
        "Thank you for loving me 🥺❤️"
    } else {
        "Dil tod diya na mera 😿💔"
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Particle canvas
        if (progress.value < 1f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val p = progress.value
                val alpha = (1f - p).coerceIn(0f, 1f)

                for (particle in particles) {
                    val distance = particle.speed * p
                    val x = center.x + (cos(particle.angle) * distance).toFloat()
                    val y = center.y + (sin(particle.angle) * distance).toFloat()

                    drawCircle(
                        color = particle.color.copy(alpha = alpha),
                        radius = particle.size * (1f - p * 0.5f),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Floating Emotional Toast
        AnimatedVisibility(
            visible = progress.value in 0.05f..0.90f,
            enter = slideInVertically(initialOffsetY = { 60 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -60 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(32.dp))
                    .background(SurfaceCard.copy(alpha = 0.95f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emotionalText,
                    style = Typography.titleMedium.copy(fontSize = 14.sp),
                    color = TextPrimary
                )
            }
        }
    }
}
