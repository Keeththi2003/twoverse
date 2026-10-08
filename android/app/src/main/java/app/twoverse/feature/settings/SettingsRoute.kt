package app.twoverse.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onSignedOut: () -> Unit,
    onDisconnected: () -> Unit,
    onSendShootingStar: () -> Unit,
    onOpenShootingStars: () -> Unit,
    onOpenLocationSetup: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
            onOpenProfile = onOpenProfile,
            onNicknameChange = viewModel::onNicknameChange,
            onSaveNickname = viewModel::onSaveNickname,
            onClearNickname = viewModel::onClearNickname,
            onEmailPartner = { email -> context.openSafely(Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null))) },
            onCallPartner = { phone -> context.openSafely(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))) },
        ),
        modifier = modifier,
    )
}

/** Mail or the dialer may be missing (e.g. on a tablet); then nothing happens. */
private fun Context.openSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No app can handle it.
    }
}
