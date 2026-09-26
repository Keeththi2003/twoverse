package app.twoverse.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Welcome has no state or logic, so it needs no ViewModel. */
@Composable
fun WelcomeRoute(
    onGetStarted: () -> Unit,
    onHaveCoupleCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WelcomeScreen(
        onGetStarted = onGetStarted,
        onHaveCoupleCode = onHaveCoupleCode,
        modifier = modifier,
    )
}
