package app.twoverse.feature.star

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.ShootingStarRepository
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.ShootingStar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** The sender's stars with their status, edit and delete while unseen (FR-STAR-9); received stars to view again (FR-STAR-13). */
@HiltViewModel
class ShootingStarsViewModel @Inject constructor(
    private val repository: ShootingStarRepository,
    private val clock: Clock,
) : ViewModel() {

    private val state = MutableStateFlow(ShootingStarsUiState())

    val uiState: StateFlow<ShootingStarsUiState> = combine(state, repository.received) { state, received ->
        state.copy(received = received.sortedByDescending { it.visibleFrom }.map { it.toItem(sentByMe = false) })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = ShootingStarsUiState(),
    )

    /** Loads the sent stars; called whenever the screen comes back, e.g. after editing. */
    fun refresh() {
        viewModelScope.launch {
            when (val result = repository.sent()) {
                is DataResult.Success -> state.update {
                    it.copy(isLoading = false, loadError = null, sent = result.value.map { star -> star.toItem(sentByMe = true) })
                }
                is DataResult.Failure -> state.update { it.copy(isLoading = false, loadError = result.error) }
            }
        }
    }

    fun onDelete(id: String) {
        state.update { it.copy(pendingDeleteId = id, error = null) }
    }

    fun onDeleteDismissed() {
        state.update { it.copy(pendingDeleteId = null) }
    }

    fun onDeleteConfirmed() {
        val id = state.value.pendingDeleteId ?: return
        state.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch {
            val result = repository.delete(id)
            state.update { it.copy(error = (result as? DataResult.Failure)?.error) }
            refresh()
        }
    }

    private fun ShootingStar.toItem(sentByMe: Boolean): StarListItem {
        val now = clock.instant()
        val status = status(now)
        return StarListItem(
            id = id,
            headline = content.title ?: content.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()?.take(HeadlineLength),
            layout = content.layout,
            status = status,
            time = visibleFrom.atZone(clock.zone).toLocalDateTime(),
            isEditable = sentByMe && isEditable,
        )
    }

    private companion object {
        const val HeadlineLength = 60
        const val StopTimeoutMillis = 5_000L
    }
}
