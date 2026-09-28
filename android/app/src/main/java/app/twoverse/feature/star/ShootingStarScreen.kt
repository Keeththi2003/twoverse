package app.twoverse.feature.star

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.component.StarField
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarLayout

/** Shooting Star viewer (FR-STAR-10 to FR-STAR-12); a star field while loading. */
@Composable
fun ShootingStarScreen(uiState: ShootingStarUiState, onEnter: () -> Unit, modifier: Modifier = Modifier) {
    if (uiState is ShootingStarUiState.Showing) {
        ShootingStarContent(
            display = uiState.display,
            enterLabel = stringResource(if (uiState.hasNext) R.string.star_next else R.string.star_enter),
            onEnter = onEnter,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TwoverseTheme.colors.background),
        ) {
            StarField(stars = ShootingStarBackdrop, twinkle = false, modifier = Modifier.fillMaxSize())
        }
    }
}

@PreviewLightDark
@Composable
private fun ShootingStarScreenPreview() {
    TwoverseTheme {
        ShootingStarScreen(
            uiState = ShootingStarUiState.Showing(
                starId = "1",
                display = StarDisplay(
                    content = StarContent(
                        layout = StarLayout.PhotoMessage,
                        eyebrow = "For you",
                        title = "Happy Birthday",
                        message = "I built a little universe,\njust for the two of us.",
                        signature = "Keeththi",
                    ),
                    photo = null,
                ),
                hasNext = true,
            ),
            onEnter = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ShootingStarScreenLoadingPreview() {
    TwoverseTheme {
        ShootingStarScreen(uiState = ShootingStarUiState.Loading, onEnter = {})
    }
}
