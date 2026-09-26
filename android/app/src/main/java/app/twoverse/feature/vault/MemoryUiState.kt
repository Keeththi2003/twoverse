package app.twoverse.feature.vault

import app.twoverse.core.common.ExpiryBadge
import app.twoverse.core.model.MemorySender
import java.time.LocalDateTime

data class MemoryUiState(
    val content: MemoryContent = MemoryContent.Loading,
    val isDeleteDialogOpen: Boolean = false,
    /** Set once the memory was deleted, so the screen can return to the vault. */
    val isDeleted: Boolean = false,
)

sealed interface MemoryContent {
    data object Loading : MemoryContent

    /** The memory expired or no longer exists (BR-6, SRS §7 "This memory has expired."). */
    data object Expired : MemoryContent

    data class Viewing(
        val sender: MemorySender,
        val imageUrl: String?,
        val caption: String?,
        /** When it was sent, in the user's local time zone. */
        val sentAt: LocalDateTime,
        val expiryBadge: ExpiryBadge?,
        /** The sender allowed keeping this temporary memory (FR-MEM-5, FR-DEL-2). */
        val canKeepForever: Boolean,
    ) : MemoryContent
}
