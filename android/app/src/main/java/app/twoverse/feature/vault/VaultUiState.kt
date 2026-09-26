package app.twoverse.feature.vault

import app.twoverse.core.common.ExpiryBadge
import app.twoverse.core.model.MemorySender

sealed interface VaultUiState {
    data object Loading : VaultUiState

    data class Success(
        val filter: VaultFilter,
        /** All accessible memories, used for the count (FR-VLT-1). */
        val totalCount: Int,
        val tiles: List<VaultTile>,
    ) : VaultUiState
}

data class VaultTile(
    val id: String,
    val sender: MemorySender,
    val imageUrl: String?,
    /** Received and not opened yet (FR-VLT-3). */
    val isNew: Boolean,
    /** Time left for temporary memories (FR-VLT-4). */
    val expiryBadge: ExpiryBadge?,
)
