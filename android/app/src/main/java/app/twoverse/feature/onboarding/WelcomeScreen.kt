package app.twoverse.feature.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.OrbitGraphic
import app.twoverse.core.designsystem.component.OrbitPosition
import app.twoverse.core.designsystem.component.OrbitRing
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.component.twoverseCardDepth
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val OrbitSize = 260.dp
private val HerPlanetSize = 44.dp
private val YouPlanetSize = 30.dp
private val StarSize = 22.dp
private val BadgeSize = 40.dp
private val BadgeIconSize = 18.dp

/** Feature badges around the orbit, positioned from its centre as in Welcome.dc.html. */
private enum class FeatureBadge(@param:DrawableRes val icon: Int, val x: Dp, val y: Dp) {
    Vault(R.drawable.ic_lock, x = (-122).dp, y = 25.dp),
    Compass(R.drawable.ic_compass, x = 122.dp, y = 25.dp),
    Countdown(R.drawable.ic_calendar, x = (-23).dp, y = 128.dp),
}

/** Welcome (FR-ONB-2). */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onHaveCoupleCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(
                start = spacing.screenHorizontalWide,
                end = spacing.screenHorizontalWide,
                top = spacing.xs,
                bottom = spacing.md,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.xxl),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            WelcomeOrbit()
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            TwoversePrimaryButton(
                text = stringResource(R.string.welcome_get_started),
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth(),
            )
            TwoverseTextButton(
                text = stringResource(R.string.welcome_have_code),
                onClick = onHaveCoupleCode,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WelcomeOrbit(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(OrbitSize), contentAlignment = Alignment.Center) {
        OrbitGraphic(
            size = OrbitSize,
            her = OrbitPosition(angleDegrees = 30f),
            you = OrbitPosition(angleDegrees = 240f, ring = OrbitRing.Inner),
            herSize = HerPlanetSize,
            youSize = YouPlanetSize,
            starSize = StarSize,
        )
        FeatureBadge.entries.forEach { badge ->
            FeatureBadgeIcon(
                icon = badge.icon,
                modifier = Modifier.offset(x = badge.x, y = badge.y),
            )
        }
    }
}

@Composable
private fun FeatureBadgeIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier) {
    val shape = TwoverseTheme.shapes.circle
    Box(
        modifier = modifier
            .size(BadgeSize)
            .twoverseCardDepth(shape)
            .clip(shape)
            .background(TwoverseTheme.colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = TwoverseTheme.colors.primary,
            modifier = Modifier.size(BadgeIconSize),
        )
    }
}

@PreviewLightDark
@Composable
private fun WelcomeScreenPreview() {
    TwoverseTheme {
        WelcomeScreen(onGetStarted = {}, onHaveCoupleCode = {})
    }
}
