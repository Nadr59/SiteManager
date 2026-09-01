// app/src/main/java/com/nadr59/sitemanager/ui/screens/BrowserScreen.kt
// أضف هذه التعديلات

@Composable
fun BrowserScreen(
    siteId: Int,
    viewModel: BrowserViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingJs by viewModel.pendingJs.collectAsState()
    
    var webView by remember { mutableStateOf<WebView?>(null) }

    // ⭐ تسجيل WebView عند الإنشاء
    LaunchedEffect(webView) {
        webView?.let { wv ->
            viewModel.registerWebView(wv)
        }
    }

    // ⭐ إلغاء التسجيل عند الخروج
    DisposableEffect(Unit) {
        onDispose {
            viewModel.unregisterWebView()
        }
    }

    LaunchedEffect(siteId) {
        viewModel.loadSite(siteId)
    }

    Scaffold(
        topBar = {
            BrowserTopBar(
                title = uiState.title,
                url = uiState.url,
                isTranslationMode = uiState.isTranslationMode,
                onBack = onBack,
                onRefresh = { webView?.reload() },
                onShare = { /* Share */ },
                onTranslate = { viewModel.showTranslationSheet() },
                onReaderMode = { viewModel.enableReaderMode() },
                onScreenshot = {
                    webView?.let { wv ->
                        // Capture screenshot logic
                    }
                }
            )
        },
        bottomBar = {
            BrowserBottomBar(
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                onBack = { webView?.goBack() },
                onForward = { webView?.goForward() },
                onHome = { /* Navigate home */ },
                onBookmarks = { /* Show bookmarks */ },
                onHistory = { /* Show history */ }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                viewModel.updateLoadingState(true)
                                viewModel.updateUrl(url ?: "")
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                viewModel.updateLoadingState(false)
                                viewModel.updateTitle(view?.title ?: "")
                                viewModel.updateNavigationState(
                                    canGoBack = view?.canGoBack() == true,
                                    canGoForward = view?.canGoForward() == true
                                )
                                
                                // إضافة إلى التاريخ
                                url?.let { u ->
                                    viewModel.addToHistory(u, view?.title ?: "")
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                viewModel.updateProgress(newProgress)
                            }
                        }

                        webView = this // ⭐ حفظ المرجع
                        loadUrl(uiState.url)
                    }
                },
                update = { view ->
                    // تنفيذ JavaScript المعلق
                    pendingJs?.let { script ->
                        view.evaluateJavascript(script) { result ->
                            viewModel.consumePendingJs()
                        }
                    }
                }
            )

            // شريط التقدم
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    progress = uiState.progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            // شريط تقدم الترجمة
            if (uiState.isTranslating) {
                TranslationProgressBar(
                    progress = uiState.translationProgress,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Translation Bottom Sheet
            if (uiState.showTranslationSheet) {
                TranslationBottomSheet(
                    targetLanguage = uiState.targetLanguage,
                    onLanguageSelected = { lang ->
                        viewModel.setTargetLanguage(lang)
                    },
                    onTranslatePage = {
                        viewModel.startPageTranslationWithCoordinator() // ⭐ استخدام الطريقة الجديدة
                    },
                    onTranslateSelection = {
                        viewModel.translateSelectionWithCoordinator() // ⭐ استخدام الطريقة الجديدة
                    },
                    onDismiss = {
                        viewModel.hideTranslationSheet()
                    }
                )
            }
        }
    }
}
