package com.example.sitemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sitemanager.ui.screens.MainScreen
import com.example.sitemanager.ui.theme.SiteManagerTheme
import com.example.sitemanager.ui.viewmodel.SiteViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        setContent {
            SiteManagerTheme {
                val viewModel: SiteViewModel = viewModel()
                MainScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val url = intent.getStringExtra(Intent.EXTRA_TEXT)
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

            if (!url.isNullOrBlank()) {
                // استخراج URL من النص (قد يحتوي على نص إضافي)
                val extractedUrl = extractUrl(url)
                setContent {
                    SiteManagerTheme {
                        val viewModel: SiteViewModel = viewModel()
                        viewModel.onShareReceived(extractedUrl, subject)
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }

    private fun extractUrl(text: String): String {
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value ?: text.trim()
    }
}
