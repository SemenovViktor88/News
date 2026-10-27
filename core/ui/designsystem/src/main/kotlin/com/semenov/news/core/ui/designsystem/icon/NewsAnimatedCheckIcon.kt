package com.semenov.news.core.ui.designsystem.icon

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.semenov.news.core.ui.designsystem.theme.NewsTheme

/** Draws the shared success check from its first point to its last point. */
@Composable
fun NewsAnimatedCheckIcon(
    progress: Float,
    modifier: Modifier = Modifier,
    tint: Color = NewsTheme.colors.accent,
) {
    Canvas(modifier) {
        val check = Path().apply {
            moveTo(size.width * 0.29f, size.height * 0.51f)
            lineTo(size.width * 0.44f, size.height * 0.66f)
            lineTo(size.width * 0.73f, size.height * 0.36f)
        }
        val measure = PathMeasure().apply { setPath(check, false) }
        val visible = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), visible)
        drawPath(
            visible,
            tint,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
