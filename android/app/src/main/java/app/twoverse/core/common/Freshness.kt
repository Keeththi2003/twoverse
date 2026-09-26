package app.twoverse.core.common

import java.time.Duration
import java.time.Instant

/** How recent the partner's last location update is (FR-LOC-10). */
enum class LocationFreshness { Live, Recent, Outdated, Unavailable }

/** Configurable freshness thresholds (FR-LOC-10). */
object FreshnessThresholds {
    val Live: Duration = Duration.ofMinutes(2)
    val Recent: Duration = Duration.ofMinutes(30)
}

/** [updatedAt] is null when sharing is off or there is no data. */
fun locationFreshness(updatedAt: Instant?, now: Instant): LocationFreshness {
    if (updatedAt == null) return LocationFreshness.Unavailable
    val age = Duration.between(updatedAt, now).coerceAtLeast(Duration.ZERO)
    return when {
        age <= FreshnessThresholds.Live -> LocationFreshness.Live
        age <= FreshnessThresholds.Recent -> LocationFreshness.Recent
        else -> LocationFreshness.Outdated
    }
}

/** Time since an event, in the largest whole unit, for "Updated 12 seconds ago" style text. */
sealed interface ElapsedTime {
    val amount: Long

    data class Seconds(override val amount: Long) : ElapsedTime
    data class Minutes(override val amount: Long) : ElapsedTime
    data class Hours(override val amount: Long) : ElapsedTime
    data class Days(override val amount: Long) : ElapsedTime
}

fun elapsedTime(since: Instant, now: Instant): ElapsedTime {
    val age = Duration.between(since, now).coerceAtLeast(Duration.ZERO)
    return when {
        age < Duration.ofMinutes(1) -> ElapsedTime.Seconds(age.seconds)
        age < Duration.ofHours(1) -> ElapsedTime.Minutes(age.toMinutes())
        age < Duration.ofDays(1) -> ElapsedTime.Hours(age.toHours())
        else -> ElapsedTime.Days(age.toDays())
    }
}
