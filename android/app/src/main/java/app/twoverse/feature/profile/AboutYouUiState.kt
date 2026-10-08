package app.twoverse.feature.profile

import app.twoverse.core.model.DataError
import app.twoverse.core.model.Pronouns

/** "About you": what this user likes to be called and their pronouns (FR-PRO-2). */
data class AboutYouUiState(
    val isLoading: Boolean = true,
    val shortName: String = "",
    val pronouns: Pronouns? = null,
    val problem: AboutYouProblem? = null,
    val error: DataError? = null,
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
) {
    val canContinue: Boolean get() = !isLoading && !isSaving
}

enum class AboutYouProblem { NameMissing, PronounsMissing }
