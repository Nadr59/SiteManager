package com.nadr59.sitemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.nadr59.sitemanager.ui.theme.SiteManagerTheme
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
