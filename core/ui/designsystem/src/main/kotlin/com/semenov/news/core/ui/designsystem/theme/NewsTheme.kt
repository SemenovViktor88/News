package com.semenov.news.core.ui.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalNewsPalette = staticCompositionLocalOf { LightNewsPalette }
private val LightNewsColorScheme = newsColorScheme(LightNewsPalette, DarkNewsPalette)
private val DarkNewsColorScheme = newsColorScheme(DarkNewsPalette, LightNewsPalette)

object NewsTheme {
    val colors: NewsPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current
}

@Composable
fun NewsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalNewsPalette provides newsPalette(darkTheme)) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkNewsColorScheme else LightNewsColorScheme,
            typography = NewsTypography,
            shapes = NewsShapes,
            content = content,
        )
    }
}

/** Explicitly maps every Material role so component defaults keep the app's palette. */
private fun newsColorScheme(palette: NewsPalette, inverse: NewsPalette): ColorScheme =
    ColorScheme(
        primary = palette.accent,
        onPrimary = palette.checkedThumb,
        primaryContainer = palette.avatarBackground,
        onPrimaryContainer = palette.avatarText,
        inversePrimary = inverse.accent,
        secondary = palette.accent,
        onSecondary = palette.checkedThumb,
        secondaryContainer = palette.iconBackground,
        onSecondaryContainer = palette.text,
        tertiary = palette.accent,
        onTertiary = palette.checkedThumb,
        tertiaryContainer = palette.avatarBackground,
        onTertiaryContainer = palette.avatarText,
        background = palette.background,
        onBackground = palette.text,
        surface = palette.card,
        onSurface = palette.text,
        surfaceVariant = palette.iconBackground,
        onSurfaceVariant = palette.secondaryText,
        surfaceTint = palette.accent,
        inverseSurface = inverse.card,
        inverseOnSurface = inverse.text,
        error = palette.error,
        onError = palette.onError,
        errorContainer = palette.errorContainer,
        onErrorContainer = palette.onErrorContainer,
        outline = palette.secondaryText,
        outlineVariant = palette.divider,
        scrim = Color.Black,
        surfaceBright = palette.card,
        surfaceDim = palette.background,
        surfaceContainer = palette.card,
        surfaceContainerHigh = palette.iconBackground,
        surfaceContainerHighest = palette.uncheckedTrack,
        surfaceContainerLow = palette.background,
        surfaceContainerLowest = palette.background,
        primaryFixed = LightNewsPalette.avatarBackground,
        primaryFixedDim = LightNewsPalette.uncheckedTrack,
        onPrimaryFixed = LightNewsPalette.text,
        onPrimaryFixedVariant = LightNewsPalette.accent,
        secondaryFixed = LightNewsPalette.iconBackground,
        secondaryFixedDim = LightNewsPalette.avatarBackground,
        onSecondaryFixed = LightNewsPalette.text,
        onSecondaryFixedVariant = LightNewsPalette.accent,
        tertiaryFixed = LightNewsPalette.avatarBackground,
        tertiaryFixedDim = LightNewsPalette.uncheckedTrack,
        onTertiaryFixed = LightNewsPalette.text,
        onTertiaryFixedVariant = LightNewsPalette.accent,
    )
