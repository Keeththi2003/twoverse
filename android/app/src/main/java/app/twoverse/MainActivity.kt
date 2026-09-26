package app.twoverse

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.AppearanceMode
import app.twoverse.navigation.TwoverseNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val openHome = intent.getBooleanExtra(EXTRA_OPEN_HOME, false)
        setContent {
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
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
                TwoverseNavHost(openHome = openHome, isOffline = isOffline)
            }
        }
    }

    companion object {
        /** Set by the home-screen widget so a tap opens Our Universe directly (FR-WGT-4). */
        const val EXTRA_OPEN_HOME = "app.twoverse.extra.OPEN_HOME"
    }
}
