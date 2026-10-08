package app.twoverse.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val DotSize = 8.dp
private val HorizontalPadding = 12.dp
private val DotGap = 6.dp
private val IconSize = 15.dp

/**
 * Non-interactive status pill with an optional coloured dot ("Location on") or icon ("Expires in 2 days").
 */
@Composable
fun TwoverseStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    @DrawableRes icon: Int? = null,
    containerColor: Color = TwoverseTheme.colors.chip,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    val spacing = TwoverseTheme.spacing
    Row(
        modifier = modifier
            .background(containerColor, TwoverseTheme.shapes.circle)
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
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = TwoverseTheme.colors.onSurface,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = text,
            style = textStyle,
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
            TwoverseStatusChip(text = "Ammu's location · 12s ago")
        }
    }
}
