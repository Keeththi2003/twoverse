package app.twoverse.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseTheme

internal val MiniCompassBorder = 1.5.dp
private val DialSize = 48.dp
private val NeedleSize = 30.dp

/** Needle path from Home.dc.html, in a 24 × 24 viewport: tip half and tail half. */
private const val NeedleViewport = 24f

/**
 * Small compass on the Your Star tile; the needle points toward the partner. The angle is read
 * while drawing, so live sensor updates only redraw this needle.
 */
@Composable
internal fun MiniCompass(needleRotation: () -> Float?, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    Box(modifier = modifier.size(DialSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(NeedleSize)) {
            val rotation = needleRotation() ?: return@Canvas
            val scale = size.width / NeedleViewport
            fun needleHalf(tipY: Float, innerY: Float) = Path().apply {
                moveTo(12f * scale, tipY * scale)
                lineTo(15f * scale, 12f * scale)
                lineTo(12f * scale, innerY * scale)
                lineTo(9f * scale, 12f * scale)
                close()
            }
            rotate(rotation) {
                drawPath(needleHalf(tipY = 3f, innerY = 10.5f), color = colors.accent)
                drawPath(needleHalf(tipY = 21f, innerY = 13.5f), color = colors.outline)
            }
        }
    }
}
