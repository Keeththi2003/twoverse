package app.twoverse.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.component.TwoversePreviewBackground
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val NeedleSize = 18.dp

/** Needle path from Home.dc.html, in a 24 × 24 viewport: tip half and tail half. */
private const val NeedleViewport = 24f

/** A small compass needle pointing along her bearing from north (degrees), for the distance card. */
@Composable
internal fun DirectionNeedle(bearingDegrees: Float, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    Canvas(modifier = modifier.size(NeedleSize)) {
        val scale = size.width / NeedleViewport
        fun needleHalf(tipY: Float, innerY: Float) = Path().apply {
            moveTo(12f * scale, tipY * scale)
            lineTo(15f * scale, 12f * scale)
            lineTo(12f * scale, innerY * scale)
            lineTo(9f * scale, 12f * scale)
            close()
        }
        rotate(bearingDegrees) {
            drawPath(needleHalf(tipY = 3f, innerY = 10.5f), color = colors.accent)
            drawPath(needleHalf(tipY = 21f, innerY = 13.5f), color = colors.onSurfaceVariant)
        }
    }
}

@PreviewLightDark
@Composable
private fun DirectionNeedlePreview() {
    TwoversePreviewBackground { DirectionNeedle(bearingDegrees = 315f) }
}
