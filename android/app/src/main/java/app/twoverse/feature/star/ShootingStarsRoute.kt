package app.twoverse.feature.star

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ShootingStarsRoute(
    onBack: () -> Unit,
    onCompose: () -> Unit,
    onEdit: (starId: String) -> Unit,
    onOpenReceived: (starId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShootingStarsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Sent stars change in the composer, so they are reloaded whenever this screen returns.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    ShootingStarsScreen(
        uiState = uiState,
        actions = ShootingStarsActions(
            onBack = onBack,
            onCompose = onCompose,
            onEdit = onEdit,
            onDelete = viewModel::onDelete,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
            onDeleteDismissed = viewModel::onDeleteDismissed,
            onOpenReceived = onOpenReceived,
            onRetry = viewModel::refresh,
        ),
        modifier = modifier,
    )
}
