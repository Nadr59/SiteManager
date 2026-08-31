package com.nadr59.sitemanager.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nadr59.sitemanager.viewmodel.BrowserViewModel

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    siteId: Int,
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingJs by viewModel.pendingJs.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val isReaderMode by viewModel.isReaderMode.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }

    val history by viewModel.browserHistory.collectAsState(initial = emptyList())
    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())

    LaunchedEffect(siteId) {
        if (siteId > 0) viewModel.loadSite(siteId)
    }

    // ═══ تنفيذ JavaScript ═══
    LaunchedEffect(pendingJs) {
        val script = pendingJs
        if (script != null && webView != null) {
            webView?.evaluateJavascript(script) { result ->
                viewModel.onJsExecuted()
                if (result != null && result != "null" && result.isNotBlank()) {
                    val cleanResult = result
                        .removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")

                    when {
                        cleanResult.trimStart().startsWith("[") -> {
                            viewModel.onNodesExtracted(result)
                        }
                        cleanResult.contains("\"text\"") -> {
                            viewModel.onTextSelected(result)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.title.ifBlank { "المتصفح" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.url,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    // ═══ وضع القراءة ═══
                    IconButton(onClick = { viewModel.toggleReaderMode() }) {
                        Icon(
                            Icons.Default.MenuBook,
                            "وضع القراءة",
                            tint = if (isReaderMode)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // ═══ إشارة مرجعية ═══
                    IconButton(onClick = { viewModel.toggleBookmark() }) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark
                            else Icons.Default.BookmarkBorder,
                            "إشارة مرجعية",
                            tint = if (isBookmarked)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // ═══ الترجمة ═══
                    IconButton(
                        onClick = {
                            if (uiState.isTranslationMode) {
                                viewModel.resetTranslation()
                            } else {
                                viewModel.showTranslationSheet()
                            }
                        }
                    ) {
                        Icon(
                            if (uiState.isTranslationMode) Icons.Default.Undo
                            else Icons.Default.Translate,
                            "ترجمة",
                            tint = if (uiState.isTranslationMode)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // ═══ المزيد ═══
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "المزيد")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("التاريخ") },
                                onClick = {
                                    showHistory = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("الإشارات المرجعية") },
                                onClick = {
                                    showBookmarks = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ترجمة النص المحدد") },
                                onClick = {
                                    viewModel.translateSelectedText()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("مسح التاريخ") },
                                onClick = {
                                    viewModel.clearAllHistory()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = false,
                    enabled = uiState.canGoBack,
                    onClick = { webView?.goBack() },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "رجوع",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
                NavigationBarItem(
                    selected = false,
                    enabled = uiState.canGoForward,
                    onClick = { webView?.goForward() },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            "تقدم",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { webView?.reload() },
                    icon = {
                        Icon(
                            Icons.Default.Refresh,
                            "تحديث",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { webView?.loadUrl(uiState.url) },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            "الرئيسية",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
            }
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
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                                defaultTextEncodingName = "UTF-8"
                                allowFileAccess = true
                                loadsImagesAutomatically = true
                            }

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
                                    viewModel.updateNavigation(canGoBack(), canGoForward())
                                    // ═══ حفظ في التاريخ ═══
                                    viewModel.saveToHistory()
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    viewModel.updateTitle(title ?: "")
                                }
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    viewModel.updateProgress(newProgress)
                                }
                            }

                            loadUrl(uiState.url)
                        }
                    },
                    update = { view -> webView = view }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            "جارٍ التحميل...",
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ═══ شريط التحميل ═══
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }

            // ═══ شريط الترجمة ═══
            AnimatedVisibility(
                visible = uiState.isTranslating,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            Text(
                                "جارٍ الترجمة...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${(uiState.translationProgress * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { uiState.translationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            // ═══ رسالة الخطأ ═══
            val error = uiState.error
            if (error != null) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("حسناً")
                        }
                    }
                ) {
                    Text(text = error)
                }
            }
        }
    }

    // ═══ ورقة الترجمة ═══
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

    // ═══ ورقة التاريخ ═══
    if (showHistory) {
        BrowserHistorySheet(
            history = history,
            onSelect = { item ->
                webView?.loadUrl(item.url)
                showHistory = false
            },
            onDelete = { item -> viewModel.deleteHistory(item.id) },
            onClearAll = { viewModel.clearAllHistory() },
            onDismiss = { showHistory = false }
        )
    }

    // ═══ ورقة الإشارات ═══
    if (showBookmarks) {
        BrowserBookmarksSheet(
            bookmarks = bookmarks,
            onSelect = { item ->
                webView?.loadUrl(item.url)
                showBookmarks = false
            },
            onDelete = { item -> viewModel.deleteBookmark(item.id) },
            onDismiss = { showBookmarks = false }
        )
    }
}
