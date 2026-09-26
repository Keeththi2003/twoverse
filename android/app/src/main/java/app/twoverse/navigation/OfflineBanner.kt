package app.twoverse.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

/** App-wide banner while offline, so cached data is never mistaken for live (NFR-REL-1, SRS §7). */
@Composable
internal fun OfflineBanner(modifier: Modifier = Modifier) {
    val spacing = TwoverseTheme.spacing
    Text(
        text = stringResource(R.string.offline_banner),
        style = MaterialTheme.typography.labelMedium,
        color = TwoverseTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(TwoverseTheme.colors.chip)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = spacing.md, vertical = spacing.xs)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@PreviewLightDark
@Composable
private fun OfflineBannerPreview() {
    TwoverseTheme {
        OfflineBanner()
    }
}
