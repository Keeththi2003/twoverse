package app.twoverse.core.common

import java.time.Instant
import java.time.ZoneId

/** Where the couple is relative to their reunion (FR-CNT-3, FR-CNT-5). */
enum class ReunionPhase {
    /** Still counting down. */
    Upcoming,

    /** The countdown reached zero and it is still the reunion day in the user's time zone. */
    Today,

    /** The reunion day is over: time to ask "When's the next time?". */
    Past,
}

fun reunionPhase(meetAt: Instant, now: Instant, zone: ZoneId): ReunionPhase = when {
    now.isBefore(meetAt) -> ReunionPhase.Upcoming
    meetAt.atZone(zone).toLocalDate() == now.atZone(zone).toLocalDate() -> ReunionPhase.Today
    else -> ReunionPhase.Past
}
