package com.nadr59.sitemanager.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.nadr59.sitemanager.viewmodel.BrowserViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    siteId: Int,
    initialUrl: String = "",
    viewModel: BrowserViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingJs by viewModel.pendingJs.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(siteId) {
        if (siteId > 0) {
            viewModel.loadSite(siteId)
        } else if (initialUrl.isNotBlank()) {
            viewModel.loadUrl(initialUrl)
        }
    }

    LaunchedEffect(pendingJs) {
        val script = pendingJs
        if (script != null && webView != null) {
            webView?.evaluateJavascript(script) { result ->
                viewModel.onJsExecuted()
                if (result != null && result != "null" && result.isNotBlank()) {
                    when {
                        result.startsWith("\"[{") || result.startsWith("[{") -> {
                            viewModel.onNodesExtracted(result)
                        }
                        result.contains("\"text\"") -> {
                            viewModel.onTextSelected(result)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            BrowserTopBar(
                title = uiState.title,
                isLoading = uiState.isLoading,
                isTranslationMode = uiState.isTranslationMode,
                onBack = onBack,
                onTranslate = { viewModel.showTranslationSheet() },
                onTranslateSelection = { viewModel.translateSelectedText() },
                onResetTranslation = { viewModel.resetTranslation() },
                onReload = { webView?.reload() }
            )
        },
        bottomBar = {
            BrowserBottomBar(
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                url = uiState.url,
                onBack = { webView?.goBack() },
                onForward = { webView?.goForward() },
                onReload = { webView?.reload() },
                onHome = { webView?.loadUrl(uiState.url) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.url.isNotBlank()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.setSupportZoom(true)
                            settings.defaultTextEncodingName = "UTF-8"

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    viewModel.setLoading(true)
                                }

                                override fun onPageFinished(
                                    view: WebView?,
                                    url: String?
                                ) {
                                    viewModel.setLoading(false)
                                    viewModel.updateNavigation(
                                        canGoBack(),
                                        canGoForward()
                                    )
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(
                                    view: WebView?,
                                    title: String?
                                ) {
                                    viewModel.updateTitle(title ?: "")
                                }

                                override fun onProgressChanged(
                                    view: WebView?,
                                    newProgress: Int
                                ) {
                                    viewModel.updateProgress(newProgress)
                                }
                            }

                            loadUrl(uiState.url)
                        }
                    },
                    update = { view ->
                        webView = view
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "loading...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                )
            }

            if (uiState.isTranslating) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    TranslationProgressBar(uiState.translationProgress)
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Translating...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${(uiState.translationProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(text = error)
                }
            }
        }
    }

    if (uiState.showTranslationSheet) {
        TranslationBottomSheet(
            currentLanguage = uiState.targetLanguage,
            onLanguageSelected = { lang -> viewModel.setTargetLanguage(lang) },
            onTranslatePage = { viewModel.startPageTranslation() },
            onTranslateSelection = {
                viewModel.hideTranslationSheet()
                viewModel.translateSelectedText()
            },
            onDismiss = { viewModel.hideTranslationSheet() }
        )
    }
}
