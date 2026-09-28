package app.twoverse.feature.pairing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ReconnectRoute(
    onBack: () -> Unit,
    onReconnected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReconnectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnReconnected by rememberUpdatedState(onReconnected)

    LaunchedEffect(uiState) {
        if (uiState == ReconnectUiState.Reconnected) currentOnReconnected()
    }

    ReconnectScreen(
        uiState = uiState,
        onBack = onBack,
        onReconnect = viewModel::onReconnect,
        modifier = modifier,
    )
}
