package com.nadr59.sitemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nadr59.sitemanager.ui.theme.AppTheme
import com.nadr59.sitemanager.viewmodel.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val themeVm: ThemeViewModel = hiltViewModel()
    val currentTheme by themeVm.currentTheme.collectAsState()
    val isDarkMode by themeVm.isDarkMode.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var serviceStatus by remember { mutableStateOf("جارٍ التحقق...") }
    var isOnline by remember { mutableStateOf(false) }
    var activeKeys by remember { mutableStateOf(0) }
    var totalApps by remember { mutableStateOf(0) }
    var providers by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadStatus() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()
                    val request = Request.Builder()
                        .url("https://ai-key-manager.vercel.app/api/ask")
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    response.body?.string() ?: "{}"
                }

                val json = Gson().fromJson(result, JsonObject::class.java)
                isOnline = json.get("status")?.asString == "online"
                serviceStatus = if (isOnline) "متصلة وتعمل" else "غير متصلة"
                activeKeys = json.get("activeKeys")?.asInt ?: 0
                totalApps = json.get("registeredApps")?.asInt ?: 0

                val keysArray = json.getAsJsonArray("keys")
                providers = keysArray?.mapNotNull {
                    val key = it.asJsonObject
                    if (key.get("active")?.asBoolean == true) {
                        "${key.get("provider")?.asString} (${key.get("model")?.asString})"
                    } else null
                } ?: emptyList()

                isLoading = false
            } catch (e: Exception) {
                isOnline = false
                serviceStatus = "فشل الاتصال"
                errorMessage = e.message
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { loadStatus() }) {
                        Icon(Icons.Default.Refresh, "تحديث")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ═══ قسم الثيم ═══
            ThemeSection(
                currentTheme = currentTheme,
                isDarkMode = isDarkMode,
                onThemeSelected = { themeVm.setTheme(it) },
                onToggleDarkMode = { themeVm.toggleDarkMode() }
            )

            // ═══ حالة الخدمة ═══
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnline)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isOnline) Icons.Default.Cloud else Icons.Default.Error,
                        null,
                        tint = if (isOnline) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("خدمة الذكاء الاصطناعي", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(serviceStatus, fontSize = 13.sp)
                        }
                    }
                    Icon(
                        if (isOnline) Icons.Default.CheckCircle else Icons.Default.Error,
                        null,
                        tint = if (isOnline) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }

            // ═══ معلومات الخدمة ═══
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("معلومات الخدمة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    InfoRow("الحالة", if (isOnline) "نشطة" else "غير متصلة")
                    InfoRow("المفاتيح النشطة", "$activeKeys")
                    InfoRow("التطبيقات المسجلة", "$totalApps")
                    if (providers.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "المزودات النشطة:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        providers.forEach { provider ->
                            Text("  • $provider", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ═══ خطأ ═══
            if (errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "خطأ في الاتصال",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            errorMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // ═══ الأمان ═══
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, Modifier.padding(end = 8.dp))
                        Text("الأمان والخصوصية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "• لا يتم تخزين أي مفاتيح API في التطبيق\n" +
                        "• يتم إرسال رابط الموقع فقط للتحليل\n" +
                        "• المحتوى يُرسل آمناً عبر HTTPS\n" +
                        "• لا يتم مشاركة بياناتك مع أي طرف",
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // ═══ معلومات التطبيق ═══
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, Modifier.padding(end = 8.dp))
                        Text("عن التطبيق", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoRow("الإصدار", "2.1.0")
                    InfoRow("المطور", "Nadr59")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSection(
    currentTheme: AppTheme,
    isDarkMode: Boolean,
    onThemeSelected: (AppTheme) -> Unit,
    onToggleDarkMode: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎨", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "المظهر والثيم",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // ═══ الوضع الداكن ═══
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الوضع الداكن", fontSize = 14.sp)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() }
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(
                "لون الثيم",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(8.dp))

            // ═══ اختيار الثيم ═══
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = currentTheme == theme,
                        onClick = { onThemeSelected(theme) },
                        label = {
                            Text(
                                "${theme.emoji} ${theme.displayName}",
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(value, fontSize = 13.sp)
    }
}
