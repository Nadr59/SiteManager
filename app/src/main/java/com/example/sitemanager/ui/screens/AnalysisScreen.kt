package com.nadr59.sitemanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nadr59.sitemanager.data.remote.AnalysisResult
import com.nadr59.sitemanager.viewmodel.AnalysisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    siteId: Int,
    siteName: String,
    siteUrl: String,
    onBack: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(siteId) {
        viewModel.loadCachedOrAnalyze(siteId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "شرح الموقع",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            siteName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    // زر تحديث التحليل
                    IconButton(
                        onClick = { viewModel.refreshAnalysis(siteId) },
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, "تحديث")
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
                // ═══ حالة التحميل ═══
                state.isLoading -> {
                    LoadingContent(state.loadingStep)
                }

                // ═══ حالة الخطأ (بدون نتائج محفوظة) ═══
                state.error != null && state.result == null -> {
                    ErrorContent(
                        error = state.error!!,
                        onRetry = { viewModel.analyze(siteId) },
                        onBack = onBack
                    )
                }

                // ═══ حالة النتائج ═══
                state.result != null -> {
                    AnalysisContent(
                        result = state.result!!,
                        isFromCache = state.isFromCache,
                        warning = state.error  // رسالة تحذيرية إن وجدت
                    )
                }
            }
        }
    }
}

// ═══ محتوى النتائج ═══
@Composable
fun AnalysisContent(
    result: AnalysisResult,
    isFromCache: Boolean,
    warning: String?
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // تحذير التخزين المؤقت
        if (isFromCache) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            null,
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
        if (result.rating > 0) {
            item { RatingCard(result.rating) }
        }

        // ═══ نظرة عامة ═══
        if (result.overview.isNotBlank()) {
            item { SectionCard("🔍 نظرة عامة", result.overview) }
        }

        // ═══ الغرض ═══
        if (result.purpose.isNotBlank()) {
            item { SectionCard("🎯 الغرض والهدف", result.purpose) }
        }

        // ═══ الميزات ═══
        if (result.features.isNotEmpty()) {
            item {
                BulletCard(
                    title = "⭐ الميزات الرئيسية",
                    items = result.features,
                    accent = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // ═══ التقنيات ═══
        if (result.techStack.isNotEmpty()) {
            item {
                BulletCard(
                    title = "🛠 التقنيات المستخدمة",
                    items = result.techStack,
                    accent = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // ═══ طريقة الاستخدام ═══
        if (result.howToUse.isNotBlank()) {
            item { SectionCard("🚀 كيفية البدء", result.howToUse) }
        }

        // ═══ الأمثلة ═══
        if (result.examples.isNotBlank()) {
            item { CodeSectionCard("💻 أمثلة عملية", result.examples) }
        }

        // ═══ نقاط القوة ═══
        if (result.prosAndCons.pros.isNotEmpty()) {
            item {
                BulletCard(
                    title = "✅ نقاط القوة",
                    items = result.prosAndCons.pros,
                    accent = Color(0xFF4CAF50)
                )
            }
        }

        // ═══ نقاط الضعف ═══
        if (result.prosAndCons.cons.isNotEmpty()) {
            item {
                BulletCard(
                    title = "⚠️ نقاط الضعف",
                    items = result.prosAndCons.cons,
                    accent = Color(0xFFFF9800)
                )
            }
        }

        // ═══ النص الكامل ═══
        if (result.rawMarkdown.isNotBlank()) {
            item { ExpandableFullText(result.rawMarkdown) }
        }

        // مساحة سفلية
        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ═══ بطاقة التقييم ═══
@Composable
fun RatingCard(rating: Float) {
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
                String.format("%.1f", rating),
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                " / 10",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.width(16.dp))
            Text(
                when {
                    rating >= 8 -> "ممتاز 🌟"
                    rating >= 6 -> "جيد 👍"
                    rating >= 4 -> "مقبول"
                    else -> "يحتاج تحسين"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ═══ بطاقة قسم عادي ═══
@Composable
fun SectionCard(title: String, content: String) {
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

// ═══ بطاقة قائمة نقاط ═══
@Composable
fun BulletCard(title: String, items: List<String>, accent: Color) {
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

// ═══ بطاقة كود ═══
@Composable
fun CodeSectionCard(title: String, code: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

// ═══ النص الكامل القابل للتوسيع ═══
@Composable
fun ExpandableFullText(markdown: String) {
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
                Text("📋 النص الكامل", fontWeight = FontWeight.Bold)
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    markdown,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══ شاشة التحميل ═══
@Composable
fun LoadingContent(step: String) {
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
        Text(
            step,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "قد يستغرق هذا بعض الوقت...",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══ شاشة الخطأ ═══
@Composable
fun ErrorContent(
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
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "حدث خطأ",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            error,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("رجوع") }
            Button(onClick = onRetry) { Text("إعادة المحاولة") }
        }
    }
}
