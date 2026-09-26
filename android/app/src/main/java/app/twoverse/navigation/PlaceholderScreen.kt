package app.twoverse.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.theme.TwoverseTheme

data class PlaceholderAction(
    @param:StringRes val labelRes: Int,
    val onClick: () -> Unit,
)

/** Stand-in for a feature screen until its real `XRoute` exists. */
@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    actions: List<PlaceholderAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = TwoverseTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TwoverseTheme.colors.background)
            .safeDrawingPadding()
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = TwoverseTheme.colors.onSurface,
        )
        Text(
            text = stringResource(R.string.placeholder_note),
            style = MaterialTheme.typography.bodyMedium,
            color = TwoverseTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = spacing.md),
        )
        actions.forEach { action ->
            TwoversePrimaryButton(
                text = stringResource(action.labelRes),
                onClick = action.onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PlaceholderScreenPreview() {
    TwoverseTheme {
        PlaceholderScreen(
            titleRes = R.string.welcome_title,
            actions = listOf(
                PlaceholderAction(R.string.welcome_get_started) {},
                PlaceholderAction(R.string.welcome_have_code) {},
            ),
        )
    }
}
