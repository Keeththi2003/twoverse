package app.twoverse.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R

/** Round back button (Pair, Countdown, Memory). */
@Composable
fun TwoverseBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
) {
    TwoverseCircleIconButton(
        icon = R.drawable.ic_chevron_left,
        contentDescription = stringResource(R.string.common_back),
        onClick = onClick,
        modifier = modifier,
        elevated = elevated,
    )
}

@PreviewLightDark
@Composable
private fun TwoverseBackButtonPreview() {
    TwoversePreviewBackground {
        TwoverseBackButton(onClick = {})
    }
}
