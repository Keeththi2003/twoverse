package app.twoverse.core.data.sensors

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import app.twoverse.core.data.HeadingSource
import app.twoverse.core.model.HeadingReading
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Device heading from the rotation vector sensor (FR-CMP-2, FR-CMP-3). */
@Singleton
class RotationVectorHeadingSource @Inject constructor(
    @ApplicationContext context: Context,
) : HeadingSource {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    override val isAvailable: Boolean = sensor != null

    override fun readings(): Flow<HeadingReading> = callbackFlow {
        val rotationSensor = sensor ?: run {
            close()
            return@callbackFlow
        }
        val rotation = FloatArray(MatrixSize)
        val remapped = FloatArray(MatrixSize)
        val orientation = FloatArray(OrientationSize)
        var accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                val (axisX, axisY) = screenAxes()
                SensorManager.remapCoordinateSystem(rotation, axisX, axisY, remapped)
                SensorManager.getOrientation(remapped, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble())
                trySend(HeadingReading(magneticDegrees = azimuth, isAccurate = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
            }

            override fun onAccuracyChanged(sensor: Sensor, newAccuracy: Int) {
                accuracy = newAccuracy
            }
        }
        // Game rate (about 50 Hz) keeps the needle smooth at 30+ fps (NFR-PRF-2).
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    override fun declinationDegrees(latitude: Double, longitude: Double, at: Instant): Double =
        GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, at.toEpochMilli()).declination.toDouble()

    /** Keeps "up" pointing to the top of the screen in any orientation. */
    private fun screenAxes(): Pair<Int, Int> = when (displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }

    private companion object {
        const val MatrixSize = 9
        const val OrientationSize = 3
    }
}
