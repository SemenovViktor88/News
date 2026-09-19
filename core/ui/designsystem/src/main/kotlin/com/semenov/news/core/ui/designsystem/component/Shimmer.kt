package com.semenov.news.core.ui.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val transition = rememberInfiniteTransition(label = "news shimmer")
    val offset =
        transition.animateFloat(
            initialValue = -600f,
            targetValue = 1_200f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1_400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "news shimmer offset",
        ).value
    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val highlightColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
    val brush =
        Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start = Offset(offset - 300f, 0f),
            end = Offset(offset, 300f),
        )

    androidx.compose.foundation.layout.Box(
        modifier =
            modifier
                .graphicsLayer {
                    this.shape = shape
                    clip = true
                }.background(brush),
    )
}
