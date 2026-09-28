package app.twoverse.feature.vault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twoverse.core.designsystem.component.SecureWindowEffect

@Composable
fun VaultRoute(
    onOpenMemory: (String) -> Unit,
    onAddMemory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    SecureWindowEffect()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unlock = rememberOursUnlocker(onUnlocked = viewModel::onUnlocked)

    if (uiState is VaultUiState.Locked) {
        LaunchedEffect(Unit) { unlock() }
    }

    VaultScreen(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onOpenMemory = onOpenMemory,
        onAddMemory = onAddMemory,
        onUnlock = unlock,
        modifier = modifier,
    )
}
