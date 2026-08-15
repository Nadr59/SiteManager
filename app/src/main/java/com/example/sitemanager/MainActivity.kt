package com.nadr59.sitemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nadr59.sitemanager.ui.screens.*
import com.nadr59.sitemanager.ui.theme.SiteManagerTheme
import dagger.hilt.android.AndroidEntryPoint

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

                    NavHost(navController = navController, startDestination = "home") {

                        // الشاشة الرئيسية
                        composable("home") {
                            HomeScreen(
                                viewModel = hiltViewModel(),
                                onNavigateToAdd = { navController.navigate("add") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                // ═══ جديد: التنقل لشاشة التحليل ═══
                                onNavigateToAnalysis = { siteId, name, url ->
                                    navController.navigate("analysis/$siteId/${java.net.URLEncoder.encode(name, "UTF-8")}/${java.net.URLEncoder.encode(url, "UTF-8")}")
                                }
                            )
                        }

                        // شاشة إضافة موقع
                        composable("add") {
                            AddSiteScreen(
                                viewModel = hiltViewModel(),
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // شاشة الإعدادات
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ═══ جديد: شاشة التحليل ═══
                        composable(
                            route = "analysis/{siteId}/{siteName}/{siteUrl}",
                            arguments = listOf(
                                navArgument("siteId") { type = NavType.IntType },
                                navArgument("siteName") { type = NavType.StringType },
                                navArgument("siteUrl") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val siteId = backStackEntry.arguments?.getInt("siteId") ?: 0
                            val siteName = java.net.URLDecoder.decode(
                                backStackEntry.arguments?.getString("siteName") ?: "", "UTF-8"
                            )
                            val siteUrl = java.net.URLDecoder.decode(
                                backStackEntry.arguments?.getString("siteUrl") ?: "", "UTF-8"
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
