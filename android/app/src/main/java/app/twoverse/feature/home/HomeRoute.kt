package app.twoverse.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    onOpenCompass: () -> Unit,
    onOpenCountdown: () -> Unit,
    onOpenVault: () -> Unit,
    onSendMemory: () -> Unit,
    onOpenLocationSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onOpenCompass = onOpenCompass,
        onOpenCountdown = onOpenCountdown,
        onOpenVault = onOpenVault,
        onSendMemory = onSendMemory,
        onOpenLocationSetup = onOpenLocationSetup,
        modifier = modifier,
    )
}
