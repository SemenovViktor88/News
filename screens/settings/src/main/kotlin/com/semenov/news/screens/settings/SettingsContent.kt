package com.semenov.news.screens.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semenov.news.core.ui.designsystem.theme.NewsPalette
import com.semenov.news.core.ui.designsystem.theme.NewsTheme
import com.semenov.news.core.ui.designsystem.theme.NewsColors
import com.semenov.news.core.ui.designsystem.theme.newsPalette
import com.semenov.news.screens.settings.components.PullCordLamp
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun SettingsContent(
    darkTheme: Boolean,
    hapticsEnabled: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onLampToggle: (Boolean) -> Unit = onDarkThemeChange,
) {
    var baseTheme by remember { mutableStateOf(darkTheme) }
    var revealTheme by remember { mutableStateOf(darkTheme) }
    var bulbPosition by remember { mutableStateOf(Offset.Zero) }
    val reveal = remember { Animatable(0f) }
    val scrollState = rememberScrollState()

    LaunchedEffect(darkTheme) {
        if (baseTheme == revealTheme) {
            if (darkTheme == baseTheme) return@LaunchedEffect
            revealTheme = darkTheme
            reveal.snapTo(0f)
        }
        // Reversing a pull during the reveal retraces the same circle smoothly.
        val target = if (darkTheme == revealTheme) 1f else 0f
        reveal.animateTo(target, tween(durationMillis = 650, easing = FastOutSlowInEasing))
        baseTheme = darkTheme
        revealTheme = darkTheme
        reveal.snapTo(0f)
    }

    val basePalette = newsPalette(baseTheme)
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().testTag("settings_screen").background(basePalette.background),
    ) {
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        ) {
            SettingsPage(
                palette = basePalette,
                darkTheme = darkTheme,
                hapticsEnabled = hapticsEnabled,
                onDarkThemeChange = onDarkThemeChange,
                onHapticsChange = onHapticsChange,
                interactive = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = viewportHeight),
            )
            if (baseTheme != revealTheme) {
                SettingsPage(
                    palette = newsPalette(revealTheme),
                    darkTheme = darkTheme,
                    hapticsEnabled = hapticsEnabled,
                    onDarkThemeChange = {},
                    onHapticsChange = {},
                    interactive = false,
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clearAndSetSemantics {}
                            .drawWithContent {
                                val center =
                                    bulbPosition.takeUnless { it == Offset.Zero }
                                        ?: Offset(size.width * .7f, 111.dp.toPx())
                                val radius =
                                    hypot(
                                        max(center.x, size.width - center.x),
                                        max(center.y, size.height - center.y),
                                    ) * reveal.value
                                val clip = Path().apply { addOval(Rect(center, radius)) }
                                clipPath(clip) { this@drawWithContent.drawContent() }
                            },
                )
            }
            // The lamp is composed once, so its physics, gesture, and accessibility
            // action stay continuous while the two palette layers are composited.
            PullCordLamp(
                darkTheme = darkTheme,
                onToggle = { onLampToggle(!darkTheme) },
                onBulbPositioned = { bulbPosition = it },
                lightViewportHeight = viewportHeight,
                modifier = Modifier.fillMaxWidth().height(LampRegionHeight),
            )
        }
    }
}

@Composable
private fun SettingsPage(
    palette: NewsPalette,
    darkTheme: Boolean,
    hapticsEnabled: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(palette.background)) {
        Box(modifier = Modifier.fillMaxWidth().height(LampRegionHeight)) {
            Column(
                modifier = Modifier.padding(start = 26.dp, top = 35.dp, end = 26.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = palette.text,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.2).sp,
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    color = palette.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            AppIdentity(palette)
            Spacer(Modifier.height(31.dp))
            Text(
                text = stringResource(R.string.settings_preferences),
                color = palette.secondaryText,
                modifier = Modifier.padding(start = 7.dp, bottom = 12.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            )
            Column(
                modifier = Modifier.fillMaxWidth().background(palette.card, RoundedCornerShape(25.dp)),
            ) {
                PreferenceRow(
                    title = stringResource(R.string.settings_dark_theme),
                    subtitle = stringResource(R.string.settings_dark_theme_description),
                    checked = darkTheme,
                    onCheckedChange = onDarkThemeChange,
                    interactive = interactive,
                    palette = palette,
                    moonIcon = true,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp, end = 20.dp),
                    color = palette.divider,
                )
                PreferenceRow(
                    title = stringResource(R.string.settings_haptics),
                    subtitle = stringResource(R.string.settings_haptics_description),
                    checked = hapticsEnabled,
                    onCheckedChange = onHapticsChange,
                    interactive = interactive,
                    palette = palette,
                    moonIcon = false,
                )
            }
            Text(
                text = stringResource(R.string.settings_pull_hint),
                color = palette.secondaryText.copy(alpha = .8f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 25.dp, bottom = 32.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AppIdentity(palette: NewsPalette) {
    Row(
        modifier =
            Modifier.fillMaxWidth().background(palette.card, RoundedCornerShape(25.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(58.dp).background(palette.avatarBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_app_initial),
                color = palette.avatarText,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(R.string.settings_app_name),
                color = palette.text,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.settings_app_subtitle),
                color = palette.secondaryText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    interactive: Boolean,
    palette: NewsPalette,
    moonIcon: Boolean,
) {
    val rowModifier =
        if (interactive) {
            Modifier
                .testTag(if (moonIcon) "settings_dark_theme" else "settings_haptics")
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
        } else {
            Modifier
        }
    Row(
        modifier =
            Modifier.fillMaxWidth().then(rowModifier).heightIn(min = 96.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PreferenceIcon(moon = moonIcon, palette = palette)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = palette.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = palette.secondaryText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = palette.checkedThumb,
                    checkedTrackColor = palette.accent,
                    checkedBorderColor = NewsColors.transparent,
                    uncheckedThumbColor = palette.uncheckedThumb,
                    uncheckedTrackColor = palette.uncheckedTrack,
                    uncheckedBorderColor = NewsColors.transparent,
                ),
        )
    }
}

@Composable
private fun PreferenceIcon(moon: Boolean, palette: NewsPalette) {
    Canvas(modifier = Modifier.size(44.dp).background(palette.iconBackground, CircleShape)) {
        val center = this.center
        val unit = size.width / 44f
        if (moon) {
            drawCircle(color = palette.accent, radius = 10f * unit, center = center)
            drawCircle(
                color = palette.iconBackground,
                radius = 9f * unit,
                center = center + Offset(5f * unit, -4f * unit),
            )
        } else {
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(16f * unit, 12f * unit),
                size = Size(12f * unit, 20f * unit),
                cornerRadius = CornerRadius(2.5f * unit),
                style = Stroke(width = 1.8f * unit, cap = StrokeCap.Round),
            )
            drawLine(
                color = palette.accent,
                start = Offset(20f * unit, 28.5f * unit),
                end = Offset(24f * unit, 28.5f * unit),
                strokeWidth = 1.8f * unit,
                cap = StrokeCap.Round,
            )
            val vibration = Path().apply {
                moveTo(11f * unit, 15f * unit)
                lineTo(8.5f * unit, 18.5f * unit)
                lineTo(11f * unit, 22f * unit)
                lineTo(8.5f * unit, 25.5f * unit)
                lineTo(11f * unit, 29f * unit)
                moveTo(33f * unit, 15f * unit)
                lineTo(35.5f * unit, 18.5f * unit)
                lineTo(33f * unit, 22f * unit)
                lineTo(35.5f * unit, 25.5f * unit)
                lineTo(33f * unit, 29f * unit)
            }
            drawPath(vibration, palette.accent, style = Stroke(1.8f * unit, cap = StrokeCap.Round))
        }
    }
}

private val LampRegionHeight = 244.dp

@Preview(name = "Settings · light", showBackground = true)
@Composable
private fun SettingsLightPreview() {
    NewsTheme(darkTheme = false) {
        SettingsContent(darkTheme = false, hapticsEnabled = true, onDarkThemeChange = {}, onHapticsChange = {})
    }
}

@Preview(name = "Settings · dark", showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    NewsTheme(darkTheme = true) {
        SettingsContent(darkTheme = true, hapticsEnabled = true, onDarkThemeChange = {}, onHapticsChange = {})
    }
}
