package app.twoverse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.CardShadowColor
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val CardShadow = Shadow(
    radius = 24.dp,
    color = CardShadowColor,
    offset = DpOffset(x = 0.dp, y = 8.dp),
)

/** Card depth from DESIGN.md: soft shadow in light theme, 1dp outline border in dark theme. */
@Composable
fun Modifier.twoverseCardDepth(shape: Shape): Modifier {
    val colors = TwoverseTheme.colors
    return if (colors.isDark) {
        border(width = 1.dp, color = colors.outline, shape = shape)
    } else {
        dropShadow(shape = shape, shadow = CardShadow)
    }
}

@Composable
fun TwoverseCard(
    modifier: Modifier = Modifier,
    shape: Shape = TwoverseTheme.shapes.card,
    color: Color = TwoverseTheme.colors.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .twoverseCardDepth(shape)
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

@PreviewLightDark
@Composable
private fun TwoverseCardPreview() {
    TwoversePreviewBackground {
        TwoverseCard {
            Text(
                text = "Ours",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(TwoverseTheme.spacing.lg),
            )
        }
    }
}
