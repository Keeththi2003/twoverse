package app.twoverse.feature.splash

sealed interface SplashUiState {
    data object Loading : SplashUiState

    data class Ready(val destination: SplashDestination) : SplashUiState
}

/** Where the app goes after the splash (FR-ONB-3). */
enum class SplashDestination { Welcome, Pair, Birthday, Home }
