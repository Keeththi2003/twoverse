package app.twoverse.core.model

import java.time.Instant

/** A user's latest known location. Only the latest one is ever kept (FR-LOC-7). */
data class UserLocation(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val precision: LocationPrecision,
    val city: String?,
    val updatedAt: Instant,
)

enum class LocationPrecision { Approximate, Precise }
