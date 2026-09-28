package app.twoverse.core.data.local

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import app.twoverse.core.data.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether Ours was unlocked with device authentication in this session (FR-VLT-5). Kept in
 * memory only: it locks again whenever Twoverse leaves the screen.
 */
@Singleton
class OursLock @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val unlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = unlocked.asStateFlow()

    fun unlock() {
        unlocked.value = true
    }

    /** Locks whenever the app goes to the background. */
    fun start() {
        appScope.launch {
            ProcessLifecycleOwner.get().lifecycle.currentStateFlow
                .filter { !it.isAtLeast(Lifecycle.State.STARTED) }
                .collect { unlocked.value = false }
        }
    }
}
