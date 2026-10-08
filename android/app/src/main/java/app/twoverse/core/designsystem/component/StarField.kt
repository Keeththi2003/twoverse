package app.twoverse.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseTheme
import kotlin.math.PI
import kotlin.math.cos

/** A background star. [x] and [y] are fractions of the field size. */
@Immutable
data class Star(
    val x: Float,
    val y: Float,
    val size: Dp,
    val gold: Boolean,
)

/** Star layout from the Splash mockup: (top %, left %, size px), every third star gold. */
val DefaultStars: List<Star> = listOf(
    Triple(8, 12, 3), Triple(18, 78, 2), Triple(26, 40, 4), Triple(14, 55, 2),
    Triple(33, 88, 3), Triple(62, 9, 2), Triple(70, 85, 4), Triple(78, 22, 3),
    Triple(85, 64, 2), Triple(90, 38, 3), Triple(48, 94, 2), Triple(55, 5, 3),
    Triple(40, 16, 2), Triple(74, 50, 2),
).mapIndexed { index, (top, left, size) ->
    Star(x = left / 100f, y = top / 100f, size = size.dp, gold = index % 3 == 0)
}

private const val TwinkleMillis = 3_200
private const val TwinkleStaggerMillis = 370
private const val MinTwinkleAlpha = 0.25f
private const val StaticBaseAlpha = 0.4f
private const val StaticAlphaStep = 0.15f

/**
 * Scattered gold and soft stars. With `twinkle = true` each star fades between 25 % and
 * 100 % opacity every 3.2 s, staggered; otherwise they rest at fixed opacities.
 */
@Composable
fun StarField(
    modifier: Modifier = Modifier,
    stars: List<Star> = DefaultStars,
    twinkle: Boolean = true,
) {
    val colors = TwoverseTheme.colors
    val animate = twinkle && rememberAnimationsEnabled()
    val progress = twinkleProgress(animate)

    Canvas(modifier = modifier) {
        stars.forEachIndexed { index, star ->
            val alpha = if (twinkle) {
                val phase = progress.value + index * TwinkleStaggerMillis / TwinkleMillis.toFloat()
                val wave = (1 - cos(2 * PI * phase).toFloat()) / 2
                MinTwinkleAlpha + (1 - MinTwinkleAlpha) * wave
            } else {
                StaticBaseAlpha + (index % 4) * StaticAlphaStep
            }
            drawCircle(
                color = if (star.gold) colors.gold else colors.starSoft,
                radius = star.size.toPx() / 2,
                center = Offset(star.x * size.width, star.y * size.height),
                alpha = alpha,
            )
        }
    }
}

@Composable
private fun twinkleProgress(twinkle: Boolean): State<Float> {
    if (!twinkle) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TwinkleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "twinkleProgress",
    )
}

@PreviewLightDark
@Composable
private fun StarFieldPreview() {
    TwoversePreviewBackground {
        StarField(
            twinkle = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )
    }
}
