package com.novacraft.launcher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.novacraft.launcher.ui.screens.accounts.AccountsScreen
import com.novacraft.launcher.ui.screens.files.FilesScreen
import com.novacraft.launcher.ui.screens.home.HomeScreen
import com.novacraft.launcher.ui.screens.mods.ModsScreen
import com.novacraft.launcher.ui.screens.play.PlayScreen
import com.novacraft.launcher.ui.screens.settings.SettingsScreen
import com.novacraft.launcher.ui.screens.versions.VersionsScreen

// ─── Route constants ──────────────────────────────────────────────────────────

object Routes {
    const val HOME      = "home"
    const val PLAY      = "play"
    const val VERSIONS  = "versions"
    const val MODS      = "mods"
    const val FILES     = "files"
    const val ACCOUNTS  = "accounts"
    const val SETTINGS  = "settings"
}

// ─── Nav items for bottom bar ─────────────────────────────────────────────────

data class NavItem(
    val route: String,
    val label: String,
    val iconRes: Int,        // vector drawable resource id
    val activeIconRes: Int
)

// ─── Root nav host ────────────────────────────────────────────────────────────

@Composable
fun NovaCraftNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(Routes.PLAY) {
            PlayScreen(navController = navController)
        }
        composable(Routes.VERSIONS) {
            VersionsScreen(navController = navController)
        }
        composable(Routes.MODS) {
            ModsScreen(navController = navController)
        }
        composable(Routes.FILES) {
            FilesScreen(navController = navController)
        }
        composable(Routes.ACCOUNTS) {
            AccountsScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
    }
}
