package app.twoverse.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object WelcomeRoute

@Serializable
data object SignInRoute

@Serializable
data object SignUpRoute

@Serializable
data object ResetPasswordRoute

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
data object LocationSetupRoute

@Serializable
data object CountdownRoute

@Serializable
data object EditReunionRoute

/** [memoryId] is read by `MemoryViewModel` under the same name (`MemoryIdKey`). */
@Serializable
data class MemoryRoute(val memoryId: String)

@Serializable
data object AddMemoryRoute
