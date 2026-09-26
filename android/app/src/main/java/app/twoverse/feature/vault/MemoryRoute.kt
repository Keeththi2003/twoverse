package app.twoverse.feature.vault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twoverse.core.designsystem.component.SecureWindowEffect

@Composable
fun MemoryRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoryViewModel = hiltViewModel(),
) {
    SecureWindowEffect()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) currentOnBack()
    }

    MemoryScreen(
        uiState = uiState,
        onBack = onBack,
        onKeepForever = viewModel::onKeepForever,
        onDelete = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        modifier = modifier,
    )
}
