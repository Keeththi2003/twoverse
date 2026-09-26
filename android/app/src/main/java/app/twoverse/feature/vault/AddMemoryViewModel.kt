package app.twoverse.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.model.MemoryDraft
import app.twoverse.core.model.MemoryExpiry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddMemoryViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
) : ViewModel() {

    private val state = MutableStateFlow(AddMemoryUiState())
    val uiState: StateFlow<AddMemoryUiState> = state.asStateFlow()

    fun onPhotoPicked(uri: String) {
        state.update { it.copy(photoUri = uri, sendFailed = false) }
    }

    fun onCaptionChange(caption: String) {
        state.update { it.copy(caption = caption.take(AddMemoryUiState.CaptionMaxLength)) }
    }

    fun onExpirySelected(expiry: MemoryExpiry) {
        state.update { it.copy(expiry = expiry) }
    }

    fun onAllowKeepChange(allow: Boolean) {
        state.update { it.copy(allowKeep = allow) }
    }

    /** Sends the memory; on failure the caption and photo stay so the user can retry (FR-MEM-6). */
    fun onSend() {
        val current = state.value
        val photoUri = current.photoUri ?: return
        if (current.isSending) return
        state.update { it.copy(isSending = true, sendFailed = false) }
        viewModelScope.launch {
            val result = memoryRepository.send(
                MemoryDraft(
                    photoUri = photoUri,
                    caption = current.caption.trim().ifEmpty { null },
                    expiry = current.expiry,
                    allowKeep = current.showKeepOption && current.allowKeep,
                ),
            )
            state.update {
                it.copy(isSending = false, isSent = result.isSuccess, sendFailed = result.isFailure)
            }
        }
    }
}
