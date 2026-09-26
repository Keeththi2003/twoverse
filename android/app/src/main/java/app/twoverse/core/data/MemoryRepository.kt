package app.twoverse.core.data

import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemoryDraft
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    /** All accessible memories, newest first. */
    val memories: Flow<List<Memory>>

    fun memory(id: String): Flow<Memory?>

    suspend fun send(draft: MemoryDraft): Result<Memory>

    /** Removes the expiry of a memory the sender allowed to keep (FR-DEL-2). */
    suspend fun keepForever(id: String)

    suspend fun delete(id: String)

    suspend fun markViewed(id: String)
}
