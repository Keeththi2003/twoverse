package app.twoverse.feature.countdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditReunionRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditReunionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) currentOnDone()
    }

    EditReunionScreen(
        uiState = uiState,
        actions = EditReunionActions(
            onBack = onDone,
            onOpenPicker = viewModel::onOpenPicker,
            onDismissPicker = viewModel::onDismissPicker,
            onDateSelected = viewModel::onDateSelected,
            onHasTimeChange = viewModel::onHasTimeChange,
            onTimeSelected = viewModel::onTimeSelected,
            onPlaceChange = viewModel::onPlaceChange,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::onSave,
            onClearRequested = viewModel::onClearRequested,
            onClearDismissed = viewModel::onClearDismissed,
            onClearConfirmed = viewModel::onClearConfirmed,
        ),
        modifier = modifier,
    )
}
