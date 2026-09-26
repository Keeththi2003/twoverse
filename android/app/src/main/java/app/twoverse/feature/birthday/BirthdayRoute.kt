package app.twoverse.feature.birthday

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BirthdayRoute(
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BirthdayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnEnter by rememberUpdatedState(onEnter)
    val state = uiState

    LaunchedEffect(state) {
        val done = state is BirthdayUiState.None || (state is BirthdayUiState.Welcome && state.isEntered)
        if (done) currentOnEnter()
    }

    BirthdayScreen(uiState = uiState, onEnter = viewModel::onEnter, modifier = modifier)
}
