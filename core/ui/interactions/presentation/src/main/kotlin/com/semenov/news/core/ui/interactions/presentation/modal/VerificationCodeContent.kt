package com.semenov.news.core.ui.interactions.presentation.modal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.semenov.news.core.ui.designsystem.icon.NewsAnimatedCheckIcon
import com.semenov.news.core.ui.designsystem.icon.NewsIcons
import com.semenov.news.core.ui.designsystem.theme.NewsColors
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import com.semenov.news.core.ui.interactions.domain.model.modal.ModalDestination
import com.semenov.news.core.ui.interactions.domain.model.modal.VerificationCodeError
import com.semenov.news.core.ui.interactions.presentation.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val VerificationAnimationDuration = 1_600
private const val TraceEnd = 450f / VerificationAnimationDuration
private const val MergeEnd = 850f / VerificationAnimationDuration
private const val CheckEnd = 1_150f / VerificationAnimationDuration

@Composable
internal fun VerificationCodeContent(
    modal: ModalDestination.VerificationCode,
    isActive: Boolean,
    onDismiss: () -> Unit,
    canDismiss: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by modal.state.collectAsStateWithLifecycle()
    val palette = NewsTheme.colors
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val busy = state.isSubmitting || state.isVerified
    val codeLabel = stringResource(R.string.modal_verification_code_label)
    val errorMessage = when (state.error) {
        VerificationCodeError.InvalidCode -> stringResource(R.string.modal_verification_invalid_code)
        VerificationCodeError.Unavailable -> stringResource(R.string.modal_verification_unavailable)
        null -> null
    }
    var focused by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val shakeDistance = with(LocalDensity.current) { 9.dp.toPx() }
    val verification = remember(modal.id) { Animatable(0f) }
    var completionDelivered by remember(modal.id) { mutableStateOf(false) }
    val latestActive by rememberUpdatedState(isActive)
    val latestVerified by rememberUpdatedState(state.isVerified)
    val latestOnAnimationFinished by rememberUpdatedState(modal.onSuccessAnimationFinished)

    LaunchedEffect(modal.id, isActive, busy) {
        if (!isActive) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        } else if (!busy) {
            delay(180.milliseconds)
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(modal.id, isActive, state.isVerified) {
        if (!state.isVerified) {
            verification.snapTo(0f)
            completionDelivered = false
        } else if (isActive && !completionDelivered) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            // Animatable respects the system duration scale, including disabled animations.
            // Cancelling or replacing the modal cancels this continuation as well.
            verification.animateTo(1f, tween(VerificationAnimationDuration, easing = LinearEasing))
            if (latestActive && latestVerified && !completionDelivered) {
                completionDelivered = true
                latestOnAnimationFinished()
            }
        }
    }
    LaunchedEffect(state.error, state.code) {
        shake.snapTo(0f)
        if (state.error != null) {
            shake.animateTo(0f, keyframes {
                durationMillis = 420
                -1f at 50
                1f at 110
                -0.7f at 180
                0.7f at 250
                -0.3f at 320
                0f at 420
            })
        }
    }

    val progress = verification.value
    val auxiliaryAlpha = 1f - progress.between(TraceEnd, MergeEnd)
    val showSuccessTitle = state.isVerified && progress >= (650f / VerificationAnimationDuration)
    val phase = when {
        !state.isVerified && state.isSubmitting -> "verifying"
        !state.isVerified -> "verification_entry"
        progress < TraceEnd -> "tracing"
        progress < MergeEnd -> "merging"
        progress < CheckEnd -> "check"
        else -> "success"
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.CenterEnd) {
            if (modal.isDismissible) {
                val closeLabel = stringResource(R.string.modal_close)
                IconButton(
                    onClick = onDismiss,
                    enabled = canDismiss,
                    modifier = Modifier.size(40.dp).alpha(auxiliaryAlpha).testTag("modal_close")
                        .semantics { contentDescription = closeLabel },
                ) {
                    Icon(
                        imageVector = NewsIcons.Close,
                        contentDescription = null,
                        tint = palette.secondaryText.copy(alpha = if (canDismiss) 1f else 0.4f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        AnimatedContent(
            targetState = showSuccessTitle,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(130)) },
            modifier = Modifier.graphicsLayer { translationY = 12.dp.toPx() * progress.between(TraceEnd, MergeEnd) },
            label = "verificationTitle",
        ) { success ->
            Text(
                text = stringResource(if (success) R.string.modal_verification_success_title else R.string.modal_verification_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.modal_verification_subtitle, modal.userName),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().alpha(auxiliaryAlpha)
                .then(if (state.isVerified) Modifier.clearAndSetSemantics {} else Modifier),
        )
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier.fillMaxWidth().widthIn(max = 348.dp)
                .testTag("auth_verification_animation")
                .semantics { this[VerificationAnimationPhase] = phase },
        ) {
            if (state.isVerified) {
                // The editable input is removed from focus and accessibility during the morph.
                VerificationCells(
                    code = state.code,
                    focused = false,
                    invalid = false,
                    progress = progress,
                    verified = true,
                )
            } else {
                BasicTextField(
                    value = state.code,
                    onValueChange = { value -> modal.onCodeChanged(value.filter { it in '0'..'9' }.take(4)) },
                    enabled = !busy,
                    singleLine = true,
                    textStyle = TextStyle(color = NewsColors.transparent),
                    cursorBrush = SolidColor(NewsColors.transparent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (state.code.length == 4 && !busy) modal.onVerify() }),
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused }
                        .testTag("auth_code")
                        .semantics {
                            contentDescription = codeLabel
                            errorMessage?.let { error(it) }
                        }
                        .graphicsLayer { translationX = shake.value * shakeDistance },
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            Box(Modifier.matchParentSize().alpha(0f)) { innerTextField() }
                            VerificationCells(
                                code = state.code,
                                focused = focused && !busy,
                                invalid = state.error != null,
                                progress = 0f,
                                verified = false,
                            )
                        }
                    },
                )
            }
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                state.isSubmitting -> Text(
                    stringResource(R.string.modal_verification_checking),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                errorMessage != null -> Text(
                    text = errorMessage,
                    color = palette.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("auth_error").semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        Column(
            modifier = Modifier.alpha(auxiliaryAlpha)
                .then(if (state.isVerified) Modifier.clearAndSetSemantics {} else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(
                onClick = {
                    modal.onResend()
                    focusRequester.requestFocus()
                    keyboard?.show()
                },
                enabled = !busy && state.canResend,
                colors = ButtonDefaults.textButtonColors(contentColor = palette.accent),
                modifier = Modifier.testTag("auth_resend"),
            ) {
                Text(stringResource(R.string.modal_verification_resend), fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(state.resendCount > 0, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    stringResource(R.string.modal_verification_resent),
                    color = palette.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Text(
                text = stringResource(R.string.modal_verification_demo),
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun VerificationCells(
    code: String,
    focused: Boolean,
    invalid: Boolean,
    progress: Float,
    verified: Boolean,
) {
    val merge = progress.between(TraceEnd, MergeEnd)
    val check = progress.between(MergeEnd, CheckEnd)
    val successDescription = stringResource(R.string.modal_verification_success_description)
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val cellSize = ((maxWidth - 36.dp) / 4).coerceAtMost(76.dp)
        val stepPx = with(LocalDensity.current) { (cellSize + 12.dp).toPx() }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { index ->
                val cellMerge = smoothStep(merge)
                val tilt = sin(merge * PI).toFloat() * when (index) { 0 -> -7f; 1 -> 4f; 2 -> -4f; else -> 7f }
                CodeCell(
                    digit = code.getOrNull(index),
                    active = focused && code.length == index,
                    invalid = invalid,
                    verified = verified,
                    trace = progress.between(0f, TraceEnd),
                    digitAlpha = if (verified) 1f - merge.between(0f, 0.72f) else 1f,
                    merge = merge,
                    check = if (index == 1) check else 0f,
                    modifier = Modifier.size(cellSize).testTag("auth_code_cell_$index")
                        .graphicsLayer {
                            translationX = -(index - 1.5f) * stepPx * cellMerge
                            rotationZ = tilt
                            alpha = if (index == 1) 1f else 1f - merge.between(0.72f, 1f)
                        },
                )
            }
        }
        if (verified && progress >= MergeEnd) {
            Box(
                Modifier.size(cellSize).testTag("auth_code_success")
                    .semantics { contentDescription = successDescription; liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun CodeCell(
    digit: Char?,
    active: Boolean,
    invalid: Boolean,
    verified: Boolean,
    trace: Float,
    digitAlpha: Float,
    merge: Float,
    check: Float,
    modifier: Modifier = Modifier,
) {
    val palette = NewsTheme.colors
    val border by animateColorAsState(
        targetValue = when {
            invalid -> palette.error
            active || (verified && trace >= 1f) -> palette.accent
            else -> palette.secondaryText.copy(alpha = 0.3f)
        },
        animationSpec = tween(150),
        label = "codeBorder",
    )
    val focusGlow by animateFloatAsState(if (active) 1f else 0f, tween(150), label = "codeFocusGlow")
    val glow = if (verified) 0.35f + 0.65f * merge else focusGlow * 0.35f
    Box(modifier = modifier.drawBehind { drawSoftOutline(palette.accent, glow) }) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(18.dp),
            color = lerp(palette.background, palette.iconBackground, merge),
            border = BorderStroke(if (active || invalid || verified) 1.5.dp else 1.dp, border),
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = digit,
                    transitionSpec = { fadeIn(tween(110)) togetherWith fadeOut(tween(80)) },
                    label = "codeDigit",
                ) { character ->
                    Text(
                        text = character?.toString().orEmpty(),
                        color = if (invalid) palette.error else palette.text,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.alpha(digitAlpha).clearAndSetSemantics {},
                    )
                }
            }
        }
        if (verified) {
            Canvas(Modifier.matchParentSize()) {
                if (trace < 1f) drawBorderTrace(palette.accent, trace)
            }
            if (check > 0f) {
                NewsAnimatedCheckIcon(
                    progress = check,
                    modifier = Modifier.matchParentSize(),
                    tint = palette.accent,
                )
            }
        }
    }
}

/** Many low-opacity strokes form a continuous diffuse edge, rather than visible rings. */
private fun DrawScope.drawSoftOutline(color: Color, intensity: Float) {
    if (intensity <= 0f) return
    val radius = 18.dp.toPx()
    repeat(20) { layer ->
        val spread = (20 - layer) / 20f * 13.dp.toPx()
        val falloff = exp(-spread * spread / (2f * 6.dp.toPx() * 6.dp.toPx()))
        drawRoundRect(
            color = color.copy(alpha = intensity * falloff * 0.026f),
            topLeft = Offset(-spread / 2f, -spread / 2f),
            size = Size(size.width + spread, size.height + spread),
            cornerRadius = CornerRadius(radius + spread / 2f),
            style = Stroke(width = spread + 1.dp.toPx()),
        )
    }
}

private fun DrawScope.drawBorderTrace(color: Color, progress: Float) {
    val inset = 1.dp.toPx()
    val radius = 18.dp.toPx()
    val right = size.width - inset
    val bottom = size.height - inset
    val outline = Path().apply {
        moveTo(size.width / 2f, inset)
        lineTo(right - radius, inset)
        quadraticTo(right, inset, right, inset + radius)
        lineTo(right, bottom - radius)
        quadraticTo(right, bottom, right - radius, bottom)
        lineTo(inset + radius, bottom)
        quadraticTo(inset, bottom, inset, bottom - radius)
        lineTo(inset, inset + radius)
        quadraticTo(inset, inset, inset + radius, inset)
        lineTo(size.width / 2f, inset)
    }
    val measure = PathMeasure().apply { setPath(outline, false) }
    val head = measure.length * progress
    val trailLength = measure.length * 0.28f
    val traced = Path()
    measure.getSegment(0f, head, traced)
    drawPath(traced, color.copy(alpha = 0.45f), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
    if (head > 0f) {
        val tailStart = (head - trailLength).coerceAtLeast(0f)
        val tail = Path()
        measure.getSegment(tailStart, head, tail)
        val headPosition = measure.getPosition(head)
        val gradient = Brush.linearGradient(
            colors = listOf(color.copy(alpha = 0f), color),
            start = measure.getPosition(tailStart),
            end = headPosition,
        )
        // One continuous path avoids separate round caps accumulating into beads.
        drawPath(tail, gradient, alpha = 0.06f, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
        drawPath(tail, gradient, alpha = 0.1f, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(tail, gradient, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0f)),
                center = headPosition,
                radius = 8.dp.toPx(),
            ),
            radius = 8.dp.toPx(),
            center = headPosition,
        )
    }
}

private fun Float.between(start: Float, end: Float): Float = ((this - start) / (end - start)).coerceIn(0f, 1f)

private fun smoothStep(value: Float): Float = value * value * (3f - 2f * value)
