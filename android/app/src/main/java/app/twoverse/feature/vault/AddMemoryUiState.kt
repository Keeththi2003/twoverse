package app.twoverse.feature.vault

import app.twoverse.core.model.MemoryExpiry

data class AddMemoryUiState(
    val photoUri: String? = null,
    val caption: String = "",
    val expiry: MemoryExpiry = MemoryExpiry.Never,
    val allowKeep: Boolean = false,
    val isSending: Boolean = false,
    /** Share of the photo uploaded, from 0 to 1, while sending (FR-MEM-6). */
    val uploadProgress: Float = 0f,
    val sendFailed: Boolean = false,
    val isSent: Boolean = false,
) {
    val canSend: Boolean get() = photoUri != null && !isSending

    /** Keeping forever only matters for temporary memories (FR-MEM-5). */
    val showKeepOption: Boolean get() = expiry != MemoryExpiry.Never

    companion object {
        /** FR-MEM-3. */
        const val CaptionMaxLength = 500
    }
}
