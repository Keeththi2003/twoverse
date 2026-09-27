package app.twoverse.core.data.fake

import app.twoverse.core.data.LocationPermissionChecker
import app.twoverse.core.model.LocationPermissionStatus

/** Permissions a test controls. */
class FakeLocationPermissionChecker(
    var current: LocationPermissionStatus = LocationPermissionStatus(foreground = true, background = true),
) : LocationPermissionChecker {
    override fun status(): LocationPermissionStatus = current
}
