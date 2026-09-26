package app.twoverse.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.DefaultStars
import app.twoverse.core.designsystem.component.LoadingDots
import app.twoverse.core.designsystem.component.OrbitGraphic
import app.twoverse.core.designsystem.component.StarField
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val OrbitSize = 232.dp
private val OrbitToWordmarkGap = 40.dp

/** Splash (FR-ONB-1): orbiting planets, twinkling stars and a loading indicator. */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        StarField(
            stars = if (colors.isDark) SplashDarkStars else DefaultStars,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(bottom = spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                OrbitGraphic(size = OrbitSize, animated = true)
                Spacer(modifier = Modifier.height(OrbitToWordmarkGap))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                Text(
                    text = stringResource(R.string.splash_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            LoadingDots(
                color = if (colors.isDark) colors.gold else colors.accent,
                contentDescription = stringResource(R.string.splash_loading),
            )
            Spacer(modifier = Modifier.height(spacing.smd))
            Text(
                text = stringResource(R.string.splash_connecting),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SplashScreenPreview() {
    TwoverseTheme {
        SplashScreen()
    }
}
