package com.nadr59.sitemanager.ui.screens

import android.webkit.URLUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadr59.sitemanager.data.local.SiteEntity
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSiteScreen(
    viewModel: SiteViewModel,
    initialUrl: String? = null,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf(initialUrl ?: "") }
    var category by remember { mutableStateOf("عام") }
    var notes by remember { mutableStateOf("") }

    // ═══ استخراج اسم تلقائي من الرابط ═══
    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            url = initialUrl
            // استخراج اسم من الرابط
            name = try {
                val host = java.net.URL(
                    if (initialUrl.startsWith("http")) initialUrl
                    else "https://$initialUrl"
                ).host
                    .removePrefix("www.")
                    .split(".")
                    .first()
                    .replaceFirstChar { it.uppercase() }
                host
            } catch (_: Exception) {
                ""
            }
        }
    }

    val categories = listOf("عام", "أداة", "مكتبة", "توثيق", "تعليم", "إخباري", "أخرى")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إضافة موقع", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ═══ إشعار المشاركة ═══
            if (!initialUrl.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "تم استلام رابط من المشاركة — يمكنك تعديل البيانات قبل الحفظ",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم الموقع") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("رابط الموقع") },
                placeholder = { Text("https://...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Column {
                Text(
                    "التصنيف",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 13.sp) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات (اختياري)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val finalUrl = if (url.isNotBlank() && !url.startsWith("http")) {
                        "https://$url"
                    } else url

                    if (name.isNotBlank() && finalUrl.isNotBlank()) {
                        viewModel.addSite(
                            SiteEntity(
                                name = name.trim(),
                                url = finalUrl.trim(),
                                category = category,
                                notes = notes.trim()
                            )
                        )
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = name.isNotBlank() && url.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("حفظ الموقع", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
