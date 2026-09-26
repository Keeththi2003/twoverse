package app.twoverse.core.common

import java.time.Duration
import java.time.Instant

/** Time left until the reunion (FR-CNT-3). All parts are zero once it is reached. */
data class CountdownTime(
    val days: Long,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    val isReached: Boolean get() = days == 0L && hours == 0 && minutes == 0 && seconds == 0
}

private const val HoursPerDay = 24
private const val MinutesPerHour = 60
private const val SecondsPerMinute = 60

fun countdownUntil(target: Instant, now: Instant): CountdownTime {
    val left = Duration.between(now, target).coerceAtLeast(Duration.ZERO)
    return CountdownTime(
        days = left.toDays(),
        hours = (left.toHours() % HoursPerDay).toInt(),
        minutes = (left.toMinutes() % MinutesPerHour).toInt(),
        seconds = (left.seconds % SecondsPerMinute).toInt(),
    )
}

/** How far along the wait is, from when the date was set ([start]) to [target], as 0..1. */
fun countdownProgress(start: Instant, target: Instant, now: Instant): Float {
    val total = Duration.between(start, target)
    if (total <= Duration.ZERO) return 1f
    val elapsed = Duration.between(start, now).toMillis().toFloat()
    return (elapsed / total.toMillis()).coerceIn(0f, 1f)
}
