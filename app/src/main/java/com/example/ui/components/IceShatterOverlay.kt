package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.game.CrystalShard

@Composable
fun IceShatterOverlay(
    shards: List<CrystalShard>,
    modifier: Modifier = Modifier
) {
    if (shards.isEmpty()) return

    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(shards) {
        progressAnim.snapTo(0f)
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = LinearEasing)
        )
    }

    val progress = progressAnim.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Central radiant flash
        if (progress < 0.4f) {
            val flashAlpha = (1f - progress / 0.4f) * 0.7f
            drawCircle(
                color = Color.White.copy(alpha = flashAlpha),
                radius = w * 0.5f * (progress / 0.4f),
                center = Offset(w / 2f, h / 2f)
            )
        }

        // Render each shard
        shards.forEach { shard ->
            val curX = shard.startX * w + shard.vx * progress
            // Apply slight gravity to shards
            val curY = shard.startY * h + shard.vy * progress + (180f * progress * progress)
            val curRot = shard.rotation + shard.vRotation * progress
            val alpha = (1f - (progress * 1.15f)).coerceIn(0f, 1f)

            if (alpha > 0f) {
                rotate(degrees = curRot, pivot = Offset(curX, curY)) {
                    val s = shard.size
                    // Diamond/crystal shard polygon
                    val shardPath = Path().apply {
                        moveTo(curX, curY - s)
                        lineTo(curX + s * 0.6f, curY)
                        lineTo(curX, curY + s * 0.8f)
                        lineTo(curX - s * 0.5f, curY - s * 0.2f)
                        close()
                    }

                    drawPath(
                        path = shardPath,
                        color = Color(shard.colorHex).copy(alpha = alpha)
                    )

                    // Bright inner facet highlight
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.8f),
                        radius = s * 0.25f,
                        center = Offset(curX, curY)
                    )
                }
            }
        }
    }
}
