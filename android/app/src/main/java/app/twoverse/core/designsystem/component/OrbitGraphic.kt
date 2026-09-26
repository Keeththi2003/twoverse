package app.twoverse.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class OrbitRing { Outer, Inner }

/** Where a planet sits: degrees clockwise from the top, on the outer or inner ring. */
@Immutable
data class OrbitPosition(
    val angleDegrees: Float,
    val ring: OrbitRing = OrbitRing.Outer,
)

private const val InnerRingRatio = 0.62f
private const val FullTurnMillis = 28_000
private val OuterRingStroke = 1.5.dp
private val InnerRingStroke = 1.dp
private val InnerRingDash = 4.dp

/**
 * Rings, gold star and both planets. `animated = true` spins the planets around the star
 * once every 28 s (Splash). Planets are drawn upright, so their highlight never rotates.
 */
@Composable
fun OrbitGraphic(
    modifier: Modifier = Modifier,
    size: Dp = 232.dp,
    her: OrbitPosition = OrbitPosition(angleDegrees = 0f),
    you: OrbitPosition = OrbitPosition(angleDegrees = 180f),
    herSize: Dp = 48.dp,
    youSize: Dp = 36.dp,
    starSize: Dp = 18.dp,
    animated: Boolean = false,
    showInnerRing: Boolean = true,
    showShadows: Boolean = true,
) {
    val outline = TwoverseTheme.colors.outline
    val rotation = orbitRotation(animated)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = this.size.minDimension / 2
            drawCircle(
                color = outline,
                radius = outerRadius - OuterRingStroke.toPx() / 2,
                style = Stroke(width = OuterRingStroke.toPx()),
            )
            if (!showInnerRing) return@Canvas
            drawCircle(
                color = outline,
                radius = outerRadius * InnerRingRatio,
                style = Stroke(
                    width = InnerRingStroke.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(InnerRingDash.toPx(), InnerRingDash.toPx()),
                    ),
                ),
            )
        }
        GoldStar(size = starSize, glow = showShadows)
        Planet(
            kind = PlanetKind.Her,
            size = herSize,
            elevated = showShadows,
            modifier = Modifier.offset { orbitOffset(her, rotation.value, size) },
        )
        Planet(
            kind = PlanetKind.You,
            size = youSize,
            elevated = showShadows,
            modifier = Modifier.offset { orbitOffset(you, rotation.value, size) },
        )
    }
}

@Composable
private fun orbitRotation(animated: Boolean): State<Float> {
    if (!animated) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "orbit").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FullTurnMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbitRotation",
    )
}

private fun Density.orbitOffset(position: OrbitPosition, rotation: Float, size: Dp): IntOffset {
    val outerRadius = size.toPx() / 2
    val radius = when (position.ring) {
        OrbitRing.Outer -> outerRadius
        OrbitRing.Inner -> outerRadius * InnerRingRatio
    }
    val radians = Math.toRadians((position.angleDegrees + rotation).toDouble())
    return IntOffset(
        x = (radius * sin(radians)).roundToInt(),
        y = (-radius * cos(radians)).roundToInt(),
    )
}

@PreviewLightDark
@Composable
private fun OrbitGraphicPreview() {
    TwoversePreviewBackground {
        Row(
            modifier = Modifier.padding(TwoverseTheme.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitGraphic(size = 180.dp)
            OrbitGraphic(
                size = 180.dp,
                her = OrbitPosition(angleDegrees = 30f),
                you = OrbitPosition(angleDegrees = 240f, ring = OrbitRing.Inner),
                herSize = 36.dp,
                youSize = 24.dp,
            )
        }
    }
}
