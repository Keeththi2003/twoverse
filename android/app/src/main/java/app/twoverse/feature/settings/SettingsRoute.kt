package app.twoverse.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onSignedOut: () -> Unit,
    onDisconnected: () -> Unit,
    onSendShootingStar: () -> Unit,
    onOpenShootingStars: () -> Unit,
    onOpenLocationSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSignedOut by rememberUpdatedState(onSignedOut)
    val currentOnDisconnected by rememberUpdatedState(onDisconnected)

    LaunchedEffect(uiState.exit) {
        when (uiState.exit) {
            SettingsExit.SignedOut -> currentOnSignedOut()
            SettingsExit.Disconnected -> currentOnDisconnected()
            null -> Unit
        }
    }

    SettingsScreen(
        uiState = uiState,
        actions = SettingsActions(
            onShareLocationChange = viewModel::onShareLocationChange,
            onLockOursChange = viewModel::onLockOursChange,
            onOpenDialog = viewModel::onOpenDialog,
            onDismissDialog = viewModel::onDismissDialog,
            onLocationPrecisionSelected = viewModel::onLocationPrecisionSelected,
            onDistanceUnitSelected = viewModel::onDistanceUnitSelected,
            onAppearanceSelected = viewModel::onAppearanceSelected,
            onLogOutConfirmed = viewModel::onLogOutConfirmed,
            onDisconnectConfirmed = viewModel::onDisconnectConfirmed,
            onDeleteAccountConfirmed = viewModel::onDeleteAccountConfirmed,
            onSendShootingStar = onSendShootingStar,
            onOpenShootingStars = onOpenShootingStars,
            onOpenLocationSetup = onOpenLocationSetup,
        ),
        modifier = modifier,
    )
}
