package com.nadr59.sitemanager.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.nadr59.sitemanager.data.local.PageNote
import com.nadr59.sitemanager.viewmodel.AiMessage
import com.nadr59.sitemanager.viewmodel.BrowserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    siteId: Int,
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pendingJs by viewModel.pendingJs.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val isReaderMode by viewModel.isReaderMode.collectAsState()
    val pageSummary by viewModel.pageSummary.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()
    val aiMessages by viewModel.aiMessages.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val pageNotes by viewModel.pageNotes.collectAsState()
    val hasNotes by viewModel.hasNotes.collectAsState()
    val screenshotPath by viewModel.screenshotPath.collectAsState()

    // ═══ WebView محفوظ بشكل ثابت ═══
    var webView by remember { mutableStateOf<WebView?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showSummarySheet by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showScreenshot by remember { mutableStateOf(false) }

    val history by viewModel.browserHistory.collectAsState(initial = emptyList())
    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())

    LaunchedEffect(siteId) {
        if (siteId > 0) viewModel.loadSite(siteId)
    }

    // ═══════════════════════════════════════════
    // تنفيذ JavaScript - المُصلَح
    // ═══════════════════════════════════════════
    LaunchedEffect(pendingJs) {
        val script = pendingJs ?: return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect

        // تأخير بسيط للتأكد من جاهزية WebView
        delay(100)

        wv.post {
            wv.evaluateJavascript(script) { result ->
                viewModel.onJsExecuted()
                if (result != null && result != "null") {
                    viewModel.onJavascriptResult(result)
                }
            }
        }
    }

    // ═══ مراقبة المحتوى الديناميكي ═══
    LaunchedEffect(uiState.isTranslationMode) {
        if (!uiState.isTranslationMode) return@LaunchedEffect
        while (true) {
            delay(2000L)
            if (webView != null && !uiState.isTranslating) {
                viewModel.pollDynamicTranslation()
            }
        }
    }

    // ═══ عرض لقطة الشاشة ═══
    LaunchedEffect(screenshotPath) {
        if (screenshotPath != null) showScreenshot = true
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
                            text = uiState.url
                                .removePrefix("https://")
                                .removePrefix("http://"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                                .copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
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

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "المزيد")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Summarize,
                                            null,
                                            Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("ملخص ذكي")
                                    }
                                },
                                onClick = {
                                    showSummarySheet = true
                                    viewModel.summarizePage()
                                    showMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            null,
                                            Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("مساعد AI")
                                    }
                                },
                                onClick = {
                                    showAiChat = true
                                    showMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (hasNotes) Badge()
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.NoteAdd,
                                                null,
                                                Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text("الملاحظات")
                                    }
                                },
                                onClick = {
                                    showNotesSheet = true
                                    showMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Camera,
                                            null,
                                            Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("لقطة شاشة")
                                    }
                                },
                                onClick = {
                                    webView?.let {
                                        viewModel.takeScreenshot(it)
                                    }
                                    showMenu = false
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (uiState.isTranslationMode)
                                                Icons.Default.Undo
                                            else
                                                Icons.Default.Translate,
                                            null,
                                            Modifier.size(18.dp),
                                            tint = if (uiState.isTranslationMode)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (uiState.isTranslationMode)
                                                "إلغاء الترجمة"
                                            else
                                                "ترجمة الصفحة"
                                        )
                                    }
                                },
                                onClick = {
                                    if (uiState.isTranslationMode) {
                                        viewModel.resetTranslation()
                                    } else {
                                        viewModel.showTranslationSheet()
                                    }
                                    showMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("سجل التصفح") },
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
                                text = {
                                    Text(
                                        "مسح التاريخ",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
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
                            Modifier.size(22.dp)
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
                            Modifier.size(22.dp)
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
                            Modifier.size(22.dp)
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
                            Modifier.size(22.dp)
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
            // ═══════════════════════════════════════
            // WebView - المُصلَح
            // ═══════════════════════════════════════
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
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
                            // ═══ مهم للـ JS ═══
                            javaScriptCanOpenWindowsAutomatically = true
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                viewModel.setLoading(true)
                                if (!url.isNullOrBlank()) {
                                    viewModel.loadUrl(url)
                                }
                            }

                            override fun onPageFinished(
                                view: WebView?,
                                url: String?
                            ) {
                                viewModel.setLoading(false)
                                if (!url.isNullOrBlank()) {
                                    viewModel.loadUrl(url)
                                }
                                viewModel.updateNavigation(
                                    canGoBack(),
                                    canGoForward()
                                )
                                viewModel.saveToHistory()
                                viewModel.onPageFinishedForTranslation()
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
                    }
                },
                update = { view ->
                    // ═══ حفظ مرجع WebView ═══
                    webView = view

                    // ═══ تحميل الرابط فقط إذا تغيّر ═══
                    val currentUrl = view.url
                    val targetUrl = uiState.url
                    if (
                        targetUrl.isNotBlank() &&
                        currentUrl != targetUrl &&
                        currentUrl == null
                    ) {
                        view.loadUrl(targetUrl)
                    }
                }
            )

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
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme
                            .primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "جارٍ الترجمة...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme
                                    .onPrimaryContainer
                            )
                            Text(
                                "${(uiState.translationProgress * 100)
                                    .toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
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
                        TextButton(
                            onClick = { viewModel.clearError() }
                        ) {
                            Text("حسناً")
                        }
                    }
                ) { Text(text = error) }
            }
        }
    }

    // ═══ ورقة الترجمة ═══
    if (uiState.showTranslationSheet) {
        TranslationBottomSheet(
            currentLanguage = uiState.targetLanguage,
            onLanguageSelected = { lang ->
                viewModel.setTargetLanguage(lang)
            },
            onTranslatePage = {
                viewModel.startPageTranslation()
            },
            onTranslateSelection = {
                viewModel.hideTranslationSheet()
                viewModel.translateSelectedText()
            },
            onDismiss = { viewModel.hideTranslationSheet() }
        )
    }

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

    if (showSummarySheet) {
        SummaryBottomSheet(
            summary = pageSummary,
            isLoading = isSummarizing,
            pageTitle = uiState.title,
            onDismiss = {
                showSummarySheet = false
                viewModel.clearSummary()
            },
            onShare = { text ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(
                    Intent.createChooser(intent, "مشاركة الملخص")
                )
            }
        )
    }

    if (showAiChat) {
        AiChatBottomSheet(
            messages = aiMessages,
            isThinking = isAiThinking,
            pageTitle = uiState.title,
            onSendMessage = { viewModel.askAiAboutPage(it) },
            onClear = { viewModel.clearAiMessages() },
            onDismiss = { showAiChat = false }
        )
    }

    if (showNotesSheet) {
        PageNotesBottomSheet(
            notes = pageNotes,
            pageTitle = uiState.title,
            onAddNote = { viewModel.addNote(it) },
            onUpdateNote = { note, text ->
                viewModel.updateNote(note, text)
            },
            onDeleteNote = { viewModel.deleteNote(it) },
            onDismiss = { showNotesSheet = false }
        )
    }

    if (showScreenshot && screenshotPath != null) {
        ScreenshotDialog(
            imagePath = screenshotPath!!,
            onShare = {
                val file = java.io.File(screenshotPath!!)
                val uri = androidx.core.content.FileProvider
                    .getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, "مشاركة لقطة الشاشة")
                )
            },
            onDismiss = {
                showScreenshot = false
                viewModel.clearScreenshot()
            }
        )
    }
}
