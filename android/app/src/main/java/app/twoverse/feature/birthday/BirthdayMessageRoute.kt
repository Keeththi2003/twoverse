package app.twoverse.feature.birthday

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BirthdayMessageRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BirthdayMessageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhotoPicked(it.toString()) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnDone()
    }

    BirthdayMessageScreen(
        uiState = uiState,
        actions = BirthdayMessageActions(
            onBack = onDone,
            onRetryLoad = viewModel::load,
            onMessageChange = viewModel::onMessageChange,
            onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            onRemovePhoto = viewModel::onRemovePhoto,
            onOpenDatePicker = viewModel::onOpenDatePicker,
            onDismissDatePicker = viewModel::onDismissDatePicker,
            onDateSelected = viewModel::onDateSelected,
            onClearDate = viewModel::onClearDate,
            onSave = viewModel::onSave,
        ),
        modifier = modifier,
    )
}
