package com.semenov.news.core.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colors shared by every screen, including both layers of the settings theme reveal. */
@Immutable
data class NewsPalette(
    val background: Color,
    val card: Color,
    val text: Color,
    val secondaryText: Color,
    val accent: Color,
    val iconBackground: Color,
    val avatarBackground: Color,
    val avatarText: Color,
    val divider: Color,
    val checkedThumb: Color,
    val uncheckedThumb: Color,
    val uncheckedTrack: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
)

internal val LightNewsPalette =
    NewsPalette(
        background = Color(0xFFF3F2EB),
        card = Color(0xFFFFFEF9),
        text = Color(0xFF23352B),
        secondaryText = Color(0xFF637166),
        accent = Color(0xFF36634A),
        iconBackground = Color(0xFFEEF1E7),
        avatarBackground = Color(0xFFE3E9D9),
        avatarText = Color(0xFF36634A),
        divider = Color(0xFFEEEEE5),
        checkedThumb = Color(0xFFF8F9F1),
        uncheckedThumb = Color(0xFFFFFEF9),
        uncheckedTrack = Color(0xFFD8DFD3),
        error = Color(0xFFA13F36),
        onError = Color(0xFFFFF8F3),
        errorContainer = Color(0xFFF6DFD7),
        onErrorContainer = Color(0xFF63251F),
    )

internal val DarkNewsPalette =
    NewsPalette(
        background = Color(0xFF10191F),
        card = Color(0xFF1B272E),
        text = Color(0xFFF3F1E8),
        secondaryText = Color(0xFFA6B0AD),
        accent = Color(0xFFE3C780),
        iconBackground = Color(0xFF303B38),
        avatarBackground = Color(0xFF334B3C),
        avatarText = Color(0xFFE3C780),
        divider = Color(0xFF2A363C),
        checkedThumb = Color(0xFF273A2E),
        uncheckedThumb = Color(0xFF98A39E),
        uncheckedTrack = Color(0xFF36443F),
        error = Color(0xFFEDB4A6),
        onError = Color(0xFF57241F),
        errorContainer = Color(0xFF68362E),
        onErrorContainer = Color(0xFFFFDAD1),
    )

/** A pure lookup also usable while drawing the outgoing and incoming theme at once. */
fun newsPalette(darkTheme: Boolean): NewsPalette =
    if (darkTheme) DarkNewsPalette else LightNewsPalette

object NewsColors {
    val shade = Color(0xFF31574A)
    val brass = Color(0xFFB89A5D)
    val beam = Color(0xFFFFD592)
    val lightMask = Color.White
    val bulbOn = Color(0xFFFFF6DC)
    val bulbOff = Color(0xFFECE6D4)
    val bulbGlow = Color(0xFFFFF6DC)
    val shadow = Color.Black
    val cordOn = Color(0xFFD5BE84)
    val cordOff = Color(0xFF9B855B)
    val focus = Color(0xFF7B997A)
    val handleShine = Color(0xFFE2D2AF)
    val transparent = Color.Transparent
}
