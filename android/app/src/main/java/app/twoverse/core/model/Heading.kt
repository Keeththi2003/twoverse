package app.twoverse.core.model

/** One reading of the phone's direction, from the rotation vector sensor. */
data class HeadingReading(
    /** Degrees clockwise from magnetic north. */
    val magneticDegrees: Double,
    /** False while the sensor reports low accuracy and needs calibrating (FR-CMP-5). */
    val isAccurate: Boolean,
)

/** Smoothed true-north heading; [degrees] is continuous and may leave 0..360. */
data class HeadingSample(
    val degrees: Double,
    val isAccurate: Boolean,
)
