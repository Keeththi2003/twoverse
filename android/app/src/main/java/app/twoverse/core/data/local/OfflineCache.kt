package app.twoverse.core.data.local

import app.twoverse.core.model.Couple
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.Memory
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * The latest data received from the server, kept on this device so Home, Countdown, Ours and
 * the widget can show it offline (NFR-REL-1, FR-WGT-1). It belongs to one signed-in user.
 * Timestamps are kept, so freshness is always worked out from when the data was really
 * recorded and saved data is never shown as live (BR-8). Null fields are not known yet.
 */
data class OfflineSnapshot(
    val ownerId: String,
    val couple: Couple? = null,
    val sharing: LocationSharing? = null,
    val myLocation: UserLocation? = null,
    val partnerLocation: UserLocation? = null,
    val reunion: Reunion? = null,
    /** Memory details without photos; photos live in the memory photo cache. */
    val memories: List<Memory>? = null,
    /** A Shooting Star from the partner is visible and not opened yet (only the fact, never its content). */
    val hasWaitingStar: Boolean? = null,
)

interface OfflineCache {
    val snapshot: Flow<OfflineSnapshot?>

    /** Changes the snapshot of [ownerId]; data saved for another user is dropped first. */
    suspend fun update(ownerId: String, change: (OfflineSnapshot) -> OfflineSnapshot)

    /** Removes everything, e.g. after logging out. */
    suspend fun clear()
}

/** The saved data of [ownerId], or null when nothing was saved for them. */
suspend fun OfflineCache.snapshotFor(ownerId: String): OfflineSnapshot? = snapshot.first()?.takeIf { it.ownerId == ownerId }

/** The snapshot [ownerId] should build on: their own, or an empty one when it belongs to someone else. */
internal fun OfflineSnapshot?.forOwner(ownerId: String): OfflineSnapshot =
    this?.takeIf { it.ownerId == ownerId } ?: OfflineSnapshot(ownerId)

/** Keeps the user's own sharing settings and position; drops everything shared with a partner. */
internal fun OfflineSnapshot.withoutCouple(): OfflineSnapshot =
    OfflineSnapshot(ownerId = ownerId, sharing = sharing, myLocation = myLocation)
