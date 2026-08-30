package com.nadr59.sitemanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nadr59.sitemanager.ui.screens.AddSiteScreen
import com.nadr59.sitemanager.ui.screens.BrowserScreen
import com.nadr59.sitemanager.ui.screens.HomeScreen
import com.nadr59.sitemanager.ui.screens.SettingsScreen
import com.nadr59.sitemanager.ui.screens.SiteDetailScreen
import com.nadr59.sitemanager.ui.theme.SiteManagerTheme
import com.nadr59.sitemanager.viewmodel.BrowserViewModel
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var sharedUrl = ""
        if (intent?.action == Intent.ACTION_SEND) {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        }

        setContent {
            SiteManagerTheme {
                MainNavHost(initialSharedUrl = sharedUrl)
            }
        }
    }
}

@Composable
fun MainNavHost(initialSharedUrl: String = "") {
    val navController = rememberNavController()
    val vm: SiteViewModel = hiltViewModel()

    var sharedUrl by remember { mutableStateOf(initialSharedUrl) }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        composable("home") {
            HomeScreen(
                viewModel = vm,
                sharedUrl = sharedUrl,
                onSharedUrlConsumed = { sharedUrl = "" },
                onNavigateToAdd = {
                    navController.navigate("add_site")
                },
                onNavigateToAddWithUrl = { url: String ->
                    navController.navigate("add_site?url=${Uri.encode(url)}")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToExport = {
                    navController.navigate("export")
                },
                onNavigateToAnalysis = { siteId: Int, name: String, url: String ->
                    navController.navigate("analysis/$siteId")
                },
                onNavigateToEdit = { siteId: Int ->
                    navController.navigate("edit_site/$siteId")
                },
                onNavigateToDetail = { siteId: Int ->
                    navController.navigate("site_detail/$siteId")
                },
                onNavigateToDashboard = {
                    navController.navigate("dashboard")
                }
            )
        }

        composable(
            route = "add_site?url={url}",
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val prefilledUrl = backStackEntry.arguments?.getString("url")
            val addSiteVm: SiteViewModel = hiltViewModel()

            AddSiteScreen(
                viewModel = addSiteVm,
                initialUrl = prefilledUrl,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══ الإعدادات ═══
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "site_detail/{siteId}",
            arguments = listOf(
                navArgument("siteId") { type = NavType.IntType }
            )
        ) { entry ->
            val siteId = entry.arguments?.getInt("siteId") ?: 0
            val detailVm: SiteViewModel = hiltViewModel()

            SiteDetailScreen(
                siteId = siteId,
                viewModel = detailVm,
                onBack = { navController.popBackStack() },
                onOpenBrowser = { id ->
                    navController.navigate("browser/$id")
                }
            )
        }

        composable(
            route = "browser/{siteId}",
            arguments = listOf(
                navArgument("siteId") { type = NavType.IntType }
            )
        ) { entry ->
            val siteId = entry.arguments?.getInt("siteId") ?: 0
            val browserVm: BrowserViewModel = hiltViewModel()

            BrowserScreen(
                siteId = siteId,
                viewModel = browserVm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
