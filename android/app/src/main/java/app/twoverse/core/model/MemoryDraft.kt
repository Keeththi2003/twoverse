package app.twoverse.core.model

import java.time.Duration

/** A memory being created, before it is sent (FR-MEM). */
data class MemoryDraft(
    val photoUri: String,
    val caption: String?,
    val expiry: MemoryExpiry,
    val allowKeep: Boolean,
)

enum class MemoryExpiry(val duration: Duration?) {
    Never(null),
    Hours24(Duration.ofHours(24)),
    Days7(Duration.ofDays(7)),
    Days30(Duration.ofDays(30)),
}

/** Progress of sending a memory (FR-MEM-6). */
sealed interface MemoryUpload {
    /** [fraction] of the photo uploaded, from 0 to 1. */
    data class Uploading(val fraction: Float) : MemoryUpload

    data class Sent(val memoryId: String) : MemoryUpload

    data class Failed(val error: DataError) : MemoryUpload
}
