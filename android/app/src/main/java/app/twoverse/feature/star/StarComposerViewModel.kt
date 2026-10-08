package app.twoverse.feature.star

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.defaultShortName
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.ShootingStarRepository
import app.twoverse.core.model.AuthState
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.ShootingStar
import app.twoverse.core.model.ShootingStarDraft
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarPhotoFit
import app.twoverse.core.model.validateStar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Sends a new Shooting Star, or edits an unseen one when opened with a star id (FR-STAR-1 to FR-STAR-9). */
@HiltViewModel
class StarComposerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShootingStarRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val clock: Clock,
) : ViewModel() {

    private val starId: String? = savedStateHandle[StarIdKey]
    private val state = MutableStateFlow(StarComposerUiState(isEditing = starId != null, today = LocalDate.now(clock)))
    val uiState: StateFlow<StarComposerUiState> = state.asStateFlow()

    init {
        load()
    }

    /** A new star starts signed with this user's name (FR-STAR-5); an edit loads the star. Also used to retry. */
    fun load() {
        state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            if (starId == null) {
                val name = myDisplayName().orEmpty().take(StarContent.MaxSignatureLength)
                state.update { it.copy(isLoading = false, signature = name) }
                return@launch
            }
            when (val result = repository.star(starId)) {
                is DataResult.Failure -> state.update { it.copy(isLoading = false, loadError = result.error) }
                is DataResult.Success -> {
                    val star = result.value
                    state.update {
                        if (star.isEditable) it.filledFrom(star) else it.copy(isLoading = false, loadError = DataError.StarUnavailable)
                    }
                }
            }
        }
    }

    fun onLayoutSelected(layout: StarLayout) {
        state.update { it.copy(layout = layout, problem = null) }
    }

    /** Fills in the eyebrow and title; the message, signature and photo stay as they are (FR-STAR-3). */
    fun onTemplateSelected(template: StarTemplate, text: StarTemplateText) {
        state.update {
            it.copy(
                template = template,
                eyebrow = text.eyebrow.take(StarContent.MaxEyebrowLength),
                title = text.title.take(StarContent.MaxTitleLength),
                problem = null,
            )
        }
    }

    fun onEyebrowChange(eyebrow: String) {
        state.update { it.copy(eyebrow = eyebrow.take(StarContent.MaxEyebrowLength), problem = null) }
    }

    fun onTitleChange(title: String) {
        state.update { it.copy(title = title.take(StarContent.MaxTitleLength), problem = null) }
    }

    fun onMessageChange(message: String) {
        state.update { it.copy(message = message.take(StarContent.MaxMessageLength), problem = null) }
    }

    fun onSignatureChange(signature: String) {
        state.update { it.copy(signature = signature.take(StarContent.MaxSignatureLength)) }
    }

    fun onPhotoPicked(uri: String) {
        state.update { it.copy(newPhotoUri = uri, isPhotoRemoved = false, problem = null, error = null) }
    }

    fun onRemovePhoto() {
        state.update { it.copy(newPhotoUri = null, isPhotoRemoved = true) }
    }

    fun onPhotoFitSelected(fit: StarPhotoFit) {
        state.update { it.copy(photoFit = fit) }
    }

    fun onShowNextOpen() {
        state.update { it.copy(schedule = StarSchedule.NextOpen, problem = null) }
    }

    fun onOpenPicker(picker: StarPicker) {
        state.update { it.copy(openPicker = picker) }
    }

    fun onDismissPicker() {
        state.update { it.copy(openPicker = null) }
    }

    /** Choosing a date asks for the time next; earlier dates can't be picked. */
    fun onDateSelected(date: LocalDate) {
        state.update { current ->
            if (date.isBefore(current.today)) return@update current.copy(openPicker = null)
            val time = (current.schedule as? StarSchedule.At)?.time ?: DefaultStarTime
            current.copy(schedule = StarSchedule.At(date, time), openPicker = StarPicker.Time, problem = null)
        }
    }

    fun onTimeSelected(time: LocalTime) {
        state.update { current ->
            val date = (current.schedule as? StarSchedule.At)?.date ?: current.today
            current.copy(schedule = StarSchedule.At(date, time), openPicker = null, problem = null)
        }
    }

    fun onOpenPreview() {
        state.update { it.copy(isPreviewOpen = true) }
    }

    fun onPreviewDarkChange(dark: Boolean) {
        state.update { it.copy(isPreviewDark = dark) }
    }

    fun onClosePreview() {
        state.update { it.copy(isPreviewOpen = false) }
    }

    fun onSend() {
        val current = state.value
        if (!current.canSend) return
        val content = current.content
        val showAt = current.schedule.showAt(clock.zone)
        validateStar(content, current.hasPhoto, showAt, clock.instant())?.let { problem ->
            state.update { it.copy(problem = problem) }
            return
        }
        state.update { it.copy(isSaving = true, problem = null, error = null) }
        viewModelScope.launch {
            val draft = ShootingStarDraft(content = content, photo = current.photoChange, showAt = showAt)
            when (val result = repository.save(starId, draft)) {
                is DataResult.Success -> state.update { it.copy(isSaving = false, isSaved = true) }
                is DataResult.Failure -> state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }

    /** Signed with what this user likes to be called (FR-PRO-1). */
    private suspend fun myDisplayName(): String? {
        val auth = authRepository.authState.first { it !is AuthState.Loading } as? AuthState.SignedIn ?: return null
        val me = (profileRepository.profile(auth.userId) as? DataResult.Success)?.value ?: return null
        return me.shortName ?: defaultShortName(me.fullName)
    }

    /** A star whose time has already come is simply waiting, like one sent for the next open. */
    private fun StarComposerUiState.filledFrom(star: ShootingStar): StarComposerUiState {
        val content = star.content
        val showAt = star.showAt?.takeIf { it.isAfter(clock.instant()) }?.atZone(clock.zone)
        return copy(
            isLoading = false,
            layout = content.layout,
            eyebrow = content.eyebrow.orEmpty(),
            title = content.title.orEmpty(),
            message = content.message.orEmpty(),
            signature = content.signature.orEmpty(),
            photoFit = content.photoFit,
            savedPhotoUrl = star.photoUrl,
            schedule = showAt?.let { StarSchedule.At(it.toLocalDate(), it.toLocalTime().truncatedTo(ChronoUnit.MINUTES)) }
                ?: StarSchedule.NextOpen,
        )
    }
}
