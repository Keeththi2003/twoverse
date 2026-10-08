package app.twoverse

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twoverse.core.common.EXTRA_LAUNCH_SCREEN
import app.twoverse.core.data.AuthDeepLinkHandler
import app.twoverse.core.designsystem.text.LocalPartnerName
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.LaunchScreen
import app.twoverse.navigation.TwoverseNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var authDeepLinkHandler: AuthDeepLinkHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleAuthDeepLink(intent)
        val startsOnScreen = launchScreen(intent)
        startsOnScreen?.let(viewModel::onLaunchScreen)
        setContent {
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
            val isPasswordRecovery by viewModel.isPasswordRecovery.collectAsStateWithLifecycle()
            val requestedScreen by viewModel.requestedScreen.collectAsStateWithLifecycle()
            val partner by viewModel.partner.collectAsStateWithLifecycle()
            val needsAboutYou by viewModel.needsAboutYou.collectAsStateWithLifecycle()
            val darkTheme = when (appearance) {
                AppearanceMode.System -> isSystemInDarkTheme()
                AppearanceMode.Light -> false
                AppearanceMode.Dark -> true
            }
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            TwoverseTheme(darkTheme = darkTheme) {
                // Every screen names the partner the same way (FR-PRO-1).
                CompositionLocalProvider(LocalPartnerName provides partner) {
                    TwoverseNavHost(
                        startsSignedIn = startsOnScreen != null,
                        requestedScreen = requestedScreen,
                        onRequestedScreenShown = viewModel::onLaunchScreenShown,
                        isOffline = isOffline,
                        isPasswordRecovery = isPasswordRecovery,
                        onPasswordRecoveryShown = viewModel::onPasswordRecoveryShown,
                        needsAboutYou = needsAboutYou,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeepLink(intent)
        launchScreen(intent)?.let(viewModel::onLaunchScreen)
    }

    /** The screen a notification or the widget opened the app for (FR-NOT, FR-WGT-4). */
    private fun launchScreen(intent: Intent): LaunchScreen? =
        intent.getStringExtra(EXTRA_LAUNCH_SCREEN)?.let { name -> LaunchScreen.entries.firstOrNull { it.name == name } }

    /** Password-reset emails return through app.twoverse://auth-callback (FR-AUTH-3). */
    private fun handleAuthDeepLink(intent: Intent) {
        authDeepLinkHandler.handle(intent, onPasswordRecovery = viewModel::onPasswordRecovery)
    }
}
