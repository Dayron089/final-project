package com.example.rickandmorty.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rickandmorty.ui.screens.detail.PokemonDetailScreen
import com.example.rickandmorty.ui.screens.favorites.FavoritesScreen
import com.example.rickandmorty.ui.screens.history.HistoryScreen
import com.example.rickandmorty.ui.screens.list.PokemonListScreen
import com.example.rickandmorty.ui.screens.profiles.ProfilesScreen
import com.example.rickandmorty.ui.screens.settings.SettingsScreen
import com.example.rickandmorty.ui.screens.tags.TagsScreen
import com.example.rickandmorty.ui.screens.teams.TeamDetailScreen
import com.example.rickandmorty.ui.screens.teams.TeamPokemonPickerScreen
import com.example.rickandmorty.ui.screens.teams.TeamsScreen
import com.example.rickandmorty.ui.viewmodel.ProfilesViewModel

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val profilesViewModel: ProfilesViewModel = hiltViewModel()
    val profilesState by profilesViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = if (profilesState.activeProfileId == -1L) Routes.PROFILES else Routes.HOME_LIST

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in BottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.PROFILES) {
                ProfilesScreen(
                    onProfileActivated = {
                        navController.navigate(Routes.HOME_LIST) {
                            popUpTo(Routes.PROFILES) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME_LIST) {
                PokemonListScreen(
                    onPokemonClick = { id -> navController.navigate(Routes.detail(id)) },
                    onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) }
                )
            }
            composable(Routes.HOME_TEAMS) {
                TeamsScreen(
                    onTeamClick = { teamId -> navController.navigate(Routes.teamDetail(teamId)) }
                )
            }
            composable(Routes.HOME_TAGS) { TagsScreen() }
            composable(Routes.HOME_SETTINGS) {
                SettingsScreen(
                    onSwitchProfile = {
                        navController.navigate(Routes.PROFILES) {
                            popUpTo(Routes.HOME_LIST) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("pokemonId") { type = NavType.IntType })
            ) {
                PokemonDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.TEAM_DETAIL,
                arguments = listOf(navArgument("teamId") { type = NavType.LongType })
            ) { entry ->
                val teamId = entry.arguments?.getLong("teamId") ?: 0L
                TeamDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPick = { navController.navigate(Routes.teamPicker(teamId)) }
                )
            }
            composable(
                route = Routes.TEAM_PICKER,
                arguments = listOf(navArgument("teamId") { type = NavType.LongType })
            ) {
                TeamPokemonPickerScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onPokemonClick = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onPokemonClick = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
        }
    }
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val BottomNavItems = listOf(
    BottomItem(Routes.HOME_LIST, "Pokedex", Icons.Filled.Pets),
    BottomItem(Routes.HOME_TEAMS, "Teams", Icons.Filled.Groups),
    BottomItem(Routes.HOME_TAGS, "Tags", Icons.Filled.Label),
    BottomItem(Routes.HOME_SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
private fun BottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar {
        BottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { if (currentRoute != item.route) onSelect(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
