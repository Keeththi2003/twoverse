package app.twoverse.feature.orbit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MeetupEditorRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeetupEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnDone()
    }
    MeetupEditorScreen(
        uiState = uiState,
        actions = MeetupEditorActions(
            onBack = onDone,
            onOpenPicker = viewModel::onOpenPicker,
            onDismissPicker = viewModel::onDismissPicker,
            onStartDateSelected = viewModel::onStartDateSelected,
            onEndDateSelected = viewModel::onEndDateSelected,
            onSeveralDaysChange = viewModel::onSeveralDaysChange,
            onPlaceChange = viewModel::onPlaceChange,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::onSave,
        ),
        modifier = modifier,
    )
}
