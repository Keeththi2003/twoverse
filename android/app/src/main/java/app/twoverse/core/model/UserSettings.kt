package app.twoverse.core.model

data class UserSettings(
    val shareLocation: Boolean,
    val locationPrecision: LocationPrecision,
    val lockOurs: Boolean,
    val distanceUnit: DistanceUnit,
    val appearance: AppearanceMode,
)

enum class DistanceUnit { Kilometres, Miles }

enum class AppearanceMode { System, Light, Dark }
