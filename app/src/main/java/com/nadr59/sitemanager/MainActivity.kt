package com.nadr59.sitemanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nadr59.sitemanager.ui.screens.*
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
        // ═══ الرئيسية ═══
        composable("home") {
            HomeScreen(
                sharedUrl = sharedUrl,
                onSharedUrlConsumed = { sharedUrl = "" },
                onNavigateToAdd = { navController.navigate("add_site") },
                onNavigateToAddWithUrl = { url ->
                    navController.navigate("add_site?url=${Uri.encode(url)}")
                },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToExport = { navController.navigate("export") },
                onNavigateToAnalysis = { navController.navigate("analysis") },
                onNavigateToEdit = { siteId, name, url ->
                    navController.navigate("edit_site/$siteId")
                },
                onNavigateToDetail = { siteId ->
                    navController.navigate("site_detail/$siteId")
                },
                onNavigateToDashboard = { siteId ->
                    navController.navigate("dashboard/$siteId")
                }
            )
        }

        // ═══ إضافة موقع ═══
        composable("add_site") {
            AddSiteScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ═══ تفاصيل الموقع ═══
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

        // ═══ المتصفح الداخلي ═══
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
