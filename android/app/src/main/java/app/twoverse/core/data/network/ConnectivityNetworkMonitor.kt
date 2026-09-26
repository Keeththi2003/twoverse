package app.twoverse.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val online = mutableSetOf<Network>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online += network
                trySend(true)
            }

            override fun onLost(network: Network) {
                online -= network
                trySend(online.isNotEmpty())
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        val active = manager.getNetworkCapabilities(manager.activeNetwork)
        trySend(active?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()
}
