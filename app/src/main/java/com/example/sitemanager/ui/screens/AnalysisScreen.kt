package com.nadr59.sitemanager.ui.screens

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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.nadr59.sitemanager.data.remote.AiConfig
import com.nadr59.sitemanager.data.remote.AiService
import com.nadr59.sitemanager.data.remote.AnalysisResult
import com.nadr59.sitemanager.data.remote.ProsAndCons
import com.nadr59.sitemanager.data.remote.WebScraper
import com.nadr59.sitemanager.data.repository.AnalyzerRepository
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    siteId: Int,
    siteName: String,
    siteUrl: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // تهيئة الخدمات محلياً (بدون Hilt لتبسيط البناء)
    val repo = remember {
        val db = com.nadr59.sitemanager.data.local.SiteDatabase.getDatabase(context)
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

    fun doAnalysis(forceRefresh: Boolean = false) {
        val config = loadConfig()
        if (config.apiKey.isBlank()) {
            error = "أدخل مفتاح AI من الإعدادات أولاً"
            return
        }

        isLoading = true
        error = null

        kotlinx.coroutines.MainScope().launch {
            try {
                // محاولة التخزين المؤقت أولاً (إذا لم يكن طلب تحديث)
                if (!forceRefresh) {
                    val cached = repo.getCachedAnalysis(siteId)
                    val stale = repo.isAnalysisStale(siteId)
                    if (cached != null && !stale) {
                        result = cached
                        isFromCache = true
                        isLoading = false
                        return@launch
                    }
                }

                loadingStep = "جارٍ جمع المحتوى..."
                kotlinx.coroutines.delay(300)
                loadingStep = "جارٍ التحليل بالذكاء الاصطناعي..."

                val res = repo.analyze(siteId, config)
                res.fold(
                    onSuccess = {
                        result = it
                        isFromCache = false
                        error = null
                    },
                    onFailure = { e ->
                        val fallback = repo.getCachedAnalysis(siteId)
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

    // بدء التحليل عند فتح الشاشة
    LaunchedEffect(siteId) {
        doAnalysis(forceRefresh = false)
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
                    IconButton(
                        onClick = { doAnalysis(forceRefresh = true) },
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
                isLoading -> {
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
                        Text(loadingStep, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "قد يستغرق هذا بعض الوقت...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                error != null && result == null -> {
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
                            error ?: "",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onBack) { Text("رجوع") }
                            Button(onClick = { doAnalysis(forceRefresh = true) }) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }

                result != null -> {
                    AnalysisContent(
                        result = result!!,
                        isFromCache = isFromCache,
                        warning = error
                    )
                }
            }
        }
    }
}

// ═══ محتوى النتائج ═══
@Composable
private fun AnalysisContent(
    result: AnalysisResult,
    isFromCache: Boolean,
    warning: String?
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // تحذير
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

        // التقييم
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
                        Text(
                            " / 10",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light
                        )
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

        // الأقسام
        if (result.overview.isNotBlank()) {
            item { AnalysisSectionCard("نظرة عامة", result.overview) }
        }
        if (result.purpose.isNotBlank()) {
            item { AnalysisSectionCard("الغرض والهدف", result.purpose) }
        }
        if (result.features.isNotEmpty()) {
            item {
                AnalysisBulletCard("الميزات الرئيسية", result.features, Color(0xFF5BD9A8))
            }
        }
        if (result.techStack.isNotEmpty()) {
            item {
                AnalysisBulletCard("التقنيات المستخدمة", result.techStack, Color(0xFF5B8DD9))
            }
        }
        if (result.howToUse.isNotBlank()) {
            item { AnalysisSectionCard("كيفية البدء", result.howToUse) }
        }
        if (result.examples.isNotBlank()) {
            item { AnalysisCodeCard("أمثلة عملية", result.examples) }
        }
        if (result.prosAndCons.pros.isNotEmpty()) {
            item { AnalysisBulletCard("نقاط القوة", result.prosAndCons.pros, Color(0xFF4CAF50)) }
        }
        if (result.prosAndCons.cons.isNotEmpty()) {
            item { AnalysisBulletCard("نقاط الضعف", result.prosAndCons.cons, Color(0xFFFF9800)) }
        }

        // النص الكامل
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

        item { Spacer(Modifier.height(32.dp)) }
    }
}

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
