package app.twoverse.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object WelcomeRoute

@Serializable
data object SignInRoute

@Serializable
data object PairRoute

@Serializable
data object BirthdayRoute

@Serializable
data object HomeRoute

@Serializable
data object CompassRoute

@Serializable
data object VaultRoute

@Serializable
data object SettingsRoute

@Serializable
data object CountdownRoute

@Serializable
data class MemoryRoute(val memoryId: String)

@Serializable
data object AddMemoryRoute
