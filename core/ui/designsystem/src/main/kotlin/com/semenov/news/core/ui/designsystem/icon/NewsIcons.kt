package com.semenov.news.core.ui.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Shared icon artwork. Screens provide the size, tint, background, and accessibility label. */
object NewsIcons {
    val Home: ImageVector
        get() = Icons.Outlined.Home

    val Categories: ImageVector
        get() = Icons.AutoMirrored.Outlined.List

    val Settings: ImageVector
        get() = Icons.Outlined.Settings

    val Search: ImageVector by lazy {
        ImageVector.Builder("News.Search", 22.dp, 22.dp, 22f, 22f).apply {
            path(fill = null, stroke = IconStroke, strokeLineWidth = 1.8f) {
                moveTo(15.5f, 9f)
                arcTo(6.5f, 6.5f, 0f, true, true, 2.5f, 9f)
                arcTo(6.5f, 6.5f, 0f, true, true, 15.5f, 9f)
                close()
            }
            path(fill = null, stroke = IconStroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                moveTo(14f, 14f)
                lineTo(19f, 19f)
            }
        }.build()
    }

    val DarkTheme: ImageVector by lazy {
        ImageVector.Builder("News.DarkTheme", 44.dp, 44.dp, 44f, 44f).apply {
            // Circle (22, 22), radius 10, with circle (27, 18), radius 9, removed.
            path(fill = IconStroke) {
                moveTo(20.139654f, 12.174568f)
                arcTo(10f, 10f, 0f, true, false, 31.17742f, 25.971773f)
                arcTo(9f, 9f, 0f, false, true, 20.139654f, 12.174568f)
                close()
            }
        }.build()
    }

    val HapticFeedback: ImageVector by lazy {
        ImageVector.Builder("News.HapticFeedback", 44.dp, 44.dp, 44f, 44f).apply {
            path(fill = null, stroke = IconStroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                moveTo(18.5f, 12f)
                lineTo(25.5f, 12f)
                arcTo(2.5f, 2.5f, 0f, false, true, 28f, 14.5f)
                lineTo(28f, 29.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 25.5f, 32f)
                lineTo(18.5f, 32f)
                arcTo(2.5f, 2.5f, 0f, false, true, 16f, 29.5f)
                lineTo(16f, 14.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 18.5f, 12f)
                close()
                moveTo(20f, 28.5f)
                lineTo(24f, 28.5f)
                moveTo(11f, 15f)
                lineTo(8.5f, 18.5f)
                lineTo(11f, 22f)
                lineTo(8.5f, 25.5f)
                lineTo(11f, 29f)
                moveTo(33f, 15f)
                lineTo(35.5f, 18.5f)
                lineTo(33f, 22f)
                lineTo(35.5f, 25.5f)
                lineTo(33f, 29f)
            }
        }.build()
    }

    val Logout: ImageVector by lazy {
        ImageVector.Builder("News.Logout", 26.dp, 26.dp, 26f, 26f).apply {
            path(
                fill = null,
                stroke = IconStroke,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                lineTo(4f, 3f)
                lineTo(4f, 23f)
                lineTo(12f, 23f)
                moveTo(17f, 7f)
                lineTo(23f, 13f)
                lineTo(17f, 19f)
                moveTo(10f, 13f)
                lineTo(23f, 13f)
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder("News.Close", 18.dp, 18.dp, 18f, 18f).apply {
            path(fill = null, stroke = IconStroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                moveTo(3f, 3f)
                lineTo(15f, 15f)
                moveTo(15f, 3f)
                lineTo(3f, 15f)
            }
        }.build()
    }
}

private val IconStroke = SolidColor(Color.Black)
