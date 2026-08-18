package com.nadr59.sitemanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadr59.sitemanager.data.local.AnalysisType
import com.nadr59.sitemanager.data.remote.AnalysisResult
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalysisScreen(
    siteId: Int,
    siteName: String,
    siteUrl: String,
    analysisTypeKey: String = "explain",
    onBack: () -> Unit,
    viewModel: SiteViewModel
) {
    val analysisType = AnalysisType.fromKey(analysisTypeKey)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AnalysisResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var customQuestion by remember { mutableStateOf("") }
    var hasStarted by remember { mutableStateOf(false) }

    fun startAnalysis(question: String = "") {
        scope.launch {
            isLoading = true
            error = null
            hasStarted = true

            val response = viewModel.analyzerRepository.analyze(
                siteId = siteId,
                analysisType = analysisType,
                customQuestion = question
            )

            response.fold(
                onSuccess = { analysisResult ->
                    result = analysisResult
                    isLoading = false
                },
                onFailure = { e ->
                    error = e.message ?: "حدث خطأ غير متوقع"
                    isLoading = false
                }
            )
        }
    }

    LaunchedEffect(siteId, analysisTypeKey) {
        val cached = viewModel.analyzerRepository.getCachedAnalysis(siteId, analysisType)
        if (cached != null) {
            result = cached
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            analysisType.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            siteName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(siteUrl))
                                )
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "فتح الموقع")
                    }
                    if (result != null) {
                        IconButton(onClick = { startAnalysis(customQuestion) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "إعادة التحليل")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // رأس نوع التحليل
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
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(analysisType.emoji, fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                analysisType.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                siteUrl,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // حقل السؤال المخصص
            if (analysisType == AnalysisType.CUSTOM) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("اسأل أي سؤال عن هذا الموقع", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customQuestion,
                                onValueChange = { customQuestion = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("مثال: هل هذا الموقع مناسب للأطفال؟") },
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { if (customQuestion.isNotBlank()) startAnalysis(customQuestion) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = customQuestion.isNotBlank() && !isLoading,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("إرسال السؤال")
                            }
                        }
                    }
                }
            }

            // زر بدء التحليل
            if (analysisType != AnalysisType.CUSTOM && result == null && !isLoading && !hasStarted) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(analysisType.emoji, fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(analysisType.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("اضغط الزر أدناه لبدء تحليل الموقع", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { startAnalysis() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("بدء التحليل", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // حالة التحميل
            if (isLoading) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
                            Spacer(Modifier.height(16.dp))
                            Text("جارٍ تحليل الموقع...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("قد يستغرق هذا بضع ثوانٍ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }

            // رسالة الخطأ
            if (error != null && !isLoading) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Error, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.height(12.dp))
                            Text("خطأ في التحليل", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.height(8.dp))
                            Text(error ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { startAnalysis(customQuestion) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
            }

            // عرض النتائج
            result?.let { r ->
                if (r.rating > 0f) { item { RatingCard(rating = r.rating) } }
                if (r.overview.isNotBlank()) { item { AnalysisSection("النظرة العامة", "📋", r.overview) } }
                if (r.purpose.isNotBlank()) { item { AnalysisSection("الغرض من الموقع", "🎯", r.purpose) } }
                if (r.features.isNotEmpty()) { item { AnalysisListSection("الميزات والخصائص", "⭐", r.features) } }
                if (r.techStack.isNotEmpty()) { item { AnalysisListSection("التقنيات المستخدمة", "⚙️", r.techStack) } }
                if (r.howToUse.isNotBlank()) { item { AnalysisSection("كيفية الاستخدام", "📖", r.howToUse) } }
                if (r.examples.isNotBlank()) { item { AnalysisSection("أمثلة", "💡", r.examples) } }
                if (r.prosAndCons.pros.isNotEmpty()) { item { ProsConsCard("الإيجابيات", "✅", r.prosAndCons.pros, true) } }
                if (r.prosAndCons.cons.isNotEmpty()) { item { ProsConsCard("السلبيات", "⚠️", r.prosAndCons.cons, false) } }
                if (r.rawMarkdown.isNotBlank() && r.overview.isBlank() && r.purpose.isBlank()) {
                    item { AnalysisSection("نتيجة التحليل", analysisType.emoji, r.rawMarkdown) }
                }

                // أزرار
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { startAnalysis(customQuestion) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إعادة التحليل")
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "تحليل ${analysisType.displayName} لموقع: $siteName\n$siteUrl\n\n${r.rawMarkdown}")
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة التحليل"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("مشاركة") }
                    }
                }

                // تحليلات أخرى
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("تحليلات أخرى", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnalysisType.entries.filter { it != analysisType }.forEach { type ->
                            Surface(onClick = { startAnalysis(if (type == AnalysisType.CUSTOM) customQuestion else "") }, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(type.emoji, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(type.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun RatingCard(rating: Float) {
    val color = when { rating >= 8f -> MaterialTheme.colorScheme.primary; rating >= 5f -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.error }
    val label = when { rating >= 8f -> "ممتاز"; rating >= 6f -> "جيد"; rating >= 4f -> "مقبول"; else -> "ضعيف" }
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(String.format("%.1f", rating), fontWeight = FontWeight.Black, fontSize = 32.sp, color = color)
            Text(" / 10", fontWeight = FontWeight.Medium, fontSize = 18.sp, color = color.copy(alpha = 0.6f))
            Spacer(Modifier.width(12.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
            }
        }
    }
}

@Composable
private fun AnalysisSection(title: String, emoji: String, content: String) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(emoji, fontSize = 18.sp); Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            Spacer(Modifier.height(10.dp))
            Text(content, fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun AnalysisListSection(title: String, emoji: String, items: List<String>) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(emoji, fontSize = 18.sp); Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            Spacer(Modifier.height(10.dp))
            items.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.padding(top = 2.dp)) {
                        Text("${index + 1}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(item, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun ProsConsCard(title: String, emoji: String, items: List<String>, isPositive: Boolean) {
    val accentColor = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(emoji, fontSize = 18.sp); Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accentColor) }
            Spacer(Modifier.height(10.dp))
            items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(if (isPositive) "+" else "-", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor, modifier = Modifier.width(20.dp))
                    Text(item, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                }
            }
        }
    }
}
