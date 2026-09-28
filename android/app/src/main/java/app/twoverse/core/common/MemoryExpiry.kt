package app.twoverse.core.common

import app.twoverse.core.model.MemoryExpiry
import java.time.Duration
import java.time.Instant

/** Time left before a temporary memory expires, as shown on vault badges ("24h", "7d") (FR-VLT-4). */
sealed interface ExpiryBadge {
    val amount: Long

    data class Hours(override val amount: Long) : ExpiryBadge
    data class Days(override val amount: Long) : ExpiryBadge
}

private const val HoursPerDay = 24L

/** When a memory sent at [sentAt] with this expiry disappears; null for Never (FR-MEM-4). */
fun MemoryExpiry.expiresAt(sentAt: Instant): Instant? = duration?.let { sentAt + it }

/** Whether a memory with this expiry can no longer be opened (BR-6). */
fun isExpired(expiresAt: Instant?, now: Instant): Boolean = expiresAt != null && !expiresAt.isAfter(now)

/**
 * Up to a day left shows whole hours rounded up ("24h", "1h"); more shows days rounded to the
 * nearest day, so a fresh "7 days" memory reads "7d" and 25 hours reads "1d".
 * Null for memories that never expire or have already expired.
 */
fun expiryBadge(expiresAt: Instant?, now: Instant): ExpiryBadge? {
    if (expiresAt == null || isExpired(expiresAt, now)) return null
    val left = Duration.between(now, expiresAt)
    val hours = ceilDiv(left.toMinutes(), MinutesPerHour).coerceAtLeast(1)
    return if (hours <= HoursPerDay) ExpiryBadge.Hours(hours) else ExpiryBadge.Days((hours + HoursPerDay / 2) / HoursPerDay)
}

private const val MinutesPerHour = 60L

private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1) / divisor
