package app.twoverse.feature.countdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CountdownRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CountdownViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CountdownScreen(
        uiState = uiState,
        onBack = onBack,
        onChangeDate = viewModel::onChangeDate,
        onDismissDatePicker = viewModel::onDismissDatePicker,
        onDateSelected = viewModel::onDateSelected,
        modifier = modifier,
    )
}
