package app.twoverse.core.model

data class UserSettings(
    val shareLocation: Boolean,
    val locationPrecision: LocationPrecision,
    val lockOurs: Boolean,
    val distanceUnit: DistanceUnit,
    val appearance: AppearanceMode,
)

/** Settings saved on the user's profile, so they follow them to a new phone (FR-VLT-5, FR-SET-3). */
data class ProfileSettings(
    val lockOurs: Boolean,
    val distanceUnit: DistanceUnit,
    val appearance: AppearanceMode,
)

enum class DistanceUnit { Kilometres, Miles }

enum class AppearanceMode { System, Light, Dark }
