package app.twoverse.feature.star

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.SegmentedOptions
import app.twoverse.core.designsystem.component.TwoverseCircleIconButton
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarLayout

private val ThemeSwitchWidth = 180.dp

/**
 * The star full screen, exactly as the partner will see it, in light or dark (FR-STAR-8). Drawn
 * in the composer's own window rather than a dialog, so FLAG_SECURE covers it too (FR-STAR-14).
 */
@Composable
internal fun StarPreview(
    display: StarDisplay,
    isDark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    TwoverseTheme(darkTheme = isDark) {
        Box(modifier = modifier.fillMaxSize()) {
            ShootingStarContent(
                display = display,
                enterLabel = stringResource(R.string.star_enter),
                onEnter = {},
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = TwoverseTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TwoverseCircleIconButton(
                    icon = R.drawable.ic_close,
                    contentDescription = stringResource(R.string.star_preview_close),
                    onClick = onClose,
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    SegmentedOptions(
                        options = listOf(
                            stringResource(R.string.star_preview_light),
                            stringResource(R.string.star_preview_dark),
                        ),
                        selectedIndex = if (isDark) 1 else 0,
                        onSelect = { onDarkChange(it == 1) },
                        modifier = Modifier.width(ThemeSwitchWidth),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StarPreviewPreview() {
    TwoverseTheme {
        StarPreview(
            display = StarDisplay(
                content = StarContent(layout = StarLayout.MessageOnly, title = "Good luck", message = "You've got this."),
                photo = null,
            ),
            isDark = TwoverseTheme.colors.isDark,
            onDarkChange = {},
            onClose = {},
        )
    }
}
