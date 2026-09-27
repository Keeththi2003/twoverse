package app.twoverse.core.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.twoverse.core.data.LocationPermissionChecker
import app.twoverse.core.model.LocationPermissionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidLocationPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationPermissionChecker {

    /** Coarse is enough to share (approximate rounding happens anyway). Before Android 10 background came with it. */
    override fun status(): LocationPermissionStatus {
        val foreground = granted(Manifest.permission.ACCESS_COARSE_LOCATION) || granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val background = foreground &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        return LocationPermissionStatus(foreground = foreground, background = background)
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
