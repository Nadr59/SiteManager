package com.example.sitemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sitemanager.ui.screens.MainScreen
import com.example.sitemanager.ui.theme.SiteManagerTheme
import com.example.sitemanager.ui.viewmodel.SiteViewModel

class MainActivity : ComponentActivity() {

    // ═══ نحفظ بيانات المشاركة هنا ═══
    private var sharedUrl: String? by mutableStateOf(null)
    private var sharedTitle: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ═══ قراءة المشاركة من intent ═══
        extractShareData(intent)

        setContent {
            SiteManagerTheme {
                val viewModel: SiteViewModel = viewModel()

                // ═══ عند وجود بيانات مشاركة: نمررها للـ ViewModel ═══
                LaunchedEffect(sharedUrl) {
                    if (sharedUrl != null) {
                        viewModel.onShareReceived(sharedUrl, sharedTitle)
                        sharedUrl = null
                        sharedTitle = null
                    }
                }

                MainScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // ═══ تحديث الـ intent ثم قراءة المشاركة ═══
        setIntent(intent)
        extractShareData(intent)
    }

    private fun extractShareData(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

            if (!text.isNullOrBlank()) {
                sharedUrl = extractUrl(text)
                sharedTitle = subject ?: extractDomain(sharedUrl!!)
            }
        }
    }

    private fun extractUrl(text: String): String {
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value?.trim() ?: text.trim()
    }

    private fun extractDomain(url: String): String {
        return try {
            val host = java.net.URI(url).host ?: url
            host.removePrefix("www.")
        } catch (_: Exception) {
            url.take(30)
        }
    }
}
