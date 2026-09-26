package app.twoverse.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val ButtonSize = 44.dp
private val IconSize = 20.dp

/**
 * Round 44dp icon button on the surface colour (back, close, more). [elevated] adds card depth;
 * leave it off when the button sits on a photo.
 */
@Composable
fun TwoverseCircleIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
) {
    val shape = TwoverseTheme.shapes.circle
    Box(
        modifier = modifier.minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ButtonSize)
                .then(if (elevated) Modifier.twoverseCardDepth(shape) else Modifier)
                .clip(shape)
                .background(TwoverseTheme.colors.surface)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = TwoverseTheme.colors.onSurface,
                modifier = Modifier.size(IconSize),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TwoverseCircleIconButtonPreview() {
    TwoversePreviewBackground {
        Row(horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm)) {
            TwoverseCircleIconButton(icon = R.drawable.ic_close, contentDescription = "Close", onClick = {})
            TwoverseCircleIconButton(
                icon = R.drawable.ic_more,
                contentDescription = "More options",
                onClick = {},
                elevated = false,
            )
        }
    }
}
