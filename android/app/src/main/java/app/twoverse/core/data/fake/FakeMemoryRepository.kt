package app.twoverse.core.data.fake

import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemoryDraft
import app.twoverse.core.model.MemorySender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeMemoryRepository @Inject constructor() : MemoryRepository {
    private val all = MutableStateFlow(SampleData.memories)

    override val memories: StateFlow<List<Memory>> = all

    override fun memory(id: String): Flow<Memory?> = all.map { list -> list.find { it.id == id } }

    override suspend fun send(draft: MemoryDraft): Result<Memory> {
        val now = Instant.now()
        val memory = Memory(
            id = UUID.randomUUID().toString(),
            sender = MemorySender.Me,
            imageUrl = draft.photoUri,
            caption = draft.caption,
            createdAt = now,
            expiresAt = draft.expiry.duration?.let { now + it },
            allowKeep = draft.allowKeep,
            viewedAt = now,
        )
        all.update { listOf(memory) + it }
        return Result.success(memory)
    }

    override suspend fun keepForever(id: String) {
        updateMemory(id) { if (it.allowKeep) it.copy(expiresAt = null) else it }
    }

    override suspend fun delete(id: String) {
        all.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun markViewed(id: String) {
        updateMemory(id) { if (it.viewedAt == null) it.copy(viewedAt = Instant.now()) else it }
    }

    private fun updateMemory(id: String, transform: (Memory) -> Memory) {
        all.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }
}
