package app.twoverse.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseGradients
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.designsystem.theme.toBrush

/** Rose planet is her (the partner), lavender planet is you. */
enum class PlanetKind { Her, You }

private const val PlanetShadowAlpha = 0.28f
private const val PlanetShadowBlurRatio = 0.45f
private const val PlanetShadowOffsetRatio = 0.2f
private const val StarGlowAlpha = 0.42f
private const val StarGlowBlurRatio = 1.3f
private const val StarGlowSpreadRatio = 0.35f

@Composable
fun Planet(
    kind: PlanetKind,
    size: Dp,
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
) {
    val stops = when (kind) {
        PlanetKind.Her -> TwoverseGradients.Rose
        PlanetKind.You -> TwoverseGradients.Lavender
    }
    val shadowModifier = if (elevated) {
        val shadowColor = when (kind) {
            PlanetKind.Her -> TwoverseGradients.RoseShadow
            PlanetKind.You -> TwoverseGradients.LavenderShadow
        }
        Modifier.dropShadow(
            shape = CircleShape,
            shadow = Shadow(
                radius = size * PlanetShadowBlurRatio,
                color = shadowColor,
                offset = DpOffset(x = 0.dp, y = size * PlanetShadowOffsetRatio),
                alpha = PlanetShadowAlpha,
            ),
        )
    } else {
        Modifier
    }
    Canvas(
        modifier = modifier
            .size(size)
            .then(shadowModifier),
    ) {
        drawCircle(brush = stops.toBrush(this.size))
    }
}

@Composable
fun GoldStar(
    size: Dp,
    modifier: Modifier = Modifier,
    glow: Boolean = true,
) {
    val glowModifier = if (glow) {
        Modifier.dropShadow(
            shape = CircleShape,
            shadow = Shadow(
                radius = size * StarGlowBlurRatio,
                color = TwoverseGradients.GoldGlow,
                spread = size * StarGlowSpreadRatio,
                alpha = StarGlowAlpha,
            ),
        )
    } else {
        Modifier
    }
    Canvas(
        modifier = modifier
            .size(size)
            .then(glowModifier),
    ) {
        drawCircle(brush = TwoverseGradients.Gold.toBrush(this.size))
    }
}

@PreviewLightDark
@Composable
private fun PlanetPreview() {
    TwoversePreviewBackground {
        Row(
            modifier = Modifier.padding(TwoverseTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Planet(kind = PlanetKind.Her, size = 48.dp, elevated = true)
            Planet(kind = PlanetKind.You, size = 36.dp, elevated = true)
            GoldStar(size = 22.dp)
            Planet(kind = PlanetKind.Her, size = 30.dp)
        }
    }
}
