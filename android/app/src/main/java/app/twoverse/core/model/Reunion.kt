package app.twoverse.core.model

import java.time.Instant

/**
 * The next time the couple meets. [meetAt] is UTC (FR-CNT-6); [hasTime] is false when only a
 * date was chosen. [dateSetAt] is when the date was chosen, the start of the "Getting closer" wait.
 */
data class Reunion(
    val meetAt: Instant,
    val hasTime: Boolean,
    val place: String?,
    val note: String?,
    val dateSetAt: Instant,
)

/** What either partner can set or edit (FR-CNT-1). */
data class ReunionPlan(
    val meetAt: Instant,
    val hasTime: Boolean,
    val place: String?,
    val note: String?,
) {
    companion object {
        /** Match the reunions table. */
        const val MaxPlaceLength = 100
        const val MaxNoteLength = 500
    }
}
