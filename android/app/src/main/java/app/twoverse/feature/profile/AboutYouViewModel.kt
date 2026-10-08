package app.twoverse.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.MaxShortNameLength
import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.ProfileEdit
import app.twoverse.core.model.Pronouns
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The step after sign-in, and once for existing users without pronouns (FR-PRO-2): the short name
 * comes prefilled from the full name; pronouns must be chosen.
 */
@HiltViewModel
class AboutYouViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val state = MutableStateFlow(AboutYouUiState())
    val uiState: StateFlow<AboutYouUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profileRepository.myProfile.filterNotNull().first()
            state.update { it.copy(isLoading = false, shortName = profile.shortName, pronouns = profile.pronouns) }
        }
    }

    fun onShortNameChange(name: String) {
        state.update { it.copy(shortName = name.take(MaxShortNameLength), problem = null) }
    }

    fun onPronounsSelected(pronouns: Pronouns) {
        state.update { it.copy(pronouns = pronouns, problem = null) }
    }

    fun onContinue() {
        val current = state.value
        if (!current.canContinue) return
        val name = current.shortName.trim()
        val problem = when {
            name.isEmpty() -> AboutYouProblem.NameMissing
            current.pronouns == null -> AboutYouProblem.PronounsMissing
            else -> null
        }
        if (problem != null) {
            state.update { it.copy(problem = problem) }
            return
        }
        state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = profileRepository.updateMyProfile(ProfileEdit(shortName = name, pronouns = current.pronouns))) {
                is DataResult.Success -> state.update { it.copy(isSaving = false, isDone = true) }
                is DataResult.Failure -> state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }
}
