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

/** [replay] shows the welcome again from Settings (FR-BDY-4) and returns there afterwards. */
@Serializable
data class BirthdayRoute(val replay: Boolean = false)

@Serializable
data object BirthdayMessageRoute

@Serializable
data object ReconnectRoute

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
