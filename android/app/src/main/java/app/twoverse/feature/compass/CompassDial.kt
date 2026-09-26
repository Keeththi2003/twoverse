package app.twoverse.feature.compass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseGradients
import app.twoverse.core.designsystem.theme.TwoverseTheme

/** All geometry below is in the 300 × 300 viewport of Compass.dc.html. */
private const val Viewport = 300f
private const val Center = 150f
private const val FaceRadius = 146f
private const val FaceStroke = 1.5f
private const val TickRadius = 126f
private const val TickLength = 10f
private const val TickWidth = 1.5f
private const val TickCount = 72
private const val TickAlpha = 0.45f
private const val InnerRingRadius = 100f
private const val InnerDash = 3f
private const val InnerGap = 5f
private const val NorthBaselineY = 50f
private const val SouthBaselineY = 258f
private const val SideBaselineY = 155f
private const val EastX = 252f
private const val WestX = 48f
private const val NeedleTipY = 64f
private const val NeedleTailY = 236f
private const val NeedleHalfWidth = 12f
private const val NeedleNotch = 10f
private const val NeedleBallY = 62f
private const val NeedleBallRadius = 12f
private const val ShineOffset = 4f
private const val ShineRadius = 4f
private const val ShineAlpha = 0.55f
private const val HubRadius = 11f
private const val HubShineOffset = 3f
private const val FullCircle = 360f

/**
 * Compass dial with 72 ticks, N/E/S/W and a needle rotated by [needleRotation] degrees
 * (clockwise from the top). A null rotation hides the needle.
 */
@Composable
internal fun CompassDial(
    needleRotation: Float?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val cardinalStyle = MaterialTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant)
    val northStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = colors.primary)
    val north = stringResource(R.string.direction_letter_north)
    val east = stringResource(R.string.direction_letter_east)
    val south = stringResource(R.string.direction_letter_south)
    val west = stringResource(R.string.direction_letter_west)
    val rotation by animateFloatAsState(targetValue = needleRotation ?: 0f, label = "needleRotation")

    Canvas(modifier = modifier.semantics { this.contentDescription = contentDescription }) {
        val scale = size.minDimension / Viewport
        fun p(x: Float, y: Float) = Offset(x * scale, y * scale)
        val center = p(Center, Center)

        drawCircle(color = colors.surface, radius = FaceRadius * scale, center = center)
        drawCircle(
            color = colors.outline,
            radius = FaceRadius * scale,
            center = center,
            style = Stroke(width = FaceStroke * scale),
        )
        repeat(TickCount) { index ->
            rotate(degrees = index * FullCircle / TickCount, pivot = center) {
                drawLine(
                    color = colors.onSurfaceVariant.copy(alpha = TickAlpha),
                    start = p(Center, Center - TickRadius - TickLength / 2),
                    end = p(Center, Center - TickRadius + TickLength / 2),
                    strokeWidth = TickWidth * scale,
                )
            }
        }
        drawCircle(
            color = colors.outline,
            radius = InnerRingRadius * scale,
            center = center,
            style = Stroke(
                width = scale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(InnerDash * scale, InnerGap * scale)),
            ),
        )

        drawLabel(textMeasurer, north, p(Center, NorthBaselineY), northStyle)
        drawLabel(textMeasurer, east, p(EastX, SideBaselineY), cardinalStyle)
        drawLabel(textMeasurer, south, p(Center, SouthBaselineY), cardinalStyle)
        drawLabel(textMeasurer, west, p(WestX, SideBaselineY), cardinalStyle)

        if (needleRotation != null) {
            rotate(degrees = rotation, pivot = center) {
                drawPath(needleHalf(scale, tipY = NeedleTipY, notchY = Center - NeedleNotch), color = colors.accent)
                drawPath(needleHalf(scale, tipY = NeedleTailY, notchY = Center + NeedleNotch), color = colors.outline)
                drawCircle(color = colors.accent, radius = NeedleBallRadius * scale, center = p(Center, NeedleBallY))
                drawCircle(
                    color = TwoverseGradients.Shine.copy(alpha = ShineAlpha),
                    radius = ShineRadius * scale,
                    center = p(Center - ShineOffset, NeedleBallY - ShineOffset),
                )
            }
        }
        drawCircle(color = colors.gold, radius = HubRadius * scale, center = center)
        drawCircle(
            color = TwoverseGradients.Gold.highlight,
            radius = ShineRadius * scale,
            center = p(Center - HubShineOffset, Center - HubShineOffset),
        )
    }
}

private fun needleHalf(scale: Float, tipY: Float, notchY: Float) = Path().apply {
    moveTo(Center * scale, tipY * scale)
    lineTo((Center + NeedleHalfWidth) * scale, Center * scale)
    lineTo(Center * scale, notchY * scale)
    lineTo((Center - NeedleHalfWidth) * scale, Center * scale)
    close()
}

/** Draws [text] centred horizontally on [anchor], with its baseline at [anchor].y. */
private fun DrawScope.drawLabel(textMeasurer: TextMeasurer, text: String, anchor: Offset, style: TextStyle) {
    val layout = textMeasurer.measure(text, style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(anchor.x - layout.size.width / 2f, anchor.y - layout.firstBaseline),
    )
}
