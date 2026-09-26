package app.twoverse.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

private const val CornerRatio = 3

/** Decorative icon on a chip-coloured rounded square (48dp / 16dp radius by default, DESIGN.md §4). */
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(TwoverseTheme.colors.chip, RoundedCornerShape(size / CornerRatio)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = TwoverseTheme.colors.primary,
            modifier = Modifier.size(iconSize),
        )
    }
}

@PreviewLightDark
@Composable
private fun IconTilePreview() {
    TwoversePreviewBackground {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = R.drawable.ic_calendar)
            IconTile(icon = R.drawable.ic_pin, size = 36.dp, iconSize = 18.dp)
        }
    }
}
