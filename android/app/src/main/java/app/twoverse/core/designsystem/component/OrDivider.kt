package app.twoverse.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

/** Horizontal rule with "or" in the middle (Sign in, Pair). */
@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outline)
        Text(
            text = stringResource(R.string.common_or),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outline)
    }
}

@PreviewLightDark
@Composable
private fun OrDividerPreview() {
    TwoversePreviewBackground {
        OrDivider()
    }
}
