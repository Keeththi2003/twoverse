package app.twoverse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val ButtonSize = 44.dp
private val IconSize = 20.dp

/** Round 44dp back button on the surface colour with card depth (Pair, Countdown, Memory). */
@Composable
fun TwoverseBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.common_back),
) {
    val shape = TwoverseTheme.shapes.circle
    Box(
        modifier = modifier.minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ButtonSize)
                .twoverseCardDepth(shape)
                .clip(shape)
                .background(TwoverseTheme.colors.surface)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = TwoverseTheme.colors.onSurface,
                modifier = Modifier.size(IconSize),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TwoverseBackButtonPreview() {
    TwoversePreviewBackground {
        TwoverseBackButton(onClick = {})
    }
}
