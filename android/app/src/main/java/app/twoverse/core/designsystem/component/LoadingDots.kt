package app.twoverse.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseTheme

private const val DotCount = 3
private const val HalfPulseMillis = 700
private const val DotDelayMillis = 200
private const val MinAlpha = 0.3f
private const val MinScale = 0.8f
private val DotSize = 7.dp

/** Three dots pulsing in turn (Splash). [contentDescription] is read by TalkBack. */
@Composable
fun LoadingDots(
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "loadingDots")
    Row(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
    ) {
        repeat(DotCount) { index ->
            val pulse by transition.pulse(index)
            Box(
                modifier = Modifier
                    .size(DotSize)
                    .graphicsLayer {
                        alpha = MinAlpha + (1 - MinAlpha) * pulse
                        scaleX = MinScale + (1 - MinScale) * pulse
                        scaleY = scaleX
                    }
                    .background(color, TwoverseTheme.shapes.circle),
            )
        }
    }
}

@Composable
private fun InfiniteTransition.pulse(index: Int) = animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = HalfPulseMillis, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
        initialStartOffset = StartOffset(index * DotDelayMillis),
    ),
    label = "dot$index",
)

@PreviewLightDark
@Composable
private fun LoadingDotsPreview() {
    TwoversePreviewBackground {
        LoadingDots(color = TwoverseTheme.colors.accent, contentDescription = "Loading")
    }
}
