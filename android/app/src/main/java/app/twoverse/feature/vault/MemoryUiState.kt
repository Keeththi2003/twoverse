package app.twoverse.feature.vault

import app.twoverse.core.common.ExpiryBadge
import app.twoverse.core.model.DataError
import app.twoverse.core.model.MemorySender
import java.time.LocalDateTime

data class MemoryUiState(
    val content: MemoryContent = MemoryContent.Loading,
    /** Lock Ours is on and Ours locked again, e.g. after the app was in the background (FR-VLT-5). */
    val isLocked: Boolean = false,
    val isRemoveDialogOpen: Boolean = false,
    /** Set once the memory was deleted or hidden, so the screen can return to the vault. */
    val isRemoved: Boolean = false,
    /** The last action failed (SRS section 7). */
    val error: DataError? = null,
)

/** What the user may do to take a memory out of their vault (SRS 12). */
enum class MemoryRemoval {
    /** The sender deletes it for both, including the photo (FR-DEL-3). */
    Delete,

    /** The recipient hides it from their own vault; the sender keeps it. */
    Hide,
}

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
        val removal: MemoryRemoval,
    ) : MemoryContent
}
