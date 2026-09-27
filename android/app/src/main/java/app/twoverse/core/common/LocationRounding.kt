package app.twoverse.core.common

import app.twoverse.core.model.DevicePosition
import app.twoverse.core.model.LocationPrecision
import kotlin.math.roundToLong

/** Two decimal places is about 1.1 km, matching the server's rounding (FR-LOC-6). */
private const val ApproximateScale = 100.0
private const val ApproximateAccuracyMeters = 1_000f

/** Rounds an approximate position to about 1 km before it leaves the phone; precise is unchanged. */
fun DevicePosition.forPrecision(precision: LocationPrecision): DevicePosition = when (precision) {
    LocationPrecision.Precise -> this
    LocationPrecision.Approximate -> DevicePosition(
        latitude = (latitude * ApproximateScale).roundToLong() / ApproximateScale,
        longitude = (longitude * ApproximateScale).roundToLong() / ApproximateScale,
        accuracyMeters = maxOf(accuracyMeters, ApproximateAccuracyMeters),
    )
}
