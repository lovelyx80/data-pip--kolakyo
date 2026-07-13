/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.random.Random

private data class GlowParticle(
    val baseX: Float,      // 0f..1f fraction of width
    val baseY: Float,      // 0f..1f fraction of height
    val radius: Float,     // in dp-equivalent px at draw time
    val speed: Float,       // relative drift speed
    val phase: Float,       // animation phase offset so particles don't move in sync
    val colorIndex: Int,
)

/**
 * A soft, low-opacity animated glow/particle field meant to sit behind screen
 * content as a subtle "neo dark" ambient background. Particles slowly drift
 * and pulse; nothing is interactive and it never intercepts touch input.
 *
 * Intentionally cheap: a fixed small particle count, single infinite transition
 * driving all of them, and only additive radial-gradient circles (no bitmaps,
 * no per-frame allocations in the hot path).
 */
@Composable
fun AnimatedGlowBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 14,
) {
    val darkTheme = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary

    val particles = remember(particleCount) {
        val rnd = Random(42)
        List(particleCount) {
            GlowParticle(
                baseX = rnd.nextFloat(),
                baseY = rnd.nextFloat(),
                radius = rnd.nextFloat() * 90f + 60f,
                speed = rnd.nextFloat() * 0.6f + 0.4f,
                phase = rnd.nextFloat() * 6.28f,
                colorIndex = rnd.nextInt(2),
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glow_bg")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glow_bg_t",
    )

    val colorA = accent.copy(alpha = if (darkTheme) 0.16f else 0.10f)
    val colorB = secondary.copy(alpha = if (darkTheme) 0.14f else 0.08f)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawGlowParticles(particles, t, colorA, colorB)
    }
}

private fun DrawScope.drawGlowParticles(
    particles: List<GlowParticle>,
    t: Float,
    colorA: Color,
    colorB: Color,
) {
    val w = size.width
    val h = size.height
    for (p in particles) {
        val drift = t * 0.02f * p.speed
        val x = (p.baseX * w) + sin(drift + p.phase) * (w * 0.08f)
        val y = (p.baseY * h) + sin(drift * 0.7f + p.phase * 1.3f) * (h * 0.06f)
        val pulse = 0.85f + 0.15f * sin(drift * 1.5f + p.phase)
        val radius = p.radius * pulse

        val color = if (p.colorIndex == 0) colorA else colorB

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, color.copy(alpha = 0f)),
                center = Offset(x, y),
                radius = radius,
            ),
            radius = radius,
            center = Offset(x, y),
        )
    }
}
