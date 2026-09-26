package app.twoverse.core.model

import java.time.Instant

/** The next time the couple meets. [meetAt] is UTC; [hasTime] is false when only a date was set. */
data class Reunion(
    val meetAt: Instant,
    val hasTime: Boolean,
    val place: String?,
    val note: String?,
)
