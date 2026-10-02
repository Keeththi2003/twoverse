package app.twoverse.feature.star

import app.twoverse.core.model.DataError
import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarStatus
import java.time.LocalDateTime

/** Sent and received Shooting Stars (FR-STAR-9, FR-STAR-13). */
data class ShootingStarsUiState(
    val isLoading: Boolean = true,
    /** Stars this user sent, newest first. */
    val sent: List<StarListItem> = emptyList(),
    /** Stars the partner sent that have become visible, newest first. */
    val received: List<StarListItem> = emptyList(),
    /** The sent stars couldn't be loaded. */
    val loadError: DataError? = null,
    /** The sent star waiting for delete confirmation. */
    val pendingDeleteId: String? = null,
    /** Deleting failed, e.g. the partner saw it in the meantime. */
    val error: DataError? = null,
)

data class StarListItem(
    val id: String,
    /** The title, else the start of the message; null names the layout instead. */
    val headline: String?,
    val layout: StarLayout,
    val status: StarStatus,
    /** Local time it shows (scheduled), or when it became visible. */
    val time: LocalDateTime,
    /** Still unseen, so the sender can edit or delete it. */
    val isEditable: Boolean,
)
