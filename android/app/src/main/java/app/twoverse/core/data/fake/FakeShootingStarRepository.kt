package app.twoverse.core.data.fake

import app.twoverse.core.data.ShootingStarRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.ShootingStar
import app.twoverse.core.model.ShootingStarDraft
import app.twoverse.core.model.StarField
import app.twoverse.core.model.StarPhotoChange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import javax.inject.Inject

/** In-memory Shooting Stars for previews and tests; starts with none. */
class FakeShootingStarRepository @Inject constructor() : ShootingStarRepository {
    private val receivedStars = MutableStateFlow<List<ShootingStar>>(emptyList())
    private val sentStars = mutableListOf<ShootingStar>()
    private var nextFailure: DataError? = null
    private var nextId = 1

    override val received: StateFlow<List<ShootingStar>> = receivedStars

    /** Every draft saved, with the id it edited (null for new stars), for tests. */
    val savedDrafts = mutableListOf<Pair<String?, ShootingStarDraft>>()

    /** Ids of every star marked seen, for tests. */
    val markedSeen = mutableListOf<String>()

    /** Photo URLs [star] returns, by star id. */
    val photoUrls = mutableMapOf<String, String>()

    /** Time given to stars created by [save]. */
    var now: Instant = Instant.EPOCH

    fun setReceived(stars: List<ShootingStar>) {
        receivedStars.value = stars
    }

    fun setSent(stars: List<ShootingStar>) {
        sentStars.clear()
        sentStars += stars
    }

    /** Makes the next call fail with [error], to test error handling. */
    fun failNextWith(error: DataError) {
        nextFailure = error
    }

    override suspend fun sent(): DataResult<List<ShootingStar>> = respond { sentStars.sortedByDescending { it.createdAt } }

    override suspend fun star(id: String): DataResult<ShootingStar> {
        failure()?.let { return DataResult.Failure(it) }
        val star = (receivedStars.value + sentStars).firstOrNull { it.id == id }
            ?: return DataResult.Failure(DataError.StarUnavailable)
        return DataResult.Success(star.copy(photoUrl = photoUrls[id]))
    }

    override suspend fun markSeen(id: String): DataResult<Unit> = respond {
        markedSeen += id
        receivedStars.value = receivedStars.value.map { if (it.id == id && it.seenAt == null) it.copy(seenAt = now) else it }
    }

    override suspend fun save(id: String?, draft: ShootingStarDraft): DataResult<Unit> {
        failure()?.let { return DataResult.Failure(it) }
        val existing = id?.let { sentId -> sentStars.firstOrNull { it.id == sentId } }
        if (id != null && existing?.isEditable != true) return DataResult.Failure(DataError.StarUnavailable)
        savedDrafts += id to draft
        val content = draft.content.normalized()
        val hasPhoto = content.layout.shows(StarField.Photo) && when (draft.photo) {
            StarPhotoChange.Keep -> existing?.hasPhoto == true
            StarPhotoChange.Remove -> false
            is StarPhotoChange.Replace -> true
        }
        val star = ShootingStar(
            id = id ?: "star-${nextId++}",
            content = content,
            hasPhoto = hasPhoto,
            showAt = draft.showAt,
            seenAt = null,
            createdAt = existing?.createdAt ?: now,
        )
        sentStars.removeAll { it.id == star.id }
        sentStars += star
        return DataResult.Success(Unit)
    }

    override suspend fun delete(id: String): DataResult<Unit> {
        failure()?.let { return DataResult.Failure(it) }
        val removed = sentStars.removeAll { it.id == id && it.isEditable }
        return if (removed) DataResult.Success(Unit) else DataResult.Failure(DataError.StarUnavailable)
    }

    private fun failure(): DataError? = nextFailure.also { nextFailure = null }

    private fun <T> respond(action: () -> T): DataResult<T> =
        failure()?.let { DataResult.Failure(it) } ?: DataResult.Success(action())
}
