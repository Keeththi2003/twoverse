package app.twoverse.feature.orbit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** [onDone] after saving, skipping, or going back. */
@Composable
fun TogetherSinceRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TogetherSinceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) currentOnDone()
    }
    TogetherSinceScreen(
        uiState = uiState,
        onBack = onDone,
        onOpenPicker = viewModel::onOpenPicker,
        onDismissPicker = viewModel::onDismissPicker,
        onDateSelected = viewModel::onDateSelected,
        onSave = viewModel::onSave,
        onSkip = viewModel::onSkip,
        modifier = modifier,
    )
}
