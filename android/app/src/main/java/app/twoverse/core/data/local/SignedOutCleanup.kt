package app.twoverse.core.data.local

import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.data.memory.MemoryPhotoCache
import app.twoverse.core.model.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Removes the couple's saved data and photos from the device once nobody is signed in. */
@Singleton
class SignedOutCleanup @Inject constructor(
    private val authRepository: AuthRepository,
    private val offlineCache: OfflineCache,
    private val photoCache: MemoryPhotoCache,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    fun start() {
        appScope.launch {
            authRepository.authState
                .distinctUntilChanged()
                .filter { it == AuthState.SignedOut }
                .collect {
                    offlineCache.clear()
                    photoCache.clear()
                }
        }
    }
}
