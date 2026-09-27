package app.twoverse.core.data.push

import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Allows an action at most once per [interval]; the server enforces the same limit (FR-LOC-5). */
class PingThrottle(private val clock: Clock, private val interval: Duration) {
    private var lastAllowed: Instant? = null

    @Synchronized
    fun tryAcquire(): Boolean {
        val now = clock.instant()
        val last = lastAllowed
        if (last != null && Duration.between(last, now) < interval) return false
        lastAllowed = now
        return true
    }
}
