package com.manoogianmedia.studiorack.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object MetronomeIcons {
    val Standard: ImageVector by lazy { buildMetronomeIcon("Metronome") }
    val Sound: ImageVector by lazy { buildMetronomeIcon("MetronomeSound", sound = true) }
    val Muted: ImageVector by lazy { buildMetronomeIcon("MetronomeMuted", muted = true) }

    private fun buildMetronomeIcon(name: String, sound: Boolean = false, muted: Boolean = false): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 3f)
                lineTo(15f, 3f)
                lineTo(18f, 21f)
                lineTo(5f, 21f)
                close()
                moveTo(11.5f, 17f)
                lineTo(14.5f, 6f)
                moveTo(13.7f, 9f)
                lineTo(16.2f, 9.7f)
                moveTo(7.5f, 15f)
                lineTo(9.5f, 15f)
                moveTo(7f, 18f)
                lineTo(9f, 18f)
            }
            if (sound) {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(19f, 8.5f)
                    curveTo(21f, 10f, 21f, 14f, 19f, 15.5f)
                    moveTo(21f, 6.5f)
                    curveTo(24f, 9f, 24f, 15f, 21f, 17.5f)
                }
            }
            if (muted) {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2.2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(4f, 4f)
                    lineTo(20f, 20f)
                }
            }
        }.build()
}
