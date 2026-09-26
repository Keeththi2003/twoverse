package app.twoverse.core.data.network

import kotlinx.coroutines.flow.Flow

/** Whether the device has a working internet connection (for the offline banner, NFR-REL-1). */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
