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
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
// ═══ شاشة التحليل الرئيسية ═══
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
    var showProviderMenu by remember { mutableStateOf(false) }

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
            loadingStep = "جارٍ التحقق من الإعدادات..."

            try {
                val config = withContext(Dispatchers.IO) { loadConfig() }

                if (config.apiKey.isBlank()) {
                    error = "أدخل مفتاح AI من الإعدادات أولاً"
                    isLoading = false
                    return@launch
                }

                // محاولة التخزين المؤقت
                if (!forceRefresh) {
                    loadingStep = "البحث عن تحليل محفوظ..."
                    val cached = withContext(Dispatchers.IO) { repo.getCachedAnalysis(siteId) }
                    val stale = withContext(Dispatchers.IO) { repo.isAnalysisStale(siteId) }
                    if (cached != null && !stale) {
                        result = cached
                        isFromCache = true
                        isLoading = false
                        return@launch
                    }
                }

                // جمع المحتوى
                loadingStep = "جارٍ جمع محتوى الموقع..."
                withContext(Dispatchers.IO) { kotlinx.coroutines.delay(500) }

                // التحليل
                loadingStep = "جارٍ إرسال المحتوى للذكاء الاصطناعي..."
                withContext(Dispatchers.IO) { kotlinx.coroutines.delay(300) }
                loadingStep = "جارٍ التحليل... قد يستغرق هذا 30-60 ثانية"

                val res = withContext(Dispatchers.IO) { repo.analyze(siteId, config) }

                res.fold(
                    onSuccess = {
                        result = it
                        isFromCache = false
                        error = null
                    },
                    onFailure = { e ->
                        val msg = formatErrorMessage(e)
                        val fallback = withContext(Dispatchers.IO) { repo.getCachedAnalysis(siteId) }
                        if (fallback != null) {
                            result = fallback
                            isFromCache = true
                            error = "فشل التحليل: $msg"
                        } else {
                            error = msg
                        }
                    }
                )
            } catch (e: Exception) {
                error = formatErrorMessage(e)
            }
            isLoading = false
        }
    }

    fun changeProviderAndAnalyze(provider: String) {
        val prefs = context.getSharedPreferences("sitemanager_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("ai_provider", provider).apply()
        startAnalysis(forceRefresh = true)
    }

    // بدء التحليل عند فتح الشاشة
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
                        shareAnalysis(context, siteName, siteUrl, result)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة")
                    }

                    // زر تغيير المزود
                    Box {
                        IconButton(onClick = { showProviderMenu = true }) {
                            Icon(Icons.Filled.SwapVert, contentDescription = "تغيير المزود")
                        }
                        DropdownMenu(
                            expanded = showProviderMenu,
                            onDismissRequest = { showProviderMenu = false }
                        ) {
                            listOf(
                                "Groq (مجاني)" to "groq",
                                "HCNSEC" to "hcnsec",
                                "OpenRouter" to "openrouter",
                                "Gemini" to "gemini"
                            ).forEach { (label, key) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        showProviderMenu = false
                                        changeProviderAndAnalyze(key)
                                    }
                                )
                            }
                        }
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
// ═══ تنسيق رسائل الخطأ ═══
// ═══════════════════════════════════════════════════════════
private fun formatErrorMessage(e: Throwable): String {
    val msg = e.message ?: "خطأ غير معروف"
    return when {
        msg.contains("timeout", true) ->
            "انتهت المهلة — الخادم بطيء. حاول مرة أخرى أو جرّب مزود آخر من ⬆"
        msg.contains("429") ->
            "تم تجاوز حد الطلبات — انتظر دقيقتين ثم حاول"
        msg.contains("401") ->
            "مفتاح API غير صالح — تحقق من الإعدادات"
        msg.contains("403") || msg.contains("402") ->
            "مفتاح API مرفوض أو منتهي الصلاحية"
        msg.contains("connect", true) || msg.contains("network", true) ->
            "فشل الاتصال بالخادم — تحقق من اتصال الإنترنت"
        msg.contains("resolve", true) ->
            "تعذر الوصول للخادم — تحقق من رابط API"
        msg.contains("مفتاح", true) ->
            msg
        else -> "خطأ: $msg"
    }
}

// ═══════════════════════════════════════════════════════════
// ═══ مشاركة نتائج التحليل ═══
// ═══════════════════════════════════════════════════════════
private fun shareAnalysis(
    context: Context,
    siteName: String,
    siteUrl: String,
    result: AnalysisResult?
) {
    val text = buildString {
        appendLine("تقرير تحليل: $siteName")
        appendLine("الرابط: $siteUrl")
        result?.let { r ->
            if (r.rating > 0f) {
                appendLine("التقييم: ${String.format("%.1f", r.rating)} / 10")
            }
            if (r.overview.isNotBlank()) {
                appendLine("")
                appendLine(r.overview.take(500))
            }
            if (r.features.isNotEmpty()) {
                appendLine("")
                appendLine("الميزات:")
                r.features.take(5).forEach { appendLine("  • $it") }
            }
        }
        appendLine("")
        appendLine("— تم التحليل عبر مدير المواقع")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "تحليل: $siteName")
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة التحليل"))
}

// ═══════════════════════════════════════════════════════════
// ═══ نسخ النص الكامل ═══
// ═══════════════════════════════════════════════════════════
private fun copyFullText(context: Context, result: AnalysisResult) {
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
            "قد يستغرق هذا 30-60 ثانية...",
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
        Spacer(Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Text(
                error,
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                lineHeight = 22.sp
            )
        }
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ═══ أزرار الإجراءات ═══
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // زر نسخ
                Button(
                    onClick = { copyFullText(context, result) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("نسخ", fontWeight = FontWeight.Bold)
                }

                // زر مشاركة
                Button(
                    onClick = {
                        shareAnalysis(context, "", "", result)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("مشاركة", fontWeight = FontWeight.Bold)
                }
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
                            warning ?: "عرض تحليل محفوظ — اضغط ⟳ للتحديث",
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
                                result.rating >= 8f -> "ممتاز 🌟"
                                result.rating >= 6f -> "جيد 👍"
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

        // ═══ نظرة عامة ═══
        if (result.overview.isNotBlank()) {
            item { AnalysisSectionCard("🔍 نظرة عامة", result.overview) }
        }

        // ═══ الغرض والهدف ═══
        if (result.purpose.isNotBlank()) {
            item { AnalysisSectionCard("🎯 الغرض والهدف", result.purpose) }
        }

        // ═══ الميزات ═══
        if (result.features.isNotEmpty()) {
            item {
                AnalysisBulletCard(
                    title = "⭐ الميزات الرئيسية",
                    items = result.features,
                    accent = Color(0xFF5BD9A8)
                )
            }
        }

        // ═══ التقنيات ═══
        if (result.techStack.isNotEmpty()) {
            item {
                AnalysisBulletCard(
                    title = "🛠 التقنيات المستخدمة",
                    items = result.techStack,
                    accent = Color(0xFF5B8DD9)
                )
            }
        }

        // ═══ طريقة الاستخدام ═══
        if (result.howToUse.isNotBlank()) {
            item { AnalysisSectionCard("🚀 كيفية البدء", result.howToUse) }
        }

        // ═══ الأمثلة ═══
        if (result.examples.isNotBlank()) {
            item { AnalysisCodeCard("💻 أمثلة عملية", result.examples) }
        }

        // ═══ نقاط القوة ═══
        if (result.prosAndCons.pros.isNotEmpty()) {
            item {
                AnalysisBulletCard(
                    title = "✅ نقاط القوة",
                    items = result.prosAndCons.pros,
                    accent = Color(0xFF4CAF50)
                )
            }
        }

        // ═══ نقاط الضعف ═══
        if (result.prosAndCons.cons.isNotEmpty()) {
            item {
                AnalysisBulletCard(
                    title = "⚠️ نقاط الضعف",
                    items = result.prosAndCons.cons,
                    accent = Color(0xFFFF9800)
                )
            }
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
                                if (expanded) Icons.Filled.ExpandLess
                                else Icons.Filled.ExpandMore,
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

        // مساحة سفلية
        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════
// ═══ بطاقة قسم نصي ═══
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
            Text(
                content,
                fontSize = 14.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ═══ بطاقة قائمة نقاط ═══
// ═══════════════════════════════════════════════════════════
@Composable
private fun AnalysisBulletCard(
    title: String,
    items: List<String>,
    accent: Color
) {
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
                    Text(
                        item,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
