package app.twoverse.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.PushRepository
import app.twoverse.core.model.MemoryDraft
import app.twoverse.core.model.MemoryExpiry
import app.twoverse.core.model.MemoryUpload
import app.twoverse.core.model.PartnerPush
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
    private val pushRepository: PushRepository,
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

    /**
     * Uploads the memory with progress; on failure the photo, caption and options stay so the
     * user can retry (FR-MEM-6). Once sent, the partner is notified without a preview (FR-NOT-1).
     */
    fun onSend() {
        val current = state.value
        val photoUri = current.photoUri ?: return
        if (current.isSending) return
        state.update { it.copy(isSending = true, sendFailed = false, uploadProgress = 0f) }
        val draft = MemoryDraft(
            photoUri = photoUri,
            caption = current.caption.trim().ifEmpty { null },
            expiry = current.expiry,
            allowKeep = current.showKeepOption && current.allowKeep,
        )
        viewModelScope.launch {
            memoryRepository.send(draft).collect { upload ->
                when (upload) {
                    is MemoryUpload.Uploading -> state.update { it.copy(uploadProgress = upload.fraction.coerceIn(0f, 1f)) }
                    is MemoryUpload.Sent -> {
                        pushRepository.sendToPartner(PartnerPush.NewMemory)
                        state.update { it.copy(isSending = false, uploadProgress = 1f, isSent = true) }
                    }
                    is MemoryUpload.Failed -> state.update { it.copy(isSending = false, sendFailed = true) }
                }
            }
            state.update { if (it.isSending) it.copy(isSending = false, sendFailed = true) else it }
        }
    }
}
