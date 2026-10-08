package app.twoverse.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.component.GoldStar
import app.twoverse.core.designsystem.component.Planet
import app.twoverse.core.designsystem.component.PlanetKind
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val ArcHeight = 70.dp
private val HerSize = 40.dp
private val YouSize = 30.dp
private val StarSize = 12.dp
private val StarTop = 14.dp
private val HerOffset = 4.dp
private val HerTop = 30.dp
private val YouOffset = 6.dp
private val YouTop = 36.dp
private val ArcStroke = 2.dp
private val ArcDash = 4.dp
private val ArcGap = 6.dp

/** Mockup geometry: the arc runs from (24, 50) over (153, -10) to (282, 50) in a 306 × 70 box. */
private const val ArcViewportWidth = 306f
private const val ArcViewportHeight = 70f
private const val ArcStartX = 24f
private const val ArcEndY = 50f
private const val ArcControlY = -10f

/**
 * Your planet (lavender, left, above "You") and hers (rose, right, above "Her") joined by a dashed arc,
 * with the gold star above the middle (Home, DESIGN.md §1).
 */
@Composable
internal fun DistanceArc(modifier: Modifier = Modifier) {
    val outline = TwoverseTheme.colors.outline
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ArcHeight),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / ArcViewportWidth
            val scaleY = size.height / ArcViewportHeight
            val path = Path().apply {
                moveTo(ArcStartX * scaleX, ArcEndY * scaleY)
                quadraticTo(
                    size.width / 2,
                    ArcControlY * scaleY,
                    size.width - ArcStartX * scaleX,
                    ArcEndY * scaleY,
                )
            }
            drawPath(
                path = path,
                color = outline,
                style = Stroke(
                    width = ArcStroke.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(ArcDash.toPx(), ArcGap.toPx())),
                ),
            )
        }
        GoldStar(
            size = StarSize,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = StarTop),
        )
        Planet(
            kind = PlanetKind.You,
            size = YouSize,
            modifier = Modifier.offset(x = YouOffset, y = YouTop),
        )
        Planet(
            kind = PlanetKind.Her,
            size = HerSize,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = -HerOffset, y = HerTop),
        )
    }
}
