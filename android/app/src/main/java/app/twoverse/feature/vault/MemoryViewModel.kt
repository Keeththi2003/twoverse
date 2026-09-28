package app.twoverse.feature.vault

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.expiryBadge
import app.twoverse.core.common.isExpired
import app.twoverse.core.common.ticks
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.local.OursLock
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import app.twoverse.core.model.canKeepForever
import app.twoverse.core.model.isNew
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Key of the memory id in the type-safe `MemoryRoute(memoryId)` destination; navigation stores
 * route properties in the SavedStateHandle under their names.
 */
internal const val MemoryIdKey = "memoryId"

@HiltViewModel
class MemoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoryRepository: MemoryRepository,
    settingsRepository: SettingsRepository,
    private val oursLock: OursLock,
    private val clock: Clock,
) : ViewModel() {

    private val memoryId: String = checkNotNull(savedStateHandle[MemoryIdKey])
    private val interaction = MutableStateFlow(MemoryUiState())
    private val memory = memoryRepository.memory(memoryId)

    val uiState: StateFlow<MemoryUiState> = combine(
        interaction,
        memory,
        clock.ticks(ExpiryRefreshMillis),
        oursLocked(settingsRepository, oursLock),
    ) { state, memory, now, locked ->
        state.copy(
            content = if (state.isRemoved || locked) MemoryContent.Loading else contentFor(memory, now),
            isLocked = locked,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = MemoryUiState(),
    )

    init {
        viewModelScope.launch {
            val opened = memory.filter { it != null }.first()
            if (opened?.isNew == true) memoryRepository.markViewed(memoryId)
        }
    }

    fun onUnlocked() {
        oursLock.unlock()
    }

    fun onKeepForever() {
        interaction.update { it.copy(error = null) }
        viewModelScope.launch { showFailure(memoryRepository.keepForever(memoryId)) }
    }

    fun onRemoveRequested() {
        interaction.update { it.copy(isRemoveDialogOpen = true, error = null) }
    }

    fun onRemoveDismissed() {
        interaction.update { it.copy(isRemoveDialogOpen = false) }
    }

    /** The sender deletes for both; the recipient only hides it (SRS 12). */
    fun onRemoveConfirmed() {
        val removal = (uiState.value.content as? MemoryContent.Viewing)?.removal ?: return
        interaction.update { it.copy(isRemoveDialogOpen = false) }
        viewModelScope.launch {
            val result = when (removal) {
                MemoryRemoval.Delete -> memoryRepository.delete(memoryId)
                MemoryRemoval.Hide -> memoryRepository.hide(memoryId)
            }
            when (result) {
                is DataResult.Success -> interaction.update { it.copy(isRemoved = true) }
                is DataResult.Failure -> interaction.update { it.copy(error = result.error) }
            }
        }
    }

    private fun showFailure(result: DataResult<Unit>) {
        interaction.update { it.copy(error = (result as? DataResult.Failure)?.error) }
    }

    private fun contentFor(memory: Memory?, now: Instant): MemoryContent {
        if (memory == null || isExpired(memory.expiresAt, now)) return MemoryContent.Expired
        return MemoryContent.Viewing(
            sender = memory.sender,
            imageUrl = memory.imageUrl,
            caption = memory.caption,
            sentAt = memory.createdAt.atZone(clock.zone).toLocalDateTime(),
            expiryBadge = expiryBadge(memory.expiresAt, now),
            canKeepForever = memory.canKeepForever,
            removal = if (memory.sender == MemorySender.Me) MemoryRemoval.Delete else MemoryRemoval.Hide,
        )
    }

    private companion object {
        const val ExpiryRefreshMillis = 60_000L
        const val StopTimeoutMillis = 5_000L
    }
}
