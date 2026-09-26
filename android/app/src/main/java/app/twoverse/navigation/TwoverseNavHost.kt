package app.twoverse.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.designsystem.component.TwoverseBottomBar
import app.twoverse.core.designsystem.component.TwoverseBottomBarItem
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.feature.auth.SignInRoute as SignInFeatureRoute
import app.twoverse.feature.compass.CompassRoute as CompassFeatureRoute
import app.twoverse.feature.countdown.CountdownRoute as CountdownFeatureRoute
import app.twoverse.feature.home.HomeRoute as HomeFeatureRoute
import app.twoverse.feature.onboarding.WelcomeRoute as WelcomeFeatureRoute
import app.twoverse.feature.pairing.PairRoute as PairFeatureRoute
import app.twoverse.feature.splash.SplashDestination
import app.twoverse.feature.splash.SplashRoute as SplashFeatureRoute
import kotlinx.coroutines.launch

@Composable
fun TwoverseNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
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
            startDestination = SplashRoute,
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

private fun NavGraphBuilder.onboardingGraph(navController: NavHostController, onConnected: () -> Unit) {
    composable<SplashRoute> {
        SplashFeatureRoute(
            onNavigate = { destination -> navController.navigateClearingBackStack(destination.toRoute()) },
        )
    }
    composable<WelcomeRoute> {
        WelcomeFeatureRoute(
            onGetStarted = { navController.navigate(SignInRoute) },
            onHaveCoupleCode = { navController.navigate(PairRoute) },
        )
    }
    composable<SignInRoute> {
        SignInFeatureRoute(
            onSignedIn = { navController.navigate(PairRoute) { launchSingleTop = true } },
            onCreateAccount = { navController.navigate(PairRoute) { launchSingleTop = true } },
        )
    }
    composable<PairRoute> {
        PairFeatureRoute(
            onBack = { navController.popBackStack() },
            onConnected = {
                navController.navigateClearingBackStack(HomeRoute)
                onConnected()
            },
        )
    }
    composable<BirthdayRoute> {
        PlaceholderScreen(
            titleRes = R.string.birthday_title,
            actions = listOf(
                PlaceholderAction(R.string.birthday_enter) { navController.navigateClearingBackStack(HomeRoute) },
            ),
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
        )
    }
    composable<CompassRoute> {
        CompassFeatureRoute()
    }
    composable<VaultRoute> {
        PlaceholderScreen(
            titleRes = R.string.vault_title,
            actions = listOf(
                PlaceholderAction(R.string.placeholder_open_memory) {
                    navController.navigate(MemoryRoute(memoryId = SampleData.SUNSET_MEMORY_ID))
                },
                PlaceholderAction(R.string.vault_add_memory) { navController.navigate(AddMemoryRoute) },
            ),
        )
    }
    composable<SettingsRoute> {
        PlaceholderScreen(titleRes = R.string.settings_title, actions = emptyList())
    }
    composable<CountdownRoute> {
        CountdownFeatureRoute(onBack = { navController.popBackStack() })
    }
    composable<MemoryRoute> {
        PlaceholderScreen(
            titleRes = R.string.vault_memory_title,
            actions = listOf(PlaceholderAction(R.string.common_back) { navController.popBackStack() }),
        )
    }
    composable<AddMemoryRoute> {
        PlaceholderScreen(
            titleRes = R.string.add_memory_title,
            actions = listOf(
                PlaceholderAction(R.string.add_memory_send) {
                    navController.popBackStack()
                    navController.navigateToTab(TopLevelDestination.Vault)
                },
                PlaceholderAction(R.string.common_close) { navController.popBackStack() },
            ),
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
