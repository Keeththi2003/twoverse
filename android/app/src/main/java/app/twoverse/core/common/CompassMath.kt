package app.twoverse.core.common

import kotlin.math.abs

private const val FullCircle = 360.0
private const val HalfCircle = 180.0

/** An angle in degrees brought into 0 until 360. */
fun normalizeDegrees(degrees: Double): Double = ((degrees % FullCircle) + FullCircle) % FullCircle

/** The shortest turn from [from] to [to], in -180..180 degrees (positive = clockwise). */
fun shortestTurn(from: Double, to: Double): Double {
    val turn = normalizeDegrees(to - from)
    return if (turn > HalfCircle) turn - FullCircle else turn
}

/** Magnetic heading corrected to true north with the local declination (FR-CMP-2). */
fun trueHeading(magneticDegrees: Double, declinationDegrees: Double): Double =
    normalizeDegrees(magneticDegrees + declinationDegrees)

/** Whether the phone points at the partner within [toleranceDegrees] (FR-CMP-7). */
fun isPointingAt(bearingDegrees: Double, headingDegrees: Double, toleranceDegrees: Double = 5.0): Boolean =
    abs(shortestTurn(headingDegrees, bearingDegrees)) <= toleranceDegrees

/**
 * Smooths a jittery sensor angle (NFR-PRF-2). Each sample moves a fraction [factor] of the
 * shortest turn towards the new reading, so it never spins the long way round at 359° → 0°.
 * The result is continuous (it can go past 360 or below 0) so an animated needle never jumps.
 */
class AngleSmoother(private val factor: Double) {
    private var current: Double? = null

    fun smooth(targetDegrees: Double): Double {
        val previous = current
        val next = if (previous == null) targetDegrees else previous + factor * shortestTurn(previous, targetDegrees)
        current = next
        return next
    }
}
