package app.twoverse.core.data.fake

import app.twoverse.core.data.HeadingSource
import app.twoverse.core.model.HeadingReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.Instant

/** A compass sensor a test controls. */
class FakeHeadingSource(
    override val isAvailable: Boolean = true,
    private val declination: Double = 0.0,
) : HeadingSource {
    val readings = MutableSharedFlow<HeadingReading>(replay = 1)

    override fun readings(): Flow<HeadingReading> = readings

    override fun declinationDegrees(latitude: Double, longitude: Double, at: Instant): Double = declination
}
