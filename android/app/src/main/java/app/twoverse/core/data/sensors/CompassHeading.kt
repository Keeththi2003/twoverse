package app.twoverse.core.data.sensors

import app.twoverse.core.common.AngleSmoother
import app.twoverse.core.common.trueHeading
import app.twoverse.core.data.HeadingSource
import app.twoverse.core.model.HeadingSample
import app.twoverse.core.model.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import javax.inject.Inject

/** Smoothed true-north heading for the compass needles (FR-CMP-2, FR-CMP-3, NFR-PRF-2). */
class CompassHeading @Inject constructor(
    private val source: HeadingSource,
    private val clock: Clock,
) {
    val isAvailable: Boolean get() = source.isAvailable

    /**
     * Headings while collected (the sensor is off otherwise). [myLocation] gives the local
     * magnetic declination; without a location, magnetic north is used.
     */
    fun headings(myLocation: Flow<UserLocation?>): Flow<HeadingSample> {
        if (!source.isAvailable) return emptyFlow()
        return flow {
            val smoother = AngleSmoother(SmoothingFactor)
            combine(source.readings(), myLocation) { reading, location ->
                val declination = location?.let {
                    source.declinationDegrees(it.latitude, it.longitude, clock.instant())
                } ?: 0.0
                HeadingSample(
                    degrees = smoother.smooth(trueHeading(reading.magneticDegrees, declination)),
                    isAccurate = reading.isAccurate,
                )
            }.collect { emit(it) }
        }
    }

    companion object {
        /** Share of each new reading applied per sample: steady needle, still quick to follow. */
        const val SmoothingFactor = 0.15
    }
}
