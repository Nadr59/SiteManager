package com.nadr59.sitemanager.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.nadr59.sitemanager.data.local.SiteDatabase
import com.nadr59.sitemanager.data.remote.AiConfig
import com.nadr59.sitemanager.data.remote.AiService
import com.nadr59.sitemanager.data.remote.AnalysisResult
import com.nadr59.sitemanager.data.remote.WebScraper
import com.nadr59.sitemanager.data.repository.AnalyzerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════
// ═══ الشاشة الرئيسية للتحليل ═══
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    siteId: Int,
    siteName: String,
    siteUrl: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repo = remember {
        val db = SiteDatabase.getDatabase(context)
        val dao = db.siteDao()
        val scraper = WebScraper()
        val ai = AiService(Gson())
        AnalyzerRepository(scraper, ai, dao)
    }

    var isLoading by remember { mutableStateOf(false) }
    var loadingStep by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<AnalysisResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isFromCache by remember { mutableStateOf(false) }

    fun loadConfig(): AiConfig {
        val prefs = context.getSharedPreferences("sitemanager_prefs", Context.MODE_PRIVATE)
        return AiConfig(
            provider = prefs.getString("ai_provider", "groq") ?: "groq",
            apiKey = prefs.getString("ai_key", "") ?: "",
            model = prefs.getString("ai_model", "") ?: "",
            baseUrl = prefs.getString("ai_base_url", "") ?: ""
        )
    }

    fun startAnalysis(forceRefresh: Boolean = false) {
        scope.launch {
            isLoading = true
            error = null
            loadingStep = ""

            try {
                val config = withContext(Dispatchers.IO) { loadConfig() }

                if (config.apiKey.isBlank()) {
                    error = "أدخل مفتاح AI من الإعدادات أولاً"
                    isLoading = false
                    return@launch
                }

                if (!forceRefresh) {
                    val cached = withContext(Dispatchers.IO) { repo.getCachedAnalysis(siteId) }
                    val stale = withContext(Dispatchers.IO) { repo.isAnalysisStale(siteId) }
                    if (cached != null && !stale) {
                        result = cached
                        isFromCache = true
                        isLoading = false
                        return@launch
                    }
                }

                loadingStep = "جارٍ جمع محتوى الموقع..."
                withContext(Dispatchers.IO) { kotlinx.coroutines.delay(300) }
                loadingStep = "جارٍ التحليل بالذكاء الاصطناعي..."

                val res = withContext(Dispatchers.IO) { repo.analyze(siteId, config) }

                res.fold(
                    onSuccess = {
                        result = it
                        isFromCache = false
                        error = null
                    },
                    onFailure = { e ->
                        val fallback = withContext(Dispatchers.IO) { repo.getCachedAnalysis(siteId) }
                        if (fallback != null) {
                            result = fallback
                            isFromCache = true
                            error = "عرض تحليل محفوظ (${e.message})"
                        } else {
                            error = e.message
                        }
                    }
                )
            } catch (e: Exception) {
                error = e.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(siteId) {
        startAnalysis(forceRefresh = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("شرح الموقع", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            siteName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // زر مشاركة
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine("تقرير تحليل: $siteName")
                            appendLine("الرابط: $siteUrl")
                            result?.let { r ->
                                if (r.rating > 0f) {
                                    appendLine("التقييم: ${String.format("%.1f", r.rating)} / 10")
                                }
                                if (r.overview.isNotBlank()) {
                                    appendLine("")
                                    appendLine(r.overview.take(300))
                                }
                            }
                            appendLine("")
                            appendLine("— تم التحليل عبر مدير المواقع")
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            putExtra(Intent.EXTRA_SUBJECT, "تحليل: $siteName")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة")
                    }

                    // زر تحديث
                    IconButton(
                        onClick = { startAnalysis(forceRefresh = true) },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> LoadingView(loadingStep)
                error != null && result == null -> ErrorView(
                    error = error!!,
                    onRetry = { startAnalysis(forceRefresh = true) },
                    onBack = onBack
                )
                result != null -> AnalysisResultContent(
                    result = result!!,
                    isFromCache = isFromCache,
                    warning = error
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ═══ شاشة التحميل ═══
// ═══════════════════════════════════════════════════════════
@Composable
private fun LoadingView(step: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(Modifier.height(20.dp))
        Text(step, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "قد يستغرق هذا بعض الوقت...",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════
// ═══ شاشة الخطأ ═══
// ═══════════════════════════════════════════════════════════
@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text("حدث خطأ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            error,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("رجوع") }
            Button(onClick = onRetry) { Text("إعادة المحاولة") }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ═══ محتوى النتائج ═══
// ═══════════════════════════════════════════════════════════
@Composable
private fun AnalysisResultContent(
    result: AnalysisResult,
    isFromCache: Boolean,
    warning: String?
) {
    val context = LocalContext.current

    fun copyFullText() {
        val text = buildString {
            appendLine("═".repeat(50))
            appendLine("تقرير تحليل الموقع")
            appendLine("═".repeat(50))
            if (result.rating > 0f) {
                appendLine("\n⭐ التقييم: ${String.format("%.1f", result.rating)} / 10")
            }
            if (result.overview.isNotBlank()) {
                appendLine("\n🔍 نظرة عامة:")
                appendLine(result.overview)
            }
            if (result.purpose.isNotBlank()) {
                appendLine("\n🎯 الغرض والهدف:")
                appendLine(result.purpose)
            }
            if (result.features.isNotEmpty()) {
                appendLine("\n⭐ الميزات:")
                result.features.forEach { appendLine("  • $it") }
            }
            if (result.techStack.isNotEmpty()) {
                appendLine("\n🛠 التقنيات:")
                result.techStack.forEach { appendLine("  • $it") }
            }
            if (result.howToUse.isNotBlank()) {
                appendLine("\n🚀 كيفية البدء:")
                appendLine(result.howToUse)
            }
            if (result.examples.isNotBlank()) {
                appendLine("\n💻 أمثلة:")
                appendLine(result.examples)
            }
            if (result.prosAndCons.pros.isNotEmpty()) {
                appendLine("\n✅ نقاط القوة:")
                result.prosAndCons.pros.forEach { appendLine("  • $it") }
            }
            if (result.prosAndCons.cons.isNotEmpty()) {
                appendLine("\n⚠️ نقاط الضعف:")
                result.prosAndCons.cons.forEach { appendLine("  • $it") }
            }
            appendLine("\n" + "═".repeat(50))
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("analysis", text))
        Toast.makeText(context, "تم نسخ النص الكامل", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ═══ زر نسخ الكل ═══
        item {
            Button(
                onClick = { copyFullText() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("نسخ النص الكامل", fontWeight = FontWeight.Bold)
            }
        }

        // ═══ تحذير التخزين المؤقت ═══
        if (isFromCache) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            warning ?: "عرض تحليل محفوظ — اضغط التحديث",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }

        // ═══ التقييم ═══
        if (result.rating > 0f) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            String.format("%.1f", result.rating),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(" / 10", fontSize = 20.sp, fontWeight = FontWeight.Light)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            when {
                                result.rating >= 8f -> "ممتاز"
                                result.rating >= 6f -> "جيد"
                                result.rating >= 4f -> "مقبول"
                                else -> "يحتاج تحسين"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ═══ الأقسام ═══
        if (result.overview.isNotBlank()) {
            item { AnalysisSectionCard("نظرة عامة", result.overview) }
        }
        if (result.purpose.isNotBlank()) {
            item { AnalysisSectionCard("الغرض والهدف", result.purpose) }
        }
        if (result.features.isNotEmpty()) {
            item { AnalysisBulletCard("الميزات", result.features, Color(0xFF5BD9A8)) }
        }
        if (result.techStack.isNotEmpty()) {
            item { AnalysisBulletCard("التقنيات", result.techStack, Color(0xFF5B8DD9)) }
        }
        if (result.howToUse.isNotBlank()) {
            item { AnalysisSectionCard("كيفية البدء", result.howToUse) }
        }
        if (result.examples.isNotBlank()) {
            item { AnalysisCodeCard("أمثلة", result.examples) }
        }
        if (result.prosAndCons.pros.isNotEmpty()) {
            item { AnalysisBulletCard("نقاط القوة", result.prosAndCons.pros, Color(0xFF4CAF50)) }
        }
        if (result.prosAndCons.cons.isNotEmpty()) {
            item { AnalysisBulletCard("نقاط الضعف", result.prosAndCons.cons, Color(0xFFFF9800)) }
        }

        // ═══ النص الكامل القابل للتوسيع ═══
        if (result.rawMarkdown.isNotBlank()) {
            item {
                var expanded by remember { mutableStateOf(false) }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("النص الكامل", fontWeight = FontWeight.Bold)
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(visible = expanded) {
                            Text(
                                result.rawMarkdown,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 12.dp),
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════
// ═══ بطاقة قسم عادي ═══
// ═══════════════════════════════════════════════════════════
@Composable
private fun AnalysisSectionCard(title: String, content: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Text(content, fontSize = 14.sp, lineHeight = 24.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ═══ بطاقة قائمة نقاط ═══
// ═══════════════════════════════════════════════════════════
@Composable
private fun AnalysisBulletCard(title: String, items: List<String>, accent: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(8.dp)
                    ) {}
                    Text(item, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ═══ بطاقة كود ═══
// ═══════════════════════════════════════════════════════════
@Composable
private fun AnalysisCodeCard(title: String, code: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1A1A2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    code,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF4EC9B0),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
