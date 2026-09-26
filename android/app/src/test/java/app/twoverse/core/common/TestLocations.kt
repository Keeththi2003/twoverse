package app.twoverse.core.common

import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.UserLocation
import java.time.Instant

internal fun location(latitude: Double, longitude: Double, updatedAt: Instant = Instant.EPOCH) = UserLocation(
    userId = "user",
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = 10f,
    precision = LocationPrecision.Precise,
    city = null,
    updatedAt = updatedAt,
)

internal val Colombo = location(6.9271, 79.8612)
internal val SamplePartner = location(7.5590, 80.4354)
