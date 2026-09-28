package app.twoverse.core.data.fake

import app.twoverse.core.common.expiresAt
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemoryDraft
import app.twoverse.core.model.MemorySender
import app.twoverse.core.model.MemoryUpload
import app.twoverse.core.model.canKeepForever
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/** In-memory memories for previews and tests, following the same sender / recipient rules as the backend. */
class FakeMemoryRepository @Inject constructor(
    private val clock: Clock,
) : MemoryRepository {
    private val all = MutableStateFlow(SampleData.memories)
    private var nextFailure: DataError? = null

    override val memories: StateFlow<List<Memory>> = all

    /** Every draft sent, for tests. */
    val sentDrafts = mutableListOf<MemoryDraft>()

    /** Makes the next call fail with [error], to test error handling. */
    fun failNextWith(error: DataError) {
        nextFailure = error
    }

    fun setMemories(memories: List<Memory>) {
        all.value = memories
    }

    override fun memory(id: String): Flow<Memory?> = all.map { list -> list.find { it.id == id } }

    override fun send(draft: MemoryDraft): Flow<MemoryUpload> = flow {
        emit(MemoryUpload.Uploading(0f))
        takeFailure()?.let {
            emit(MemoryUpload.Failed(it))
            return@flow
        }
        emit(MemoryUpload.Uploading(1f))
        sentDrafts += draft
        val now = clock.instant()
        val memory = Memory(
            id = UUID.randomUUID().toString(),
            sender = MemorySender.Me,
            imageUrl = draft.photoUri,
            caption = draft.caption,
            createdAt = now,
            expiresAt = draft.expiry.expiresAt(now),
            allowKeep = draft.allowKeep,
            viewedAt = null,
        )
        all.update { listOf(memory) + it }
        emit(MemoryUpload.Sent(memory.id))
    }

    override suspend fun keepForever(id: String): DataResult<Unit> =
        change(id, allowed = { it.canKeepForever }) { it.copy(expiresAt = null) }

    override suspend fun delete(id: String): DataResult<Unit> =
        remove(id, allowed = { it.sender == MemorySender.Me })

    override suspend fun hide(id: String): DataResult<Unit> =
        remove(id, allowed = { it.sender == MemorySender.Partner })

    override suspend fun markViewed(id: String): DataResult<Unit> =
        change(id, allowed = { true }) { memory ->
            if (memory.sender == MemorySender.Partner && memory.viewedAt == null) memory.copy(viewedAt = clock.instant()) else memory
        }

    private fun change(id: String, allowed: (Memory) -> Boolean, transform: (Memory) -> Memory): DataResult<Unit> =
        check(id, allowed) { all.update { list -> list.map { if (it.id == id) transform(it) else it } } }

    private fun remove(id: String, allowed: (Memory) -> Boolean): DataResult<Unit> =
        check(id, allowed) { all.update { list -> list.filterNot { it.id == id } } }

    private fun check(id: String, allowed: (Memory) -> Boolean, apply: () -> Unit): DataResult<Unit> {
        takeFailure()?.let { return DataResult.Failure(it) }
        val memory = all.value.find { it.id == id } ?: return DataResult.Failure(DataError.MemoryUnavailable)
        if (!allowed(memory)) return DataResult.Failure(DataError.Unknown)
        apply()
        return DataResult.Success(Unit)
    }

    private fun takeFailure(): DataError? = nextFailure.also { nextFailure = null }
}
