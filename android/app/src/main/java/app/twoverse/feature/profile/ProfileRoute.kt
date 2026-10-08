package app.twoverse.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        actions = ProfileActions(
            onBack = onBack,
            onFullNameChange = viewModel::onFullNameChange,
            onShortNameChange = viewModel::onShortNameChange,
            onPronounsSelected = viewModel::onPronounsSelected,
            onPhoneChange = viewModel::onPhoneChange,
            onShareEmailChange = viewModel::onShareEmailChange,
            onSharePhoneChange = viewModel::onSharePhoneChange,
            onSave = viewModel::onSave,
            onChangeEmail = viewModel::onChangeEmail,
            onNewEmailChange = viewModel::onNewEmailChange,
            onConfirmEmail = viewModel::onConfirmEmail,
            onDismissEmailDialog = viewModel::onDismissEmailDialog,
        ),
        modifier = modifier,
    )
}
