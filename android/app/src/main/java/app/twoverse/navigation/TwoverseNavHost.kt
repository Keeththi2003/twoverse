package app.twoverse.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun TwoverseNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selectedTab = TopLevelDestination.entries.indexOfFirst { tab ->
        destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    Scaffold(
        modifier = modifier,
        containerColor = TwoverseTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
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
            onboardingGraph(navController)
            tabsGraph(navController)
        }
    }
}

private fun NavGraphBuilder.onboardingGraph(navController: NavHostController) {
    composable<SplashRoute> {
        PlaceholderScreen(
            titleRes = R.string.app_name,
            actions = listOf(
                PlaceholderAction(R.string.placeholder_continue) {
                    navController.navigate(WelcomeRoute) { popUpTo<SplashRoute> { inclusive = true } }
                },
            ),
        )
    }
    composable<WelcomeRoute> {
        PlaceholderScreen(
            titleRes = R.string.welcome_title,
            actions = listOf(
                PlaceholderAction(R.string.welcome_get_started) { navController.navigate(SignInRoute) },
                PlaceholderAction(R.string.welcome_have_code) { navController.navigate(PairRoute) },
            ),
        )
    }
    composable<SignInRoute> {
        PlaceholderScreen(
            titleRes = R.string.sign_in_title,
            actions = listOf(
                PlaceholderAction(R.string.sign_in_button) { navController.navigate(PairRoute) },
            ),
        )
    }
    composable<PairRoute> {
        PlaceholderScreen(
            titleRes = R.string.pair_title,
            actions = listOf(
                PlaceholderAction(R.string.pair_connect) { navController.navigateClearingBackStack(HomeRoute) },
                PlaceholderAction(R.string.placeholder_connect_birthday) {
                    navController.navigateClearingBackStack(BirthdayRoute)
                },
            ),
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
        PlaceholderScreen(
            titleRes = R.string.home_title,
            actions = listOf(
                PlaceholderAction(R.string.compass_title) { navController.navigateToTab(TopLevelDestination.Compass) },
                PlaceholderAction(R.string.countdown_title) { navController.navigate(CountdownRoute) },
                PlaceholderAction(R.string.vault_title) { navController.navigateToTab(TopLevelDestination.Vault) },
                PlaceholderAction(R.string.home_send_memory) { navController.navigate(AddMemoryRoute) },
            ),
        )
    }
    composable<CompassRoute> {
        PlaceholderScreen(titleRes = R.string.compass_title, actions = emptyList())
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
        PlaceholderScreen(
            titleRes = R.string.countdown_title,
            actions = listOf(PlaceholderAction(R.string.common_back) { navController.popBackStack() }),
        )
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
