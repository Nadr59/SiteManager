package com.nadr59.sitemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nadr59.sitemanager.ui.screens.AddSiteScreen
import com.nadr59.sitemanager.ui.screens.AnalysisScreen
import com.nadr59.sitemanager.ui.screens.HomeScreen
import com.nadr59.sitemanager.ui.screens.SettingsScreen
import com.nadr59.sitemanager.ui.theme.SiteManagerTheme
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiteManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {

                        // ═══ الشاشة الرئيسية ═══
                        composable("home") {
                            val viewModel: SiteViewModel = hiltViewModel()
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAdd = {
                                    navController.navigate("add")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToAnalysis = { siteId, name, url ->
                                    val encodedName = URLEncoder.encode(name, "UTF-8")
                                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                                    navController.navigate(
                                        "analysis/$siteId/$encodedName/$encodedUrl"
                                    )
                                }
                            )
                        }

                        // ═══ إضافة موقع ═══
                        composable("add") {
                            val viewModel: SiteViewModel = hiltViewModel()
                            AddSiteScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ═══ الإعدادات ═══
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ═══ شاشة التحليل ═══
                        composable(
                            route = "analysis/{siteId}/{siteName}/{siteUrl}",
                            arguments = listOf(
                                navArgument("siteId") { type = NavType.IntType },
                                navArgument("siteName") { type = NavType.StringType },
                                navArgument("siteUrl") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val siteId = backStackEntry.arguments?.getInt("siteId") ?: 0
                            val siteName = URLDecoder.decode(
                                backStackEntry.arguments?.getString("siteName") ?: "",
                                "UTF-8"
                            )
                            val siteUrl = URLDecoder.decode(
                                backStackEntry.arguments?.getString("siteUrl") ?: "",
                                "UTF-8"
                            )
                            AnalysisScreen(
                                siteId = siteId,
                                siteName = siteName,
                                siteUrl = siteUrl,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
