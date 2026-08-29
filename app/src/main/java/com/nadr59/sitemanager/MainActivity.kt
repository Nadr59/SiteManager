package com.nadr59.sitemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        setContent {
            SiteManagerTheme {
                MainNavHost()
            }
        }
    }
}

@Composable
fun MainNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ═══ الرئيسية ═══
        composable("home") {
            val vm: SiteViewModel = hiltViewModel()
            HomeScreen(
                navController = navController,
                viewModel = vm
            )
        }

        // ═══ إضافة موقع ═══
        composable("add_site") {
            val vm: SiteViewModel = hiltViewModel()
            AddSiteScreen(
                navController = navController,
                viewModel = vm
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
            val vm: SiteViewModel = hiltViewModel()
            SiteDetailScreen(
                siteId = siteId,
                navController = navController,
                viewModel = vm
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
            val vm: BrowserViewModel = hiltViewModel()

            BrowserScreen(
                siteId = siteId,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
