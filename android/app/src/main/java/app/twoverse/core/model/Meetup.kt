package app.twoverse.core.model

import java.time.Instant
import java.time.LocalDate

/** A time the couple met in person (FR-ORB-3). */
data class Meetup(
    val id: String,
    val startDate: LocalDate,
    /** The last day of a visit lasting several days; null for a single day. */
    val endDate: LocalDate?,
    val place: String?,
    val note: String?,
    /** The reunion this meetup records, if it came from Until We Meet (FR-ORB-10). */
    val fromReunionAt: Instant? = null,
) {
    /** The last day together; the start date for a single-day meetup. */
    val lastDay: LocalDate get() = endDate ?: startDate
}

/** What either partner saves. */
data class MeetupDraft(
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val place: String?,
    val note: String?,
    /** Set when recording a passed reunion, so it is recorded only once. */
    val fromReunionAt: Instant? = null,
) {
    companion object {
        /** Match the meetups table. */
        const val MaxPlaceLength = 100
        const val MaxNoteLength = 300
    }
}
