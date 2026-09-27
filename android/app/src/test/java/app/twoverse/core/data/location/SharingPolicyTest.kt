package app.twoverse.core.data.location

import app.twoverse.core.model.LocationPermissionStatus
import app.twoverse.core.model.LocationSharing
import org.junit.Assert.assertEquals
import org.junit.Test

class SharingPolicyTest {

    private val on = LocationSharing(enabled = true)
    private val off = LocationSharing(enabled = false)
    private val all = LocationPermissionStatus(foreground = true, background = true)
    private val whileUsing = LocationPermissionStatus(foreground = true, background = false)
    private val none = LocationPermissionStatus(foreground = false, background = false)

    @Test
    fun openAppWithAllPermissionsTracksAndSchedules() {
        assertEquals(SharingMode(trackInForeground = true, runInBackground = true), sharingMode(true, on, all))
    }

    @Test
    fun closedAppOnlyRunsTheBackgroundSchedule() {
        assertEquals(SharingMode(trackInForeground = false, runInBackground = true), sharingMode(false, on, all))
    }

    @Test
    fun whileUsingPermissionNeverRunsInTheBackground() {
        assertEquals(SharingMode(trackInForeground = true, runInBackground = false), sharingMode(true, on, whileUsing))
        assertEquals(SharingMode(trackInForeground = false, runInBackground = false), sharingMode(false, on, whileUsing))
    }

    @Test
    fun nothingRunsWhenSharingIsOff() {
        assertEquals(SharingMode(trackInForeground = false, runInBackground = false), sharingMode(true, off, all))
    }

    @Test
    fun nothingRunsWithoutPermission() {
        assertEquals(SharingMode(trackInForeground = false, runInBackground = false), sharingMode(true, on, none))
    }
}
