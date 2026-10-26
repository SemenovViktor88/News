package com.semenov.news.core.ui.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NewsTypography =
    Typography(
        displayLarge = newsTextStyle(56, 64, FontWeight.Bold, -1.5f),
        displayMedium = newsTextStyle(45, 52, FontWeight.Bold, -1.2f),
        displaySmall = newsTextStyle(40, 46, FontWeight.Bold, -1.2f),
        headlineLarge = newsTextStyle(36, 42, FontWeight.Bold, -1.2f),
        headlineMedium = newsTextStyle(30, 36, FontWeight.Bold, -.8f),
        headlineSmall = newsTextStyle(24, 30, FontWeight.SemiBold, -.4f),
        titleLarge = newsTextStyle(22, 28, FontWeight.SemiBold, -.3f),
        titleMedium = newsTextStyle(19, 25, FontWeight.SemiBold, -.2f),
        titleSmall = newsTextStyle(15, 22, FontWeight.SemiBold),
        bodyLarge = newsTextStyle(16, 24),
        bodyMedium = newsTextStyle(14, 21),
        bodySmall = newsTextStyle(12, 18),
        labelLarge = newsTextStyle(14, 20, FontWeight.SemiBold),
        labelMedium = newsTextStyle(12, 16, FontWeight.SemiBold, .2f),
        labelSmall = newsTextStyle(11, 16, FontWeight.SemiBold, 1.5f),
    )

private fun newsTextStyle(
    fontSize: Int,
    lineHeight: Int,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f,
): TextStyle =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = fontWeight,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
    )
