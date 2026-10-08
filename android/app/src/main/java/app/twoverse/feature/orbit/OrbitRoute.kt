package app.twoverse.feature.orbit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OrbitRoute(
    onSetTogetherSince: () -> Unit,
    onAddMeetup: () -> Unit,
    onEditMeetup: (meetupId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrbitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        uiState = uiState,
        actions = OrbitActions(
            onSetTogetherSince = onSetTogetherSince,
            onAddMeetup = onAddMeetup,
            onEditMeetup = onEditMeetup,
            onDelete = viewModel::onDelete,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
            onDeleteDismissed = viewModel::onDeleteDismissed,
        ),
        modifier = modifier,
    )
}
