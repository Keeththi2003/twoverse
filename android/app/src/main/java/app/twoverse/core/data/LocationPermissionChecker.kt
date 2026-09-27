package app.twoverse.core.data

import app.twoverse.core.model.LocationPermissionStatus

/** Reads the current location permissions; the user can change them in system settings any time. */
fun interface LocationPermissionChecker {
    fun status(): LocationPermissionStatus
}
