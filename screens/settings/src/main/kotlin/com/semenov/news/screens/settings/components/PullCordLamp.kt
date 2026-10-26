package com.semenov.news.screens.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.semenov.news.core.ui.designsystem.theme.NewsColors
import com.semenov.news.screens.settings.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A pendant lamp with a pull cord. A 48 dp downward pull toggles once per gesture; sideways
 * movement only swings the lamp. The clock stops when the cord has returned to rest.
 *
 * [onBulbPositioned] reports the bulb center in pixels, relative to this composable's top-left.
 * Give the lamp the full available width and approximately 244 dp of height. The bulb rests at
 * 70% of the width (30% in RTL) and 111 dp below the top, making it an origin for a theme reveal.
 */
@Composable
fun PullCordLamp(
    darkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onBulbPositioned: (Offset) -> Unit = {},
    lightViewportHeight: Dp = Dp.Unspecified,
) {
    val density = LocalDensity.current.density
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val latestOnToggle by rememberUpdatedState(onToggle)
    val latestOnBulbPositioned by rememberUpdatedState(onBulbPositioned)
    val description = stringResource(R.string.settings_lamp_description)
    val toggleAction = stringResource(R.string.settings_lamp_toggle_action)
    val lampState = stringResource(if (darkTheme) R.string.settings_lamp_on else R.string.settings_lamp_off)
    val simulation = remember(density, isRtl) { LampSimulation(density, isRtl) }
    val lightPainter = remember { LampLightPainter(SHADE_EDGE_Y, SHADE_HALF_WIDTH) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var animating by remember(simulation) { mutableStateOf(false) }
    var frame by remember(simulation) { mutableIntStateOf(0) }
    var focused by remember { mutableStateOf(false) }

    val toggle: () -> Unit = {
        latestOnToggle()
    }

    LaunchedEffect(simulation, canvasSize) {
        if (canvasSize != IntSize.Zero) latestOnBulbPositioned(simulation.bulbPosition)
    }
    LaunchedEffect(simulation, animating) {
        if (!animating) return@LaunchedEffect
        var previousTime = withFrameNanos { it }
        while (animating) {
            val time = withFrameNanos { it }
            val elapsed = ((time - previousTime) / 1_000_000_000f).coerceIn(1f / 240f, 1f / 30f)
            previousTime = time
            val stillMoving = simulation.step(elapsed)
            latestOnBulbPositioned(simulation.bulbPosition)
            frame++
            if (!stillMoving) animating = false
        }
    }

    Box(
        modifier =
            modifier
                .onSizeChanged { size ->
                    canvasSize = size
                    simulation.resize(size)
                    if (size != IntSize.Zero) {
                        simulation.nudge()
                        animating = true
                    }
                    frame++
                },
    ) {
        Canvas(
            modifier =
                Modifier.matchParentSize()
                    .testTag("settings_lamp_gestures")
                    .pointerInput(simulation) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!simulation.hitsCord(down.position)) return@awaitEachGesture
                            var dragging = false
                            var toggled = false
                            try {
                                val change =
                                    awaitTouchSlopOrCancellation(down.id) { pointer, _ -> pointer.consume() }
                                        ?: return@awaitEachGesture
                                simulation.beginDrag(down.position)
                                dragging = true
                                animating = true

                                fun move(position: Offset) {
                                    simulation.dragTo(position)
                                    if (!toggled && simulation.pulledPastThreshold) {
                                        toggled = true
                                        toggle()
                                    }
                                    frame++
                                }

                                move(change.position)
                                drag(change.id) { pointer ->
                                    move(pointer.position)
                                    pointer.consume()
                                }
                            } finally {
                                if (dragging) {
                                    simulation.release()
                                    animating = true
                                }
                            }
                        }
                    },
        ) {
            // Reading the frame only in drawing keeps physics updates out of composition/layout.
            @Suppress("UNUSED_VARIABLE") val currentFrame = frame
            val lightHeight =
                if (lightViewportHeight == Dp.Unspecified) size.height + 230.dp.toPx()
                else lightViewportHeight.toPx()
            drawLamp(simulation, darkTheme, focused, lightPainter, lightHeight)
        }
        // Keep the important accessibility node around the actual lamp. A full-width switch
        // above the header would cause TalkBack to treat the header underneath as occluded.
        Box(
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .absoluteOffset {
                        val center = canvasSize.width * if (isRtl) .30f else .70f
                        IntOffset((center - canvasSize.width / 2f).roundToInt(), 0)
                    }
                    .width(130.dp)
                    .fillMaxHeight()
                    .semantics {
                        role = Role.Switch
                        toggleableState = if (darkTheme) ToggleableState.On else ToggleableState.Off
                        contentDescription = description
                        stateDescription = lampState
                        onClick(label = toggleAction) {
                            simulation.nudge()
                            animating = true
                            toggle()
                            true
                        }
                    }
                    .onFocusChanged { focused = it.isFocused }
                    .onKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyUp &&
                            (event.key == Key.Enter || event.key == Key.Spacebar || event.key == Key.DirectionCenter)
                        ) {
                            simulation.nudge()
                            animating = true
                            toggle()
                            true
                        } else {
                            false
                        }
                    }
                    .focusable(),
        )
    }
}

private fun DrawScope.drawLamp(
    simulation: LampSimulation,
    darkTheme: Boolean,
    focused: Boolean,
    lightPainter: LampLightPainter,
    lightViewportHeight: Float,
) {
    val scale = simulation.density
    val x = simulation.centerX
    val angle = simulation.angle * 180f / Math.PI.toFloat()
    fun point(dx: Float, y: Float) = Offset(x + dx * scale, y * scale)

    rotate(degrees = angle, pivot = Offset(x, 0f)) {
        if (darkTheme) with(lightPainter) { drawLight(x, scale, lightViewportHeight) }
    }
    drawCord(simulation, darkTheme, focused)

    rotate(degrees = angle, pivot = Offset(x, 0f)) {
        drawLine(
            color = NewsColors.brass,
            start = point(0f, 4f),
            end = point(0f, 68f),
            strokeWidth = 1.7f * scale,
        )
        // The opaque flat shade covers the top of a full globe. Its rounded bottom
        // remains visible below the straight rim, exactly like the reference.
        drawCircle(
            color = if (darkTheme) NewsColors.bulbOn else NewsColors.bulbOff,
            radius = 12.5f * scale,
            center = point(0f, BULB_CENTER_Y),
        )
        if (darkTheme) with(lightPainter) { drawHalo(x, scale) }
        val shade = Path().apply {
            moveTo(x - SHADE_HALF_WIDTH * scale, SHADE_EDGE_Y * scale)
            cubicTo(x - 42f * scale, 94f * scale, x - 18f * scale, 85f * scale, x - 10f * scale, 70f * scale)
            lineTo(x + 10f * scale, 70f * scale)
            cubicTo(x + 18f * scale, 85f * scale, x + 42f * scale, 94f * scale, x + SHADE_HALF_WIDTH * scale, SHADE_EDGE_Y * scale)
            close()
        }
        drawPath(shade, NewsColors.shade)
        drawRoundRect(
            color = NewsColors.brass,
            topLeft = point(-11f, 65f),
            size = Size(22f * scale, 6f * scale),
            cornerRadius = CornerRadius(3f * scale),
        )
        drawLine(
            color = NewsColors.brass,
            start = point(-SHADE_HALF_WIDTH, SHADE_EDGE_Y),
            end = point(SHADE_HALF_WIDTH, SHADE_EDGE_Y),
            strokeWidth = 2f * scale,
            cap = StrokeCap.Round,
        )
    }
    // The ceiling plate stays attached to the ceiling while the pendant swings.
    drawRoundRect(
        color = NewsColors.brass,
        topLeft = point(-14f, 0f),
        size = Size(28f * scale, 6f * scale),
        cornerRadius = CornerRadius(3f * scale),
    )
}

private const val BULB_CENTER_Y = 111f
private const val SHADE_EDGE_Y = 112f
private const val SHADE_HALF_WIDTH = 42f
private const val CORD_ANCHOR_Y = 106f
private const val CORD_OFFSET_X = 18f

private fun DrawScope.drawCord(
    simulation: LampSimulation,
    darkTheme: Boolean,
    focused: Boolean,
) {
    val points = simulation.points
    if (points.isEmpty()) return
    val scale = simulation.density
    val rope =
        Path().apply {
            moveTo(points.first().x, points.first().y)
            for (index in 1 until points.lastIndex) {
                val next = (points[index] + points[index + 1]) / 2f
                quadraticTo(points[index].x, points[index].y, next.x, next.y)
            }
            lineTo(points.last().x, points.last().y)
        }
    drawPath(rope, NewsColors.shadow.copy(alpha = 0.09f), style = Stroke(3.7f * scale, cap = StrokeCap.Round))
    drawPath(
        rope,
        if (darkTheme) NewsColors.cordOn else NewsColors.cordOff,
        style = Stroke(1.65f * scale, cap = StrokeCap.Round),
    )
    val handle = points.last()
    if (focused) {
        drawCircle(NewsColors.focus.copy(alpha = 0.5f), 22f * scale, handle, style = Stroke(2f * scale))
    }
    drawCircle(
        color = NewsColors.brass,
        radius = 7.5f * scale,
        center = handle,
    )
    drawCircle(
        color = NewsColors.handleShine,
        radius = 2f * scale,
        center = handle + Offset(-2.2f * scale, -2.6f * scale),
    )
}

/** Mutable physics is confined to the UI thread; the composable publishes one draw invalidation per frame. */
private class LampSimulation(
    val density: Float,
    private val isRtl: Boolean,
) {
    var centerX = 0f
        private set
    var angle = 0f
        private set
    private var angularVelocity = 0f
    private var size = IntSize.Zero
    private var restingHandle = Offset.Zero
    private var dragging = false
    private var pointerOrigin = Offset.Zero
    private var handleOrigin = Offset.Zero
    private var dragTarget = Offset.Zero
    private var extension = 0f
    private var extensionVelocity = 0f
    private var releasedTime = 0f
    private var quietFrames = 0
    private var previousDragX = 0f
    val points = Array(POINT_COUNT) { Offset.Zero }
    private val previousPoints = Array(POINT_COUNT) { Offset.Zero }

    val bulbPosition: Offset
        get() = rotatePoint(Offset(centerX, BULB_CENTER_Y * density))

    val pulledPastThreshold: Boolean
        get() = dragTarget.y - handleOrigin.y >= 48f * density

    private val cordAnchor: Offset
        get() = rotatePoint(Offset(centerX + (if (isRtl) -CORD_OFFSET_X else CORD_OFFSET_X) * density, CORD_ANCHOR_Y * density))

    fun resize(newSize: IntSize) {
        if (newSize == size) return
        size = newSize
        centerX = size.width * if (isRtl) 0.30f else 0.70f
        restingHandle =
            Offset(
                centerX + (if (isRtl) -CORD_OFFSET_X else CORD_OFFSET_X) * density,
                min(220f * density, max(150f * density, size.height - 24f * density)),
            )
        settle()
    }

    fun hitsCord(position: Offset): Boolean {
        if (size == IntSize.Zero) return false
        val handle = points.last()
        val nearHandle = (position - handle).getDistance() <= 34f * density
        val nearRope = abs(position.x - handle.x) <= 26f * density &&
            position.y >= cordAnchor.y + 24f * density && position.y <= handle.y + 25f * density
        return nearHandle || nearRope
    }

    fun beginDrag(pointer: Offset) {
        dragging = true
        pointerOrigin = pointer
        handleOrigin = points.last()
        dragTarget = handleOrigin
        previousDragX = dragTarget.x
        quietFrames = 0
    }

    fun dragTo(pointer: Offset) {
        val delta = pointer - pointerOrigin
        dragTarget =
            Offset(
                (handleOrigin.x + delta.x).coerceIn(restingHandle.x - 80f * density, restingHandle.x + 80f * density),
                (handleOrigin.y + delta.y).coerceIn(restingHandle.y - 24f * density, restingHandle.y + 64f * density),
            )
        angularVelocity += ((dragTarget.x - previousDragX) / density * 0.004f).coerceIn(-0.12f, 0.12f)
        previousDragX = dragTarget.x
        extension = max(0f, (dragTarget - cordAnchor).getDistance() - (restingHandle - cordAnchor).getDistance())
        extensionVelocity = 0f
        points[points.lastIndex] = dragTarget
    }

    fun release() {
        dragging = false
        releasedTime = 0f
        quietFrames = 0
    }

    fun nudge() {
        if (size == IntSize.Zero) return
        angularVelocity += if (isRtl) -0.22f else 0.22f
        extension = max(extension, 12f * density)
        release()
    }

    fun step(elapsed: Float): Boolean {
        if (size == IntSize.Zero) return false
        // Small fixed substeps make the rope stable on both 60 Hz and high refresh displays.
        val steps = max(1, kotlin.math.ceil(elapsed / (1f / 120f)).toInt())
        val dt = elapsed / steps
        var movement = 0f
        repeat(steps) {
            val oldAngle = angle
            val pull = if (dragging) (dragTarget.x - restingHandle.x) / (80f * density) * 0.65f else 0f
            angularVelocity += (-angle * 31f - angularVelocity * 5.8f + pull) * dt
            angle = (angle + angularVelocity * dt).coerceIn(-0.13f, 0.13f)
            if (!dragging) {
                extensionVelocity += (-extension * 100f - extensionVelocity * 15f) * dt
                extension = max(0f, extension + extensionVelocity * dt)
            }
            val anchor = cordAnchor
            val restLength = (restingHandle - Offset(restingHandle.x, CORD_ANCHOR_Y * density)).getDistance()
            val segmentLength = (restLength + extension) / points.lastIndex
            val damping = kotlin.math.exp(-3.8f * dt)
            for (index in 1..points.lastIndex) {
                if (dragging && index == points.lastIndex) continue
                val point = points[index]
                val velocity = (point - previousPoints[index]) * damping
                previousPoints[index] = point
                points[index] = point + velocity + Offset(0f, 1700f * density * dt * dt)
            }
            repeat(14) {
                points[0] = anchor
                if (dragging) points[points.lastIndex] = dragTarget
                for (index in 0 until points.lastIndex) {
                    val difference = points[index + 1] - points[index]
                    val length = sqrt(difference.x * difference.x + difference.y * difference.y).coerceAtLeast(0.001f)
                    val correction = difference * ((length - segmentLength) / length)
                    when {
                        index == 0 -> points[1] -= correction
                        dragging && index + 1 == points.lastIndex -> points[index] += correction
                        else -> {
                            points[index] += correction * 0.5f
                            points[index + 1] -= correction * 0.5f
                        }
                    }
                }
            }
            points[0] = anchor
            if (dragging) points[points.lastIndex] = dragTarget
            for (index in 1..points.lastIndex) {
                movement = max(movement, (points[index] - previousPoints[index]).getDistance())
            }
            movement = max(movement, abs(angle - oldAngle) * 100f * density)
        }
        if (dragging) return true
        releasedTime += elapsed
        if (movement < 0.1f * density && abs(angularVelocity) < 0.01f && extension < 0.1f * density) {
            quietFrames++
        } else {
            quietFrames = 0
        }
        if (quietFrames >= 12 || releasedTime > 3.5f) {
            settle()
            return false
        }
        return true
    }

    private fun settle() {
        angle = 0f
        angularVelocity = 0f
        extension = 0f
        extensionVelocity = 0f
        dragging = false
        val anchor = cordAnchor
        for (index in points.indices) {
            val amount = index.toFloat() / points.lastIndex
            points[index] = anchor + (restingHandle - anchor) * amount
            previousPoints[index] = points[index]
        }
    }

    private fun rotatePoint(point: Offset): Offset {
        val relativeX = point.x - centerX
        return Offset(
            centerX + relativeX * cos(angle) - point.y * sin(angle),
            relativeX * sin(angle) + point.y * cos(angle),
        )
    }

    private companion object {
        const val POINT_COUNT = 12
    }
}
