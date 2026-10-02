package app.twoverse.feature.orbit

import app.twoverse.core.common.Anniversary
import app.twoverse.core.common.MeetupStats
import app.twoverse.core.common.TogetherDuration
import app.twoverse.core.common.UpcomingMilestone
import app.twoverse.core.model.DataError
import app.twoverse.core.model.Meetup
import java.time.LocalDate

/** Our Orbit (FR-ORB-4 to FR-ORB-8, FR-ORB-11). */
data class OrbitUiState(
    val isLoading: Boolean = true,
    /** When the relationship began; null until a partner sets it. */
    val togetherSince: LocalDate? = null,
    val duration: TogetherDuration? = null,
    val nextAnniversary: Anniversary? = null,
    /** The next day milestone (100, 365, 500, 1000, every 1000 days). */
    val nextDayMilestone: UpcomingMilestone? = null,
    /** Today is an anniversary or milestone. */
    val celebration: Celebration? = null,
    val stats: MeetupStats = MeetupStats(timesMet = 0, daysTogether = 0, daysSinceLastMet = null),
    /** Newest first. */
    val meetups: List<Meetup> = emptyList(),
    val pendingDeleteId: String? = null,
    val error: DataError? = null,
)

/** A small celebration on the day itself (FR-ORB-11). */
sealed interface Celebration {
    data class Anniversary(val years: Int) : Celebration

    data class DayMilestone(val day: Long) : Celebration
}
