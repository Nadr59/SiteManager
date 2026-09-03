package com.nadr59.sitemanager.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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

    // ═══ تنفيذ JavaScript ═══
    LaunchedEffect(pendingJs) {
        val script = pendingJs
        if (script != null && webView != null) {
            webView?.evaluateJavascript(script) { result ->
                viewModel.onJsExecuted()
                viewModel.onJavascriptResult(result)
            }
        }
    }
    // ═══ مراقبة المحتوى الديناميكي أثناء الترجمة ═══
LaunchedEffect(uiState.isTranslationMode, webView) {
    if (!uiState.isTranslationMode) return@LaunchedEffect

    while (true) {
        delay(1500L)

        if (webView != null) {
            viewModel.pollDynamicTranslation()
        }
    }
}

    // ═══ عرض لقطة الشاشة تلقائياً ═══
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

                    // ═══ المزيد ═══
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "المزيد")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // ═══ ملخص ذكي ═══
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                            // ═══ مساعد AI ═══
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                            // ═══ ملاحظات ═══
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                            // ═══ لقطة شاشة ═══
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    webView?.let { viewModel.takeScreenshot(it) }
                                    showMenu = false
                                }
                            )

                            HorizontalDivider()

                            // ═══ الترجمة ═══
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (uiState.isTranslationMode)
                                                Icons.Default.Undo
                                            else
                                                Icons.Default.Translate,
                                            null,
                                            Modifier.size(18.dp)
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

                            // ═══ التاريخ ═══
                            DropdownMenuItem(
                                text = { Text("سجل التصفح") },
                                onClick = {
                                    showHistory = true
                                    showMenu = false
                                }
                            )

                            // ═══ الإشارات ═══
                            DropdownMenuItem(
                                text = { Text("الإشارات المرجعية") },
                                onClick = {
                                    showBookmarks = true
                                    showMenu = false
                                }
                            )

                            // ═══ مسح التاريخ ═══
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
                    icon = { Icon(Icons.Default.Refresh, "تحديث", Modifier.size(22.dp)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { webView?.loadUrl(uiState.url) },
                    icon = { Icon(Icons.Default.Home, "الرئيسية", Modifier.size(22.dp)) }
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
                    factory = { ctx ->
                        WebView(ctx).apply {
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
                                    if (!url.isNullOrBlank()) viewModel.loadUrl(url)
                                }

                                override fun onPageFinished(
                                    view: WebView?,
                                    url: String?
                                ) {
                                    viewModel.setLoading(false)

                                       if (!url.isNullOrBlank()) {
                                        viewModel.loadUrl(url)
                                    }

                                      viewModel.updateNavigation(canGoBack(), canGoForward())
                                       viewModel.saveToHistory()

                                         // إذا كانت الترجمة مفعلة قبل إعادة تحميل الصفحة،
                                         // سيعاد تجهيز المراقب بعد اكتمال الصفحة.
                                        viewModel.onPageFinishedForTranslation()
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
                    CircularProgressIndicator()
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer
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
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${(uiState.translationProgress * 100).toInt()}%",
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
                        TextButton(onClick = { viewModel.clearError() }) {
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

    // ═══ ورقة الملخص ═══
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
                context.startActivity(Intent.createChooser(intent, "مشاركة الملخص"))
            }
        )
    }

    // ═══ ورقة مساعد AI ═══
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

    // ═══ ورقة الملاحظات ═══
    if (showNotesSheet) {
        PageNotesBottomSheet(
            notes = pageNotes,
            pageTitle = uiState.title,
            onAddNote = { viewModel.addNote(it) },
            onUpdateNote = { note, text -> viewModel.updateNote(note, text) },
            onDeleteNote = { viewModel.deleteNote(it) },
            onDismiss = { showNotesSheet = false }
        )
    }

    // ═══ عرض لقطة الشاشة ═══
    if (showScreenshot && screenshotPath != null) {
        ScreenshotDialog(
            imagePath = screenshotPath!!,
            onShare = {
                val file = java.io.File(screenshotPath!!)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "مشاركة لقطة الشاشة"))
            },
            onDismiss = {
                showScreenshot = false
                viewModel.clearScreenshot()
            }
        )
    }
}

// ═══════════════════════════════════════════════
// ورقة الملخص الذكي
// ═══════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryBottomSheet(
    summary: String?,
    isLoading: Boolean,
    pageTitle: String,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Summarize,
                        null,
                        Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "ملخص ذكي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                if (summary != null) {
                    IconButton(onClick = { onShare(summary) }) {
                        Icon(Icons.Default.Share, "مشاركة")
                    }
                }
            }

            if (pageTitle.isNotBlank()) {
                Text(
                    pageTitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            when {
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "جارٍ تلخيص الصفحة...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
                summary != null -> {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp
                    )
                }
                else -> {
                    Text(
                        "لا يوجد ملخص متاح",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════
// ورقة مساعد AI
// ═══════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatBottomSheet(
    messages: List<AiMessage>,
    isThinking: Boolean,
    pageTitle: String,
    onSendMessage: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // ═══ العنوان ═══
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                null,
                                Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("مساعد AI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "اسأل عن محتوى هذه الصفحة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (messages.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("مسح", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ═══ أسئلة سريعة ═══
            if (messages.isEmpty()) {
                val quickQuestions = listOf(
                    "ما موضوع هذه الصفحة؟",
                    "لخّص المحتوى في نقاط",
                    "ما أهم المعلومات هنا؟",
                    "هل هذا المحتوى موثوق؟"
                )
                Column {
                    Text(
                        "أسئلة سريعة:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    quickQuestions.forEach { q ->
                        Surface(
                            onClick = {
                                question = q
                                onSendMessage(q)
                                question = ""
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                q,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ═══ الرسائل ═══
            if (messages.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageBubble(message = message)
                    }
                    if (isThinking) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("يفكر...", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ═══ حقل الإدخال ═══
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("اكتب سؤالك...", fontSize = 13.sp) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    enabled = !isThinking
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = {
                        if (question.isNotBlank() && !isThinking) {
                            onSendMessage(question)
                            question = ""
                        }
                    },
                    shape = CircleShape,
                    color = if (question.isNotBlank() && !isThinking)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            null,
                            Modifier.size(20.dp),
                            tint = if (question.isNotBlank() && !isThinking)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ChatMessageBubble(message: AiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser)
            Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = when {
                message.isError -> MaterialTheme.colorScheme.errorContainer
                message.isUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp,
                color = when {
                    message.isError -> MaterialTheme.colorScheme.onErrorContainer
                    message.isUser -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                lineHeight = 20.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════
// ورقة الملاحظات
// ═══════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageNotesBottomSheet(
    notes: List<PageNote>,
    pageTitle: String,
    onAddNote: (String) -> Unit,
    onUpdateNote: (PageNote, String) -> Unit,
    onDeleteNote: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newNoteText by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<PageNote?>(null) }
    var editText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.NoteAdd,
                    null,
                    Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("ملاحظاتي", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (pageTitle.isNotBlank()) {
                        Text(
                            pageTitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    "${notes.size} ملاحظة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // ═══ إضافة ملاحظة جديدة ═══
            OutlinedTextField(
                value = newNoteText,
                onValueChange = { newNoteText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("اكتب ملاحظة عن هذه الصفحة...") },
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 4,
                trailingIcon = {
                    if (newNoteText.isNotBlank()) {
                        IconButton(onClick = {
                            onAddNote(newNoteText)
                            newNoteText = ""
                        }) {
                            Icon(
                                Icons.Default.Save,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            // ═══ قائمة الملاحظات ═══
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📝", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "لا توجد ملاحظات بعد",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            if (editingNote?.id == note.id) {
                                // ═══ وضع التعديل ═══
                                Column(modifier = Modifier.padding(12.dp)) {
                                    OutlinedTextField(
                                        value = editText,
                                        onValueChange = { editText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        minLines = 2
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { editingNote = null }) {
                                            Text("إلغاء")
                                        }
                                        Button(onClick = {
                                            onUpdateNote(note, editText)
                                            editingNote = null
                                        }) {
                                            Text("حفظ")
                                        }
                                    }
                                }
                            } else {
                                // ═══ عرض الملاحظة ═══
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        note.note,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                    Column {
                                        IconButton(
                                            onClick = {
                                                editingNote = note
                                                editText = note.note
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                null,
                                                Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteNote(note.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                null,
                                                Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════
// حوار لقطة الشاشة
// ═══════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotDialog(
    imagePath: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Camera, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("لقطة الشاشة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "إغلاق")
                }
            }

            Spacer(Modifier.height(12.dp))

            // ═══ معاينة الصورة ═══
            AsyncImage(
                model = java.io.File(imagePath),
                contentDescription = "لقطة الشاشة",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("مشاركة")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إغلاق")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}


 
