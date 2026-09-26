package app.twoverse.core.model

import java.time.Instant

/** One-time pairing code in the format `XXXX-XXXX`, valid for 24 hours (FR-PAIR-1). */
data class CoupleCode(
    val code: String,
    val expiresAt: Instant,
)
