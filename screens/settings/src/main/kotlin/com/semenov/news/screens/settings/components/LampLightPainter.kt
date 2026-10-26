package com.semenov.news.screens.settings.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.semenov.news.core.ui.designsystem.theme.NewsColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.tan

/** Reuses the reference's soft light gradients while the lamp swings. Dimensions are in dp. */
internal class LampLightPainter(
    private val rimY: Float,
    private val rimHalfWidth: Float,
) {
    private val additiveLayer = Paint().apply { blendMode = BlendMode.Plus }
    private var cachedReach = -1f
    private lateinit var cone: Brush
    private val coneMask = run {
        val inner = CONE_HALF_ANGLE * (1f - FEATHER) / TURN
        val outer = CONE_HALF_ANGLE * (1f + FEATHER) / TURN
        Brush.sweepGradient(
            *sides(.25f - outer, .25f - inner, .25f + inner, .25f + outer),
            center = Offset.Zero,
        )
    }
    private val underRim = Brush.sweepGradient(*shadowEdge(), center = Offset.Zero)
    private val halo = Brush.radialGradient(
        *falloff(NewsColors.bulbGlow, .4f) { t -> exp(-4f * t * t) * (1f - t) },
        center = Offset.Zero,
        radius = HALO_RADIUS,
    )
    private val spill = Brush.radialGradient(
        *falloff(NewsColors.beam, .14f) { t -> exp(-2.5f * t * t) * (1f - t) },
        center = Offset.Zero,
        radius = 230f,
    )
    private val pool = Brush.radialGradient(
        *falloff(NewsColors.beam, .42f) { t -> exp(-3f * t * t) * (1f - t) },
        center = Offset.Zero,
        radius = 120f,
    )

    fun DrawScope.drawLight(x: Float, scale: Float, viewportHeight: Float) {
        val reach = max(size.width, viewportHeight) / scale * 1.4f
        if (cachedReach != reach) {
            cachedReach = reach
            cone = Brush.radialGradient(
                *falloff(NewsColors.beam, .34f) { t ->
                    (1f - t) * (1f - t) / (1f + 5f * t)
                },
                center = Offset.Zero,
                radius = reach,
            )
        }

        // DstIn must only mask the emitted light. Add the isolated result to the page,
        // including its cards, instead of masking or darkening the content underneath.
        val canvas = drawContext.canvas
        canvas.saveLayer(Rect(0f, 0f, size.width, viewportHeight), additiveLayer)
        try {
            scale(scale, scale, Offset.Zero) {
                val bulbX = x / scale
                val rise = rimHalfWidth / tan(CONE_HALF_ANGLE) * .3f
                // Keep the shade opaque, with no halo leaking above its lower edge.
                clipRect(top = rimY, bottom = viewportHeight / scale) {
                    translate(bulbX, rimY) {
                        drawRect(cone, Offset(-reach, -rise), Size(2f * reach, reach))
                    }
                    translate(bulbX, rimY - rise) {
                        drawRect(
                            coneMask,
                            Offset(-reach - MASK_MARGIN, -MASK_MARGIN),
                            Size(2f * (reach + MASK_MARGIN), reach + 2f * MASK_MARGIN),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                    translate(bulbX, rimY) {
                        drawCircle(spill, 230f, Offset.Zero, blendMode = BlendMode.Plus)
                        drawCircle(pool, 120f, Offset.Zero, blendMode = BlendMode.Plus)
                    }
                    translate(bulbX, rimY - LIGHT_SOURCE) {
                        val around = reach + MASK_MARGIN
                        drawRect(
                            underRim,
                            Offset(-around, -around),
                            Size(2f * around, 2f * around),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                }
            }
        } finally {
            canvas.restore()
        }
    }

    /** The bulb's small soft halo, drawn over the bulb and cord, then covered by the shade. */
    fun DrawScope.drawHalo(x: Float, density: Float) {
        scale(density, density, Offset.Zero) {
            translate(x / density, rimY) {
                clipRect(-HALO_RADIUS, 0f, HALO_RADIUS, HALO_RADIUS) {
                    scale(rimHalfWidth / HALO_RADIUS * .9f, 1f, Offset.Zero) {
                        drawCircle(halo, HALO_RADIUS, Offset.Zero, blendMode = BlendMode.Plus)
                    }
                }
            }
        }
    }

    private fun shadowEdge(): Array<Pair<Float, Color>> {
        val throughRim = atan2(LIGHT_SOURCE, rimHalfWidth) / TURN
        val start = throughRim - .04f
        val span = .08f
        return Array(241) { index ->
            val turn = index / 240f
            val right = if (turn > .5f) turn - 1f else turn
            val t = minOf((right - start) / span, (.5f - start - turn) / span).coerceIn(0f, 1f)
            turn to NewsColors.lightMask.copy(alpha = smoothStep(t))
        }
    }
}

private inline fun falloff(
    color: Color,
    peak: Float,
    curve: (Float) -> Float,
): Array<Pair<Float, Color>> = Array(17) { index ->
    val t = index / 16f
    t to color.copy(alpha = peak * curve(t))
}

private fun sides(from: Float, full: Float, fade: Float, to: Float): Array<Pair<Float, Color>> {
    val stops = ArrayList<Pair<Float, Color>>(20)
    stops += 0f to NewsColors.lightMask.copy(alpha = 0f)
    for (index in 0..8) {
        val t = index / 8f
        stops += (from + (full - from) * t) to NewsColors.lightMask.copy(alpha = smoothStep(t))
    }
    for (index in 0..8) {
        val t = index / 8f
        stops += (fade + (to - fade) * t) to NewsColors.lightMask.copy(alpha = 1f - smoothStep(t))
    }
    stops += 1f to NewsColors.lightMask.copy(alpha = 0f)
    return stops.toTypedArray()
}

private fun smoothStep(t: Float) = t * t * (3f - 2f * t)

private const val CONE_HALF_ANGLE = .78f
private const val FEATHER = .8f
private const val TURN = 2f * PI.toFloat()
private const val MASK_MARGIN = 4f
private const val LIGHT_SOURCE = 30f
private const val HALO_RADIUS = 64f
