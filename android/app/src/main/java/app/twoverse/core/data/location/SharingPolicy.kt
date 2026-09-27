package app.twoverse.core.data.location

import app.twoverse.core.model.LocationPermissionStatus
import app.twoverse.core.model.LocationSharing

/** What should run right now to share this user's location. */
data class SharingMode(
    /** Frequent updates while the app is open (FR-LOC-3). */
    val trackInForeground: Boolean,
    /** WorkManager and significant-movement updates (FR-LOC-4); needs "Allow all the time". */
    val runInBackground: Boolean,
)

fun sharingMode(appInForeground: Boolean, sharing: LocationSharing, permissions: LocationPermissionStatus): SharingMode =
    SharingMode(
        trackInForeground = appInForeground && sharing.enabled && permissions.foreground,
        runInBackground = sharing.enabled && permissions.background,
    )
