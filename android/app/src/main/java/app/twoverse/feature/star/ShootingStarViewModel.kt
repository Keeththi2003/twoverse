package app.twoverse.feature.star

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.ShootingStarRepository
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.waitingToBeShown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Key of the star id in the type-safe `ShootingStarRoute(starId)` and `StarComposerRoute(starId)`
 * destinations; navigation stores route properties in the SavedStateHandle under their names.
 */
internal const val StarIdKey = "starId"

/**
 * Shows the waiting Shooting Stars one after another, oldest first, marking each seen as the
 * recipient moves on (FR-STAR-12). With a star id it shows just that star again (FR-STAR-13).
 */
@HiltViewModel
class ShootingStarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShootingStarRepository,
    private val clock: Clock,
) : ViewModel() {

    private val replayId: String? = savedStateHandle[StarIdKey]
    private val state = MutableStateFlow<ShootingStarUiState>(ShootingStarUiState.Loading)
    val uiState: StateFlow<ShootingStarUiState> = state.asStateFlow()

    private var queue: List<String> = emptyList()
    private var currentUnseen = false
    private var isAdvancing = false

    init {
        viewModelScope.launch {
            queue = replayId?.let(::listOf)
                ?: repository.received.first().waitingToBeShown(clock.instant()).map { it.id }
            showNext()
        }
    }

    /**
     * Marks the star seen so it only shows once, then moves on. Continues even if that fails
     * offline; the star is then shown once more next time.
     */
    fun onEnter() {
        val current = state.value as? ShootingStarUiState.Showing ?: return
        if (isAdvancing) return
        isAdvancing = true
        viewModelScope.launch {
            if (currentUnseen) repository.markSeen(current.starId)
            showNext()
            isAdvancing = false
        }
    }

    /** A star that can't be loaded is skipped; it stays unseen and shows again next time. */
    private suspend fun showNext() {
        while (queue.isNotEmpty()) {
            val id = queue.first()
            queue = queue.drop(1)
            val result = repository.star(id)
            if (result is DataResult.Success) {
                val star = result.value
                currentUnseen = star.seenAt == null
                state.value = ShootingStarUiState.Showing(
                    starId = star.id,
                    display = StarDisplay(content = star.content.normalized(), photo = star.photoUrl),
                    hasNext = queue.isNotEmpty(),
                )
                return
            }
        }
        state.value = ShootingStarUiState.Done
    }
}
