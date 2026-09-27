package app.twoverse.core.data

import app.twoverse.core.model.HeadingReading
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** The phone's orientation sensor. */
interface HeadingSource {
    /** False on phones without a rotation vector sensor (FR-CMP-8). */
    val isAvailable: Boolean

    /** Readings while collected; the sensor is registered only during collection. */
    fun readings(): Flow<HeadingReading>

    /** Degrees between magnetic and true north at a place and time (FR-CMP-2). */
    fun declinationDegrees(latitude: Double, longitude: Double, at: Instant): Double
}
