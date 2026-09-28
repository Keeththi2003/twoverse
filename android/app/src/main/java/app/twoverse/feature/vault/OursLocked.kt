package app.twoverse.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val LockCircleSize = 72.dp
private val LockIconSize = 32.dp

/** Shown instead of Ours until the user confirms it's them (FR-VLT-5). */
@Composable
internal fun OursLocked(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(LockCircleSize)
                .background(colors.chip, TwoverseTheme.shapes.circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(LockIconSize),
            )
        }
        Text(
            text = stringResource(R.string.vault_locked_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = spacing.lg)
                .semantics { heading() },
        )
        Text(
            text = stringResource(R.string.vault_locked_message),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs),
        )
        TwoversePrimaryButton(
            text = stringResource(R.string.vault_unlock),
            onClick = onUnlock,
            leadingIcon = R.drawable.ic_lock,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.xl),
        )
    }
}

@PreviewLightDark
@Composable
private fun OursLockedPreview() {
    TwoverseTheme {
        Box(modifier = Modifier.background(TwoverseTheme.colors.background)) {
            OursLocked(onUnlock = {})
        }
    }
}
