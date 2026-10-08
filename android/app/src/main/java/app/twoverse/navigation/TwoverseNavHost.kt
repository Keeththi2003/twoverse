package app.twoverse.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoverseBottomBar
import app.twoverse.core.designsystem.component.TwoverseBottomBarItem
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.LaunchScreen
import app.twoverse.feature.auth.ResetPasswordRoute as ResetPasswordFeatureRoute
import app.twoverse.feature.auth.SignInRoute as SignInFeatureRoute
import app.twoverse.feature.auth.SignUpRoute as SignUpFeatureRoute
import app.twoverse.feature.auth.SignedInDestination
import app.twoverse.feature.compass.CompassRoute as CompassFeatureRoute
import app.twoverse.feature.countdown.CountdownRoute as CountdownFeatureRoute
import app.twoverse.feature.countdown.EditReunionRoute as EditReunionFeatureRoute
import app.twoverse.feature.home.HomeRoute as HomeFeatureRoute
import app.twoverse.feature.location.LocationSetupRoute as LocationSetupFeatureRoute
import app.twoverse.feature.onboarding.WelcomeRoute as WelcomeFeatureRoute
import app.twoverse.feature.orbit.MeetupEditorRoute as MeetupEditorFeatureRoute
import app.twoverse.feature.orbit.OrbitRoute as OrbitFeatureRoute
import app.twoverse.feature.orbit.TogetherSinceRoute as TogetherSinceFeatureRoute
import app.twoverse.feature.pairing.PairRoute as PairFeatureRoute
import app.twoverse.feature.pairing.ReconnectRoute as ReconnectFeatureRoute
import app.twoverse.feature.profile.AboutYouRoute as AboutYouFeatureRoute
import app.twoverse.feature.profile.ProfileRoute as ProfileFeatureRoute
import app.twoverse.feature.settings.SettingsRoute as SettingsFeatureRoute
import app.twoverse.feature.splash.SplashDestination
import app.twoverse.feature.splash.SplashRoute as SplashFeatureRoute
import app.twoverse.feature.star.ShootingStarRoute as ShootingStarFeatureRoute
import app.twoverse.feature.star.ShootingStarsRoute as ShootingStarsFeatureRoute
import app.twoverse.feature.star.StarComposerRoute as StarComposerFeatureRoute
import app.twoverse.feature.vault.AddMemoryRoute as AddMemoryFeatureRoute
import app.twoverse.feature.vault.MemoryRoute as MemoryFeatureRoute
import app.twoverse.feature.vault.VaultRoute as VaultFeatureRoute
import kotlinx.coroutines.launch

/**
 * App navigation (DESIGN.md §7). [startsSignedIn] starts on Our Universe instead of the splash,
 * when a notification or the widget opened the app; [requestedScreen] then opens the screen it
 * asked for (FR-NOT, FR-WGT-4). [isOffline] shows the offline banner above every screen.
 * [isPasswordRecovery] opens Reset password after a reset link (FR-AUTH-3).
 * [needsAboutYou] asks for the short name and pronouns once signed in (FR-PRO-2).
 */
@Composable
fun TwoverseNavHost(
    modifier: Modifier = Modifier,
    startsSignedIn: Boolean = false,
    requestedScreen: LaunchScreen? = null,
    onRequestedScreenShown: () -> Unit = {},
    isOffline: Boolean = false,
    isPasswordRecovery: Boolean = false,
    onPasswordRecoveryShown: () -> Unit = {},
    needsAboutYou: Boolean = false,
) {
    val navController = rememberNavController()
    val currentOnPasswordRecoveryShown by rememberUpdatedState(onPasswordRecoveryShown)
    val currentOnRequestedScreenShown by rememberUpdatedState(onRequestedScreenShown)
    LaunchedEffect(requestedScreen) {
        when (requestedScreen) {
            null -> return@LaunchedEffect
            LaunchScreen.Home -> navController.navigateToTab(TopLevelDestination.Home)
            LaunchScreen.Vault -> navController.navigateToTab(TopLevelDestination.Vault)
            LaunchScreen.Countdown -> navController.navigate(CountdownRoute) { launchSingleTop = true }
            LaunchScreen.ShootingStar -> navController.navigate(ShootingStarRoute()) { launchSingleTop = true }
            LaunchScreen.Orbit -> navController.navigateToTab(TopLevelDestination.Orbit)
        }
        currentOnRequestedScreenShown()
    }
    LaunchedEffect(isPasswordRecovery) {
        if (isPasswordRecovery) {
            navController.navigate(ResetPasswordRoute) { launchSingleTop = true }
            currentOnPasswordRecoveryShown()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val connectedMessage = stringResource(R.string.pair_connected)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    // Saving About you refreshes the profile a moment later; don't ask again in between.
    var aboutYouDone by remember { mutableStateOf(false) }
    LaunchedEffect(needsAboutYou, destination) {
        if (!needsAboutYou) {
            aboutYouDone = false
        } else if (!aboutYouDone && destination != null && destination.isSignedInScreen()) {
            navController.navigate(AboutYouRoute) { launchSingleTop = true }
        }
    }
    val selectedTab = TopLevelDestination.entries.indexOfFirst { tab ->
        destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    Scaffold(
        modifier = modifier,
        containerColor = TwoverseTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = { if (isOffline) OfflineBanner() },
        snackbarHost = { TwoverseSnackbarHost(snackbarHostState) },
        bottomBar = {
            if (selectedTab >= 0) {
                TwoverseBottomBar(
                    items = TopLevelDestination.entries.map {
                        TwoverseBottomBarItem(label = stringResource(it.labelRes), icon = it.iconRes)
                    },
                    selectedIndex = selectedTab,
                    onItemSelected = { navController.navigateToTab(TopLevelDestination.entries[it]) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startsSignedIn) HomeRoute else SplashRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            onboardingGraph(
                navController = navController,
                onConnected = { scope.launch { snackbarHostState.showSnackbar(connectedMessage) } },
            )
            tabsGraph(navController)
            composable<AboutYouRoute> {
                AboutYouFeatureRoute(
                    onDone = {
                        aboutYouDone = true
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

/** [onConnected] confirms pairing (FR-PAIR-6); a waiting Shooting Star is its own confirmation. */
private fun NavGraphBuilder.onboardingGraph(navController: NavHostController, onConnected: () -> Unit) {
    composable<SplashRoute> {
        SplashFeatureRoute(
            onNavigate = { destination -> navController.navigateClearingBackStack(destination.toRoute()) },
        )
    }
    composable<WelcomeRoute> {
        WelcomeFeatureRoute(
            onGetStarted = { navController.navigate(SignInRoute) },
            // Pairing needs an account, so the couple code is entered after signing in.
            onHaveCoupleCode = { navController.navigate(SignInRoute) },
        )
    }
    composable<SignInRoute> {
        SignInFeatureRoute(
            onSignedIn = { destination ->
                navController.navigateClearingBackStack(
                    when (destination) {
                        SignedInDestination.Pair -> PairRoute
                        SignedInDestination.Home -> HomeRoute
                    },
                )
            },
            onCreateAccount = { navController.navigate(SignUpRoute) },
        )
    }
    composable<SignUpRoute> {
        SignUpFeatureRoute(
            onBack = { navController.popBackStack() },
            onSignedUp = { navController.navigateClearingBackStack(PairRoute) },
        )
    }
    composable<ResetPasswordRoute> {
        ResetPasswordFeatureRoute(onSaved = { navController.navigateClearingBackStack(SplashRoute) })
    }
    composable<PairRoute> {
        PairFeatureRoute(
            onBack = if (navController.previousBackStackEntry != null) {
                { navController.popBackStack() }
            } else {
                null
            },
            onConnected = { showShootingStar, askTogetherSince ->
                if (askTogetherSince) {
                    navController.navigateClearingBackStack(
                        TogetherSinceRoute(afterPairing = true, showShootingStarNext = showShootingStar),
                    )
                } else if (showShootingStar) {
                    navController.navigateClearingBackStack(ShootingStarRoute())
                } else {
                    navController.navigateClearingBackStack(HomeRoute)
                    onConnected()
                }
            },
            onReconnect = { navController.navigate(ReconnectRoute) },
            onSignedOut = { navController.navigateClearingBackStack(WelcomeRoute) },
        )
    }
    composable<TogetherSinceRoute> { entry ->
        val route = entry.toRoute<TogetherSinceRoute>()
        TogetherSinceFeatureRoute(
            onDone = {
                when {
                    !route.afterPairing -> navController.popBackStack()
                    route.showShootingStarNext -> navController.navigateClearingBackStack(ShootingStarRoute())
                    else -> {
                        navController.navigateClearingBackStack(HomeRoute)
                        onConnected()
                    }
                }
            },
        )
    }
    composable<ReconnectRoute> {
        ReconnectFeatureRoute(
            onBack = { navController.popBackStack() },
            onReconnected = {
                navController.navigateClearingBackStack(HomeRoute)
                onConnected()
            },
        )
    }
    composable<ShootingStarRoute> {
        ShootingStarFeatureRoute(
            onDone = {
                // Opened from the splash or pairing it is the root; from a list or notification it returns.
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    navController.navigateClearingBackStack(HomeRoute)
                }
            },
        )
    }
}

private fun NavGraphBuilder.tabsGraph(navController: NavHostController) {
    composable<HomeRoute> {
        HomeFeatureRoute(
            onOpenCompass = { navController.navigateToTab(TopLevelDestination.Compass) },
            onOpenCountdown = { navController.navigate(CountdownRoute) },
            onOpenVault = { navController.navigateToTab(TopLevelDestination.Vault) },
            onSendMemory = { navController.navigate(AddMemoryRoute) },
            onSendShootingStar = { navController.navigate(StarComposerRoute()) },
            onOpenLocationSetup = { navController.navigate(LocationSetupRoute) },
            onOpenOrbit = { navController.navigateToTab(TopLevelDestination.Orbit) },
            onSetTogetherSince = { navController.navigate(TogetherSinceRoute()) },
            onRecordMeetup = { navController.navigate(MeetupEditorRoute(fromReunion = true)) },
        )
    }
    composable<OrbitRoute> {
        OrbitFeatureRoute(
            onSetTogetherSince = { navController.navigate(TogetherSinceRoute()) },
            onAddMeetup = { navController.navigate(MeetupEditorRoute()) },
            onEditMeetup = { id -> navController.navigate(MeetupEditorRoute(meetupId = id)) },
        )
    }
    composable<MeetupEditorRoute> {
        MeetupEditorFeatureRoute(onDone = { navController.popBackStack() })
    }
    composable<LocationSetupRoute> {
        LocationSetupFeatureRoute(onDone = { navController.popBackStack() })
    }
    composable<CompassRoute> {
        CompassFeatureRoute()
    }
    composable<VaultRoute> {
        VaultFeatureRoute(
            onOpenMemory = { id -> navController.navigate(MemoryRoute(memoryId = id)) },
            onAddMemory = { navController.navigate(AddMemoryRoute) },
        )
    }
    composable<SettingsRoute> {
        SettingsFeatureRoute(
            onSignedOut = { navController.navigateClearingBackStack(WelcomeRoute) },
            onDisconnected = { navController.navigateClearingBackStack(PairRoute) },
            onSendShootingStar = { navController.navigate(StarComposerRoute()) },
            onOpenShootingStars = { navController.navigate(ShootingStarsRoute) },
            onOpenLocationSetup = { navController.navigate(LocationSetupRoute) },
            onOpenProfile = { navController.navigate(ProfileRoute) },
        )
    }
    composable<ProfileRoute> {
        ProfileFeatureRoute(onBack = { navController.popBackStack() })
    }
    composable<StarComposerRoute> {
        StarComposerFeatureRoute(onDone = { navController.popBackStack() })
    }
    composable<ShootingStarsRoute> {
        ShootingStarsFeatureRoute(
            onBack = { navController.popBackStack() },
            onCompose = { navController.navigate(StarComposerRoute()) },
            onEdit = { id -> navController.navigate(StarComposerRoute(starId = id)) },
            onOpenReceived = { id -> navController.navigate(ShootingStarRoute(starId = id)) },
        )
    }
    composable<CountdownRoute> {
        CountdownFeatureRoute(
            onBack = { navController.popBackStack() },
            onEditPlan = { navController.navigate(EditReunionRoute) },
        )
    }
    composable<EditReunionRoute> {
        EditReunionFeatureRoute(onDone = { navController.popBackStack() })
    }
    composable<MemoryRoute> {
        MemoryFeatureRoute(onBack = { navController.popBackStack() })
    }
    composable<AddMemoryRoute> {
        AddMemoryFeatureRoute(
            onClose = { navController.popBackStack() },
            onSent = {
                navController.popBackStack()
                navController.navigateToTab(TopLevelDestination.Vault)
            },
        )
    }
}

@Composable
private fun TwoverseSnackbarHost(hostState: SnackbarHostState) {
    val colors = TwoverseTheme.colors
    SnackbarHost(hostState = hostState) { data ->
        Snackbar(
            snackbarData = data,
            shape = TwoverseTheme.shapes.input,
            containerColor = colors.onSurface,
            contentColor = colors.background,
        )
    }
}

/** Screens shown before signing in, and About you itself, never open About you. */
private fun NavDestination.isSignedInScreen(): Boolean =
    listOf(SplashRoute::class, WelcomeRoute::class, SignInRoute::class, SignUpRoute::class, ResetPasswordRoute::class, AboutYouRoute::class)
        .none { hasRoute(it) }

private fun SplashDestination.toRoute(): Any = when (this) {
    SplashDestination.Welcome -> WelcomeRoute
    SplashDestination.Pair -> PairRoute
    SplashDestination.ShootingStar -> ShootingStarRoute()
    SplashDestination.Home -> HomeRoute
}

/** Switches tabs, keeping Home as the root and restoring each tab's own back stack. */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Opens [route] as the new root, so Back leaves the app instead of returning to onboarding. */
private fun NavHostController.navigateClearingBackStack(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
    }
}
