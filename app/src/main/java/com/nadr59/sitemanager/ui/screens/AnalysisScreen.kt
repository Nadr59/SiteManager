package com.nadr59.sitemanager.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadr59.sitemanager.data.local.AnalysisType
import com.nadr59.sitemanager.data.local.SiteAnalysisEntity
import com.nadr59.sitemanager.data.local.SiteEntity
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalysisScreen(
    siteId: Int,
    viewModel: SiteViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val site: SiteEntity? = uiState.allSites.find { it.id == siteId }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // ═══ حالة التحليل ═══
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var analysisRating by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(AnalysisType.EXPLAIN) }
    var customQuestion by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var cachedAnalyses by remember { mutableStateOf<List<SiteAnalysisEntity>>(emptyList()) }
    var showCachedResult by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }

    // ═══ تحميل التحليلات المحفوظة ═══
    LaunchedEffect(siteId) {
        viewModel.getAnalysesForSite(siteId).collect { analyses ->
            cachedAnalyses = analyses
            // عرض أحدث تحليل تلقائياً
            if (analyses.isNotEmpty() && analysisResult == null) {
                val latest = analyses.first()
                analysisResult = latest.result
                analysisRating = latest.rating
                showCachedResult = true
            }
        }
    }

    // ═══ محاكاة تقدم التحليل ═══
    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            analysisProgress = 0f
            while (isAnalyzing && analysisProgress < 0.9f) {
                kotlinx.coroutines.delay(300)
                analysisProgress += 0.05f
            }
        } else {
            analysisProgress = if (analysisResult != null) 1f else 0f
        }
    }

    if (site == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "تحليل الموقع",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            site.name,
                            fontSize = 12.sp,
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
                    // ═══ زر السجل ═══
                    if (cachedAnalyses.isNotEmpty()) {
                        IconButton(onClick = { showHistory = !showHistory }) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "${cachedAnalyses.size}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Icon(Icons.Default.History, "السجل")
                        }
                    }
                    // ═══ زر المشاركة ═══
                    if (analysisResult != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "تحليل ${site.name}:\n\n$analysisResult"
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "مشاركة التحليل")
                            )
                        }) {
                            Icon(Icons.Default.Share, "مشاركة")
                        }
                        // ═══ زر النسخ ═══
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(analysisResult ?: ""))
                        }) {
                            Icon(Icons.Default.ContentCopy, "نسخ")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══ شريط التقدم ═══
            AnimatedVisibility(visible = isAnalyzing) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "جارٍ التحليل بالذكاء الاصطناعي...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${(analysisProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { analysisProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            // ═══ معلومات الموقع ═══
            SiteInfoCard(site = site, modifier = Modifier.padding(horizontal = 16.dp))

            // ═══ سجل التحليلات ═══
            AnimatedVisibility(
                visible = showHistory && cachedAnalyses.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AnalysisHistorySection(
                    analyses = cachedAnalyses,
                    onSelect = { analysis ->
                        analysisResult = analysis.result
                        analysisRating = analysis.rating
                        showCachedResult = true
                        showHistory = false
                    },
                    onDelete = { analysis ->
                        scope.launch {
                            viewModel.analyzerRepository.clearCache(siteId)
                            cachedAnalyses = emptyList()
                            analysisResult = null
                            showCachedResult = false
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ═══ اختيار نوع التحليل ═══
            AnalysisTypeSelector(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // ═══ سؤال مخصص ═══
            AnimatedVisibility(visible = selectedType == AnalysisType.CUSTOM) {
                OutlinedTextField(
                    value = customQuestion,
                    onValueChange = { customQuestion = it },
                    label = { Text("اكتب سؤالك هنا...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4,
                    trailingIcon = {
                        if (customQuestion.isNotBlank()) {
                            IconButton(onClick = { customQuestion = "" }) {
                                Icon(Icons.Default.Delete, null)
                            }
                        }
                    }
                )
            }

            // ═══ زر التحليل ═══
            AnalyzeButton(
                isAnalyzing = isAnalyzing,
                selectedType = selectedType,
                customQuestion = customQuestion,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    if (selectedType == AnalysisType.CUSTOM && customQuestion.isBlank()) return@AnalyzeButton

                    isAnalyzing = true
                    errorMessage = null
                    showCachedResult = false

                    scope.launch {
                        try {
                            val result = viewModel.analyzerRepository.analyze(
                                siteId = siteId,
                                analysisType = selectedType,
                                customQuestion = customQuestion
                            )
                            result.fold(
                                onSuccess = { analysis ->
                                    analysisResult = analysis.rawMarkdown
                                    analysisRating = analysis.rating
                                    showCachedResult = false
                                    isAnalyzing = false
                                },
                                onFailure = { e ->
                                    errorMessage = e.message ?: "فشل التحليل"
                                    isAnalyzing = false
                                }
                            )
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "خطأ غير معروف"
                            isAnalyzing = false
                        }
                    }
                }
            )

            // ═══ تحليل محفوظ مسبقاً ═══
            if (showCachedResult && analysisResult != null) {
                CachedResultBanner(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onRefresh = {
                        showCachedResult = false
                        isAnalyzing = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val result = viewModel.analyzerRepository.analyze(
                                    siteId = siteId,
                                    analysisType = selectedType
                                )
                                result.fold(
                                    onSuccess = { analysis ->
                                        analysisResult = analysis.rawMarkdown
                                        analysisRating = analysis.rating
                                        isAnalyzing = false
                                    },
                                    onFailure = { e ->
                                        errorMessage = e.message
                                        isAnalyzing = false
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = e.message
                                isAnalyzing = false
                            }
                        }
                    }
                )
            }

            // ═══ التقييم ═══
            if (analysisRating > 0f) {
                RatingCard(
                    rating = analysisRating,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ═══ نتيجة التحليل - Markdown ═══
            AnimatedVisibility(
                visible = analysisResult != null && !isAnalyzing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MarkdownResultCard(
                    markdown = analysisResult ?: "",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ═══ رسالة الخطأ ═══
            AnimatedVisibility(visible = errorMessage != null) {
                ErrorCard(
                    message = errorMessage ?: "",
                    onRetry = {
                        errorMessage = null
                        isAnalyzing = true
                        scope.launch {
                            try {
                                val result = viewModel.analyzerRepository.analyze(
                                    siteId = siteId,
                                    analysisType = selectedType,
                                    customQuestion = customQuestion
                                )
                                result.fold(
                                    onSuccess = { analysis ->
                                        analysisResult = analysis.rawMarkdown
                                        analysisRating = analysis.rating
                                        isAnalyzing = false
                                    },
                                    onFailure = { e ->
                                        errorMessage = e.message
                                        isAnalyzing = false
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = e.message
                                isAnalyzing = false
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════
// المكونات الفرعية
// ═══════════════════════════════════════════════════

@Composable
private fun SiteInfoCard(site: SiteEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ═══ أيقونة الموقع ═══
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = site.name.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Text(
                    text = site.url,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
                if (site.category.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = site.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // ═══ تقييم AI السابق ═══
            if (site.aiRating > 0f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${site.aiRating}/10",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnalysisTypeSelector(
    selectedType: AnalysisType,
    onTypeSelected: (AnalysisType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "نوع التحليل",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnalysisType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = {
                        Text(
                            "${type.emoji} ${type.displayName}",
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AnalyzeButton(
    isAnalyzing: Boolean,
    selectedType: AnalysisType,
    customQuestion: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isEnabled = !isAnalyzing &&
        (selectedType != AnalysisType.CUSTOM || customQuestion.isNotBlank())

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        AnimatedContent(targetState = isAnalyzing) { analyzing ->
            if (analyzing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "جارٍ التحليل...",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedType == AnalysisType.CUSTOM) "إرسال السؤال"
                        else "تحليل بالذكاء الاصطناعي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CachedResultBanner(
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Cached,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "نتيجة محفوظة مسبقاً",
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تحديث", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RatingCard(rating: Float, modifier: Modifier = Modifier) {
    val color = when {
        rating >= 8f -> MaterialTheme.colorScheme.tertiary
        rating >= 5f -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(10) { index ->
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (index < rating.toInt()) color
                    else color.copy(alpha = 0.2f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "$rating/10",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
        }
    }
}

@Composable
private fun MarkdownResultCard(
    markdown: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "نتيجة التحليل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // ═══ عرض Markdown منسق ═══
            MarkdownText(
                markdown = markdown,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            )
        }
    }
}

@Composable
private fun AnalysisHistorySection(
    analyses: List<SiteAnalysisEntity>,
    onSelect: (SiteAnalysisEntity) -> Unit,
    onDelete: (SiteAnalysisEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "سجل التحليلات",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))

            analyses.take(5).forEach { analysis ->
                val type = AnalysisType.fromKey(analysis.analysisType)
                Surface(
                    onClick = { onSelect(analysis) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(type.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                type.displayName,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Text(
                                analysis.result.take(60) + "...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                        if (analysis.rating > 0f) {
                            Text(
                                "${analysis.rating}★",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { onDelete(analysis) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "❌ فشل التحليل",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إعادة المحاولة")
            }
        }
    }
}
