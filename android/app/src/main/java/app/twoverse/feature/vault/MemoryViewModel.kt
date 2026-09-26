package app.twoverse.feature.vault

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.expiryBadge
import app.twoverse.core.common.isExpired
import app.twoverse.core.common.ticks
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val clock: Clock,
) : ViewModel() {

    private val memoryId: String = checkNotNull(savedStateHandle[MemoryIdKey])
    private val dialogState = MutableStateFlow(MemoryUiState())

    val uiState: StateFlow<MemoryUiState> = combine(
        dialogState,
        memoryRepository.memory(memoryId),
        clock.ticks(ExpiryRefreshMillis),
    ) { state, memory, now ->
        state.copy(content = if (state.isDeleted) MemoryContent.Loading else contentFor(memory, now))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = MemoryUiState(),
    )

    init {
        viewModelScope.launch { memoryRepository.markViewed(memoryId) }
    }

    fun onKeepForever() {
        viewModelScope.launch { memoryRepository.keepForever(memoryId) }
    }

    fun onDeleteRequested() {
        dialogState.update { it.copy(isDeleteDialogOpen = true) }
    }

    fun onDeleteDismissed() {
        dialogState.update { it.copy(isDeleteDialogOpen = false) }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            memoryRepository.delete(memoryId)
            dialogState.update { it.copy(isDeleteDialogOpen = false, isDeleted = true) }
        }
    }

    private fun contentFor(memory: Memory?, now: Instant): MemoryContent {
        if (memory == null || isExpired(memory.expiresAt, now)) return MemoryContent.Expired
        return MemoryContent.Viewing(
            sender = memory.sender,
            imageUrl = memory.imageUrl,
            caption = memory.caption,
            sentAt = memory.createdAt.atZone(clock.zone).toLocalDateTime(),
            expiryBadge = expiryBadge(memory.expiresAt, now),
            canKeepForever = memory.sender == MemorySender.Partner && memory.allowKeep && memory.expiresAt != null,
        )
    }

    private companion object {
        const val ExpiryRefreshMillis = 60_000L
        const val StopTimeoutMillis = 5_000L
    }
}
