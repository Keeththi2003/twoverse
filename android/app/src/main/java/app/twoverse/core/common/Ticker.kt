package app.twoverse.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant

private const val DefaultTickMillis = 1_000L

/** Emits the current instant now and then every [periodMillis]; drives live clocks and "ago" text. */
fun Clock.ticks(periodMillis: Long = DefaultTickMillis): Flow<Instant> = flow {
    while (true) {
        emit(instant())
        delay(periodMillis)
    }
}
