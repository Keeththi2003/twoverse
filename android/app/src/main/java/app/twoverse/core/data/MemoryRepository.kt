package app.twoverse.core.data

import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemoryDraft
import app.twoverse.core.model.MemoryUpload
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    /** All accessible memories, newest first, updated live (FR-VLT-1). */
    val memories: Flow<List<Memory>>

    fun memory(id: String): Flow<Memory?>

    /**
     * Resizes, compresses and uploads the photo, then saves the memory (FR-MEM-2, FR-MEM-6).
     * Ends with [MemoryUpload.Sent] or [MemoryUpload.Failed].
     */
    fun send(draft: MemoryDraft): Flow<MemoryUpload>

    /** Removes the expiry of a memory the sender allowed to keep; recipient only (FR-DEL-2). */
    suspend fun keepForever(id: String): DataResult<Unit>

    /** Deletes a memory for both partners, including its photo; sender only (FR-DEL-3, SRS 12). */
    suspend fun delete(id: String): DataResult<Unit>

    /** Removes a received memory from the recipient's own vault; the sender still has it (SRS 12). */
    suspend fun hide(id: String): DataResult<Unit>

    /** Clears the "new" indicator of a received memory (FR-VLT-3). */
    suspend fun markViewed(id: String): DataResult<Unit>
}
