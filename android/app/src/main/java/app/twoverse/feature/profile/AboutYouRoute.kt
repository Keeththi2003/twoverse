package app.twoverse.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Required (FR-PRO-2): Back does nothing until the step is done; [onDone] continues into the app. */
@Composable
fun AboutYouRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutYouViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    BackHandler {}
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) currentOnDone()
    }
    AboutYouScreen(
        uiState = uiState,
        onShortNameChange = viewModel::onShortNameChange,
        onPronounsSelected = viewModel::onPronounsSelected,
        onContinue = viewModel::onContinue,
        modifier = modifier,
    )
}
