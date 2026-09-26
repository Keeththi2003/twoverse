package app.twoverse.core.common

import kotlin.math.roundToInt

/** The eight compass points (FR-CMP-4). */
enum class CompassDirection {
    North, NorthEast, East, SouthEast, South, SouthWest, West, NorthWest;

    companion object {
        private const val SectorDegrees = 45.0

        fun fromBearing(bearingDegrees: Double): CompassDirection {
            val normalized = ((bearingDegrees % 360) + 360) % 360
            return entries[(normalized / SectorDegrees).roundToInt() % entries.size]
        }
    }
}
