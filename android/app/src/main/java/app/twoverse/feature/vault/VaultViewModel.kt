package app.twoverse.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.expiryBadge
import app.twoverse.core.common.isExpired
import app.twoverse.core.common.ticks
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.local.OursLock
import app.twoverse.core.model.isNew
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    memoryRepository: MemoryRepository,
    settingsRepository: SettingsRepository,
    private val oursLock: OursLock,
    clock: Clock,
) : ViewModel() {

    private val filter = MutableStateFlow(VaultFilter.All)

    val uiState: StateFlow<VaultUiState> = combine(
        memoryRepository.memories,
        filter,
        clock.ticks(BadgeRefreshMillis),
        oursLocked(settingsRepository, oursLock),
    ) { memories, filter, now, locked ->
        if (locked) return@combine VaultUiState.Locked
        val accessible = memories
            .filterNot { isExpired(it.expiresAt, now) }
            .sortedByDescending { it.createdAt }
        VaultUiState.Success(
            filter = filter,
            totalCount = accessible.size,
            tiles = accessible.filteredBy(filter).map { memory ->
                VaultTile(
                    id = memory.id,
                    sender = memory.sender,
                    imageUrl = memory.imageUrl,
                    isNew = memory.isNew,
                    expiryBadge = expiryBadge(memory.expiresAt, now),
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = VaultUiState.Loading,
    )

    fun onFilterSelected(selected: VaultFilter) {
        filter.value = selected
    }

    /** The user confirmed it's them with biometrics or the device PIN (FR-VLT-5). */
    fun onUnlocked() {
        oursLock.unlock()
    }

    private companion object {
        const val BadgeRefreshMillis = 60_000L
        const val StopTimeoutMillis = 5_000L
    }
}
