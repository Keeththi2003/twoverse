package app.twoverse.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.MaxShortNameLength
import app.twoverse.core.common.normalizePhone
import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.ProfileEdit
import app.twoverse.core.model.Pronouns
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Edit your names, pronouns, phone, email and what your partner may see (FR-PRO-4). */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val state = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = state.asStateFlow()

    init {
        // The form is filled once; later updates only refresh the email, so typing isn't overwritten.
        viewModelScope.launch {
            profileRepository.myProfile.filterNotNull().collect { profile ->
                state.update { current ->
                    if (current.isLoading) {
                        current.copy(
                            isLoading = false,
                            fullName = profile.fullName,
                            shortName = profile.shortName,
                            pronouns = profile.pronouns,
                            phone = profile.phone.orEmpty(),
                            shareEmail = profile.shareEmail,
                            sharePhone = profile.sharePhone,
                            email = profile.email,
                            pendingEmail = profile.pendingEmail,
                            canChangeEmail = profile.canChangeEmail,
                        )
                    } else {
                        current.copy(email = profile.email, pendingEmail = profile.pendingEmail)
                    }
                }
            }
        }
    }

    fun onFullNameChange(name: String) = edit { it.copy(fullName = name.take(ProfileEdit.MaxFullNameLength)) }

    fun onShortNameChange(name: String) = edit { it.copy(shortName = name.take(MaxShortNameLength)) }

    fun onPronounsSelected(pronouns: Pronouns) = edit { it.copy(pronouns = pronouns) }

    fun onPhoneChange(phone: String) = edit { it.copy(phone = phone) }

    fun onShareEmailChange(share: Boolean) = edit { it.copy(shareEmail = share) }

    fun onSharePhoneChange(share: Boolean) = edit { it.copy(sharePhone = share) }

    fun onSave() {
        val current = state.value
        if (!current.canSave) return
        val phoneText = current.phone.trim()
        val phone = if (phoneText.isEmpty()) null else normalizePhone(phoneText)
        val problem = when {
            current.fullName.isBlank() -> ProfileProblem.FullNameMissing
            current.shortName.isBlank() -> ProfileProblem.ShortNameMissing
            phoneText.isNotEmpty() && phone == null -> ProfileProblem.PhoneInvalid
            else -> null
        }
        if (problem != null) {
            state.update { it.copy(problem = problem) }
            return
        }
        val edit = ProfileEdit(
            fullName = current.fullName.trim(),
            shortName = current.shortName.trim(),
            pronouns = current.pronouns,
            phone = phone,
            clearPhone = phone == null,
            shareEmail = current.shareEmail,
            // Nothing to share without a number.
            sharePhone = current.sharePhone && phone != null,
        )
        state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = profileRepository.updateMyProfile(edit)) {
                is DataResult.Success -> state.update {
                    it.copy(isSaving = false, isSaved = true, phone = phone.orEmpty(), sharePhone = edit.sharePhone == true)
                }
                is DataResult.Failure -> state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun onChangeEmail() {
        state.update { it.copy(isEmailDialogOpen = true, newEmail = "", isNewEmailInvalid = false) }
    }

    fun onNewEmailChange(email: String) {
        state.update { it.copy(newEmail = email, isNewEmailInvalid = false) }
    }

    fun onDismissEmailDialog() {
        state.update { it.copy(isEmailDialogOpen = false) }
    }

    /** Supabase sends a confirmation to the new address; the old email stays until it's confirmed. */
    fun onConfirmEmail() {
        val email = state.value.newEmail.trim()
        if (!isValidEmail(email) || email.equals(state.value.email, ignoreCase = true)) {
            state.update { it.copy(isNewEmailInvalid = true) }
            return
        }
        state.update { it.copy(isEmailDialogOpen = false, error = null) }
        viewModelScope.launch {
            when (val result = profileRepository.changeEmail(email)) {
                is DataResult.Success -> state.update { it.copy(isEmailChangeSent = true) }
                is DataResult.Failure -> state.update { it.copy(error = result.error) }
            }
        }
    }

    private fun edit(change: (ProfileUiState) -> ProfileUiState) {
        state.update { change(it).copy(problem = null, isSaved = false) }
    }
}

private val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

internal fun isValidEmail(email: String): Boolean = EmailPattern.matches(email)
