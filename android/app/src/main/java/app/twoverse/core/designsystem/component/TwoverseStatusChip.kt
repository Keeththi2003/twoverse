package app.twoverse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val DotSize = 8.dp
private val HorizontalPadding = 12.dp
private val DotGap = 6.dp

/** Non-interactive status pill with an optional coloured dot ("Location on", "Compass calibrated"). */
@Composable
fun TwoverseStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    val spacing = TwoverseTheme.spacing
    Row(
        modifier = modifier
            .background(TwoverseTheme.colors.chip, TwoverseTheme.shapes.circle)
            .padding(horizontal = HorizontalPadding, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DotGap),
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(DotSize)
                    .background(dotColor, TwoverseTheme.shapes.circle),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TwoverseTheme.colors.onSurface,
        )
    }
}

@PreviewLightDark
@Composable
private fun TwoverseStatusChipPreview() {
    TwoversePreviewBackground {
        Row(horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs)) {
            TwoverseStatusChip(text = "Location on", dotColor = TwoverseTheme.colors.gold)
            TwoverseStatusChip(text = "Her location · 12s ago")
        }
    }
}
