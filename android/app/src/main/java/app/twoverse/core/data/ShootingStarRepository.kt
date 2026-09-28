package app.twoverse.core.data

import app.twoverse.core.model.DataResult
import app.twoverse.core.model.ShootingStar
import app.twoverse.core.model.ShootingStarDraft
import kotlinx.coroutines.flow.Flow

/** Shooting Stars, stored in the backend (FR-STAR-1). Lists carry no photo URLs; [star] loads one. */
interface ShootingStarRepository {
    /**
     * Stars the partner sent this user that are visible now, oldest first. Empty when not paired
     * or when they can't be loaded.
     */
    val received: Flow<List<ShootingStar>>

    /** Stars this user sent, newest first (FR-STAR-9). */
    suspend fun sent(): DataResult<List<ShootingStar>>

    /** One sent or received star, with a fresh signed photo URL. */
    suspend fun star(id: String): DataResult<ShootingStar>

    /** The recipient opened it, so it shows only once (FR-STAR-12). */
    suspend fun markSeen(id: String): DataResult<Unit>

    /** Sends a new star ([id] null) or edits an unseen one (FR-STAR-1, FR-STAR-9). */
    suspend fun save(id: String?, draft: ShootingStarDraft): DataResult<Unit>

    /** Deletes an unseen star this user sent (FR-STAR-9). */
    suspend fun delete(id: String): DataResult<Unit>
}
