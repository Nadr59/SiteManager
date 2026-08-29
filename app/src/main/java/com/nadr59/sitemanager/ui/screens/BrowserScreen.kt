package com.nadr59.sitemanager.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose" to "فارسی",
        "id" to "Indonesia",
        "ms" to "Melayu",
        "zh" to "中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "hi" to "हिन्दी",
        "ru" to "Русский"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "ترجمة الصفحة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "اللغة المستهدفة:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // ═══ شبكة اللغات ═══
            val rows = languages.chunked(3)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (code, name) ->
                        val selected = currentLanguage == code
                        FilterChip(
                            selected = selected,
                            onClick = { onLanguageSelected(code) },
                            label = {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
.material3.*
import androidx.compose.runtime.*
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
        if (initialUrl.isNotBlank()) {
            viewModel.loadUrl(initialUrl)
        } else {
            viewModel.loadSite(siteId)
        }
    }

    // ═══ تنفيذ JavaScript المعلق ═══
    LaunchedEffect(pendingJs) {
        val script = pendingJs
        if (script != null && webView != null) {
            webView?.evaluateJavascript(script) { result ->
                viewModel.onJsExecuted()

                // ═══ معالجة النتائج ═══
                if (result != null && result != "null" && result.isNotBlank()) {
                    when {
                        // نتيجة استخراج العقد
                        result.startsWith("\"[{") || result.startsWith("[{") -> {
                            viewModel.onNodesExtracted(result)
                        }
                        // نتيجة تحديد النص
                        result.startsWith("\"{\\\"text") || result.startsWith("{\"text") -> {
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
                .Host:

```kotlin
           fillMaxSize()
                .padding(padding)
        ) {
            // ═══ WebView ═══
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
                                    view: WebView?, url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    viewModel.setLoading(true)
                                }

                                override fun onPageFinished(
                                    view: WebView?, url: String?
                                ) {
                                    viewModel.setLoading(false)
                                    viewModel.updateNavigation(
                                        canGoBack(), canGoForward()
                                    )
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(
                                    view: WebView?, title: String?
                                ) {
                                    viewModel.updateTitle(title ?: "")
                                }

                                override fun onProgressChanged(
                                    view: WebView?, new // ═══ المتصفح مع الترجمة ═══
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
Progress: Int
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
            }

            // ═══ شريط التحميل ═══
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                )
            }

            // ═══ شريط تقدم الترجمة ═══
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
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "جاري ترجمة الصفحة...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${(uiState.translationProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ═══ رسالة الخطأ ═══
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("إغلاق")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }

    // ═══ قائمة الترجمة السفلية ═══
    if (uiState.showTranslationSheet) {
        TranslationBottomSheet(
            currentLanguage = uiState.targetLanguage,
            onLanguageSelected = { viewModel.setTargetLanguage(it) },
            onTranslatePage = { viewModel.startPageTranslation() },
            onTranslateSelection = {
                viewModel.hideTranslationSheet()
                viewModel.translateSelectedText()
            },
            onDismiss = { viewModel.hideTranslationSheet() }
        )
    }
                  }
