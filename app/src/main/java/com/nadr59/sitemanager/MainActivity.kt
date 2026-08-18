package com.nadr59.sitemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nadr59.sitemanager.ui.screens.AddSiteScreen
import com.nadr59.sitemanager.ui.screens.AnalysisScreen
import com.nadr59.sitemanager.ui.screens.DashboardScreen
import com.nadr59.sitemanager.ui.screens.EditSiteScreen
import com.nadr59.sitemanager.ui.screens.ExportImportScreen
import com.nadr59.sitemanager.ui.screens.HomeScreen
import com.nadr59.sitemanager.ui.screens.SettingsScreen
import com.nadr59.sitemanager.ui.screens.SiteDetailScreen
import com.nadr59.sitemanager.ui.theme.SiteManagerTheme
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

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

                        // ═══ الرئيسية ═══
                        composable("home") {
                            val viewModel: SiteViewModel = hiltViewModel()
                            HomeScreen(
                                viewModel = viewModel,
                                sharedUrl = sharedUrl,
                                onSharedUrlConsumed = { sharedUrl = null },
                                onNavigateToAdd = {
                                    navController.navigate("add")
                                },
                                onNavigateToAddWithUrl = { url ->
                                    val encoded = URLEncoder.encode(url, "UTF-8")
                                    navController.navigate("add?url=$encoded")
                                    sharedUrl = null
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToExport = {
                                    navController.navigate("export")
                                },
                                onNavigateToAnalysis = { siteId, name, url ->
                                    val encName = URLEncoder.encode(name, "UTF-8")
                                    val encUrl = URLEncoder.encode(url, "UTF-8")
                                    navController.navigate(
                                        "analysis/$siteId/$encName/$encUrl/explain"
                                    )
                                },
                                onNavigateToEdit = { siteId ->
                                    navController.navigate("edit/$siteId")
                                },
                                onNavigateToDetail = { siteId ->
                                    navController.navigate("detail/$siteId")
                                },
                                onNavigateToDashboard = {
                                    navController.navigate("dashboard")
                                }
                            )
                        }

                        // ═══ إضافة موقع (بدون رابط) ═══
                        composable("add") {
                            val vm: SiteViewModel = hiltViewModel()
                            AddSiteScreen(
                                viewModel = vm,
                                initialUrl = null,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ═══ إضافة موقع (مع رابط مُشارك) ═══
                        composable(
                            route = "add?url={sharedUrl}",
                            arguments = listOf(
                                navArgument("sharedUrl") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { entry ->
                            val url = URLDecoder.decode(
                                entry.arguments?.getString("sharedUrl") ?: "",
                                "UTF-8"
                            )
                            val vm: SiteViewModel = hiltViewModel()
                            AddSiteScreen(
                                viewModel = vm,
                                initialUrl = url.ifBlank { null },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ═══ تعديل موقع ═══
                        composable(
                            route = "edit/{siteId}",
                            arguments = listOf(
                                navArgument("siteId") {
                                    type = NavType.IntType
                                }
                            )
                        ) { entry ->
                            val siteId = entry.arguments?.getInt("siteId") ?: 0
                            val vm: SiteViewModel = hiltViewModel()
                            EditSiteScreen(
                                viewModel = vm,
                                siteId = siteId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ═══ تصدير واستيراد ═══
                        composable("export") {
                            val vm: SiteViewModel = hiltViewModel()
                            ExportImportScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ═══ تفاصيل الموقع ═══
                        composable(
                            route = "detail/{siteId}",
                            arguments = listOf(
                                navArgument("siteId") {
                                    type = NavType.IntType
                                }
                            )
                        ) { entry ->
                            val siteId = entry.arguments?.getInt("siteId") ?: 0
                            val vm: SiteViewModel = hiltViewModel()
                            SiteDetailScreen(
                                siteId = siteId,
                                viewModel = vm,
                                onBack = { navController.popBackStack() },
                                onNavigateToEdit = { id ->
                                    navController.navigate("edit/$id")
                                },
                                onNavigateToAnalysis = { id, name, url, type ->
                                    val encName = URLEncoder.encode(name, "UTF-8")
                                    val encUrl = URLEncoder.encode(url, "UTF-8")
                                    navController.navigate(
                                        "analysis/$id/$encName/$encUrl/$type"
                                    )
                                }
                            )
                        }

                        // ═══ الإعدادات ═══
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ═══ لوحة المعلومات ═══
                        composable("dashboard") {
                            val vm: SiteViewModel = hiltViewModel()
                            DashboardScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ═══ التحليل (مع نوع التحليل) ═══
                        composable(
                            route = "analysis/{siteId}/{siteName}/{siteUrl}/{analysisType}",
                            arguments = listOf(
                                navArgument("siteId") {
                                    type = NavType.IntType
                                },
                                navArgument("siteName") {
                                    type = NavType.StringType
                                },
                                navArgument("siteUrl") {
                                    type = NavType.StringType
                                },
                                navArgument("analysisType") {
                                    type = NavType.StringType
                                }
                            )
                        ) { entry ->
                            val siteId = entry.arguments?.getInt("siteId") ?: 0
                            val siteName = URLDecoder.decode(
                                entry.arguments?.getString("siteName") ?: "",
                                "UTF-8"
                            )
                            val siteUrl = URLDecoder.decode(
                                entry.arguments?.getString("siteUrl") ?: "",
                                "UTF-8"
                            )
                            val analysisType =
                                entry.arguments?.getString("analysisType") ?: "explain"
                            val vm: SiteViewModel = hiltViewModel()
                            AnalysisScreen(
                                viewModel = vm,
                                siteId = siteId,
                                siteName = siteName,
                                siteUrl = siteUrl,
                                analysisTypeKey = analysisType,
                                onBack = { navController.popBackStack() }
                            )
                        }

                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                }
            }
            Intent.ACTION_VIEW -> {
                sharedUrl = intent.data?.toString()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
}
