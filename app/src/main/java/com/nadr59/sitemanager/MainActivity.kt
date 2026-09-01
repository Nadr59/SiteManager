package com.nadr59.sitemanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.nadr59.sitemanager.ui.screens.AnalysisScreen
import com.nadr59.sitemanager.ui.screens.BrowserScreen
import com.nadr59.sitemanager.ui.screens.DashboardScreen
import com.nadr59.sitemanager.ui.screens.EditSiteScreen
import com.nadr59.sitemanager.ui.screens.ExportImportScreen
import com.nadr59.sitemanager.ui.screens.HomeScreen
import com.nadr59.sitemanager.ui.screens.SettingsScreen
import com.nadr59.sitemanager.ui.screens.SiteDetailScreen
import com.nadr59.sitemanager.ui.theme.SiteManagerTheme
import com.nadr59.sitemanager.viewmodel.BrowserViewModel
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import com.nadr59.sitemanager.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedUrl = extractSharedUrl(intent)

        setContent {
            val themeVm: ThemeViewModel = hiltViewModel()
            val currentTheme by themeVm.currentTheme.collectAsState()
            val isDarkMode by themeVm.isDarkMode.collectAsState()

            SiteManagerTheme(
                themePreference = currentTheme,
                darkTheme = isDarkMode
            ) {
                MainNavHost(initialSharedUrl = sharedUrl)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sharedUrl = extractSharedUrl(intent)
        if (sharedUrl.isNotBlank()) {
            setContent {
                val themeVm: ThemeViewModel = hiltViewModel()
                val currentTheme by themeVm.currentTheme.collectAsState()
                val isDarkMode by themeVm.isDarkMode.collectAsState()

                SiteManagerTheme(
                    themePreference = currentTheme,
                    darkTheme = isDarkMode
                ) {
                    MainNavHost(initialSharedUrl = sharedUrl)
                }
            }
        }
    }

    private fun extractSharedUrl(intent: Intent?): String {
        if (intent == null) return ""
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    extractUrlFromText(text)
                } else ""
            }
            Intent.ACTION_VIEW -> intent.dataString ?: ""
            else -> ""
        }
    }

    private fun extractUrlFromText(text: String): String {
        if (text.isBlank()) return ""
        if (text.startsWith("http://") || text.startsWith("https://")) return text.trim()
        val urlRegex = Regex("""https?://[^\s]+""")
        val match = urlRegex.find(text)
        if (match != null) return match.value.trim()
        if (text.contains(".") && !text.contains(" ")) return "https://${text.trim()}"
        return text.trim()
    }
}

@Composable
fun MainNavHost(initialSharedUrl: String = "") {
    val navController = rememberNavController()
    val vm: SiteViewModel = hiltViewModel()
    var sharedUrl by remember { mutableStateOf(initialSharedUrl) }

    NavHost(navController = navController, startDestination = "home") {

        // ═══ الرئيسية ═══
        composable("home") {
            HomeScreen(
                viewModel = vm,
                sharedUrl = sharedUrl,
                onSharedUrlConsumed = { sharedUrl = "" },
                onNavigateToAdd = { navController.navigate("add_site") },
                onNavigateToAddWithUrl = { url ->
                    navController.navigate("add_site?url=${Uri.encode(url)}")
                },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToExport = { navController.navigate("export") },
                onNavigateToAnalysis = { siteId, _, _ ->
                    navController.navigate("analysis/$siteId")
                },
                onNavigateToEdit = { siteId ->
                    navController.navigate("edit_site/$siteId")
                },
                onNavigateToDetail = { siteId ->
                    navController.navigate("site_detail/$siteId")
                },
                onNavigateToDashboard = { navController.navigate("dashboard") }
            )
        }

        // ═══ إضافة موقع ═══
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
            AddSiteScreen(
                viewModel = hiltViewModel(),
                initialUrl = prefilledUrl,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══ تعديل موقع ═══
        composable(
            route = "edit_site/{siteId}",
            arguments = listOf(
                navArgument("siteId") { type = NavType.IntType }
            )
        ) { entry ->
            val siteId = entry.arguments?.getInt("siteId") ?: 0
            EditSiteScreen(
                viewModel = hiltViewModel(),
                siteId = siteId,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══ الإعدادات ═══
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        // ═══ تصدير واستيراد ═══
        composable("export") {
            ExportImportScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══ لوحة التحكم ═══
        composable("dashboard") {
            DashboardScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══ تحليل الموقع ═══
        composable(
            route = "analysis/{siteId}",
            arguments = listOf(
                navArgument("siteId") { type = NavType.IntType }
            )
        ) { entry ->
            val siteId = entry.arguments?.getInt("siteId") ?: 0
            AnalysisScreen(
                siteId = siteId,
                viewModel = hiltViewModel(),
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
            SiteDetailScreen(
                siteId = siteId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onOpenBrowser = { id -> navController.navigate("browser/$id") }
            )
        }

        // ═══ المتصفح ═══
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

