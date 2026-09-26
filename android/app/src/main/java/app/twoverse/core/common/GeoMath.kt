package app.twoverse.core.common

import app.twoverse.core.model.UserLocation
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EarthRadiusKm = 6371.0088

/** Great-circle (haversine) distance in kilometres (FR-LOC-8). */
fun distanceKm(from: UserLocation, to: UserLocation): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EarthRadiusKm * asin(sqrt(h))
}

/** Initial bearing from [from] to [to], in degrees clockwise from true north, 0 until 360 (FR-CMP-1). */
fun initialBearingDegrees(from: UserLocation, to: UserLocation): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}
