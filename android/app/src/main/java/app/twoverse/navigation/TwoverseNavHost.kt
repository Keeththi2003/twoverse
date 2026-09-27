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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoverseBottomBar
import app.twoverse.core.designsystem.component.TwoverseBottomBarItem
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.feature.auth.ResetPasswordRoute as ResetPasswordFeatureRoute
import app.twoverse.feature.auth.SignInRoute as SignInFeatureRoute
import app.twoverse.feature.auth.SignUpRoute as SignUpFeatureRoute
import app.twoverse.feature.auth.SignedInDestination
import app.twoverse.feature.birthday.BirthdayRoute as BirthdayFeatureRoute
import app.twoverse.feature.compass.CompassRoute as CompassFeatureRoute
import app.twoverse.feature.countdown.CountdownRoute as CountdownFeatureRoute
import app.twoverse.feature.home.HomeRoute as HomeFeatureRoute
import app.twoverse.feature.onboarding.WelcomeRoute as WelcomeFeatureRoute
import app.twoverse.feature.pairing.PairRoute as PairFeatureRoute
import app.twoverse.feature.settings.SettingsRoute as SettingsFeatureRoute
import app.twoverse.feature.splash.SplashDestination
import app.twoverse.feature.splash.SplashRoute as SplashFeatureRoute
import app.twoverse.feature.vault.AddMemoryRoute as AddMemoryFeatureRoute
import app.twoverse.feature.vault.MemoryRoute as MemoryFeatureRoute
import app.twoverse.feature.vault.VaultRoute as VaultFeatureRoute
import kotlinx.coroutines.launch

/**
 * App navigation (DESIGN.md §7). [openHome] starts on Our Universe instead of the splash, for
 * widget taps (FR-WGT-4). [isOffline] shows the offline banner above every screen.
 * [isPasswordRecovery] opens Reset password after a reset link (FR-AUTH-3).
 */
@Composable
fun TwoverseNavHost(
    modifier: Modifier = Modifier,
    openHome: Boolean = false,
    isOffline: Boolean = false,
    isPasswordRecovery: Boolean = false,
    onPasswordRecoveryShown: () -> Unit = {},
) {
    val navController = rememberNavController()
    val currentOnPasswordRecoveryShown by rememberUpdatedState(onPasswordRecoveryShown)
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
            startDestination = if (openHome) HomeRoute else SplashRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            onboardingGraph(
                navController = navController,
                onConnected = { scope.launch { snackbarHostState.showSnackbar(connectedMessage) } },
            )
            tabsGraph(navController)
        }
    }
}

/** [onConnected] confirms pairing (FR-PAIR-6); the birthday welcome is its own confirmation. */
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
            onConnected = { showBirthday ->
                if (showBirthday) {
                    navController.navigateClearingBackStack(BirthdayRoute)
                } else {
                    navController.navigateClearingBackStack(HomeRoute)
                    onConnected()
                }
            },
        )
    }
    composable<BirthdayRoute> {
        BirthdayFeatureRoute(onEnter = { navController.navigateClearingBackStack(HomeRoute) })
    }
}

private fun NavGraphBuilder.tabsGraph(navController: NavHostController) {
    composable<HomeRoute> {
        HomeFeatureRoute(
            onOpenCompass = { navController.navigateToTab(TopLevelDestination.Compass) },
            onOpenCountdown = { navController.navigate(CountdownRoute) },
            onOpenVault = { navController.navigateToTab(TopLevelDestination.Vault) },
            onSendMemory = { navController.navigate(AddMemoryRoute) },
        )
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
            onShowBirthday = { navController.navigate(BirthdayRoute) },
        )
    }
    composable<CountdownRoute> {
        CountdownFeatureRoute(onBack = { navController.popBackStack() })
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

private fun SplashDestination.toRoute(): Any = when (this) {
    SplashDestination.Welcome -> WelcomeRoute
    SplashDestination.Pair -> PairRoute
    SplashDestination.Birthday -> BirthdayRoute
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
