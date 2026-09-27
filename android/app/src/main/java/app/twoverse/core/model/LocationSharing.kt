package app.twoverse.core.model

/** The user's location sharing choice (FR-LOC-2, FR-LOC-6). Off by default. */
data class LocationSharing(
    val enabled: Boolean = false,
    val precision: LocationPrecision = LocationPrecision.Approximate,
)

/** A position reported by the phone, before it is rounded and uploaded. */
data class DevicePosition(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
)

/** Which location permissions the user has granted (FR-LOC-1). */
data class LocationPermissionStatus(
    val foreground: Boolean,
    val background: Boolean,
)
