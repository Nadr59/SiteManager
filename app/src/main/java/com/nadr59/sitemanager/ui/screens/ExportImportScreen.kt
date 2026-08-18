package com.nadr59.sitemanager.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadr59.sitemanager.data.local.SiteEntity
import com.nadr59.sitemanager.utils.ExportImportUtils
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(
    viewModel: SiteViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ═══ استيراد JSON ═══
    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val content = ExportImportUtils.readFromUri(context, it)
            if (content != null) {
                try {
                    val sites = ExportImportUtils.importFromJson(content)
                    sites.forEach { exportSite ->
                        val finalUrl = if (exportSite.url.startsWith("http")) exportSite.url else "https://${exportSite.url}"
                        viewModel.addSite(
                            SiteEntity(
                                name = exportSite.name,
                                url = finalUrl,
                                category = exportSite.category,
                                tags = exportSite.tags,
                                notes = exportSite.notes,
                                isFavorite = exportSite.isFavorite,
                                isPinned = exportSite.isPinned,
                                createdAt = exportSite.createdAt
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // ═══ استيراد CSV ═══
    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val content = ExportImportUtils.readFromUri(context, it)
            if (content != null) {
                try {
                    val sites = ExportImportUtils.importFromCsv(content)
                    sites.forEach { exportSite ->
                        val finalUrl = if (exportSite.url.startsWith("http")) exportSite.url else "https://${exportSite.url}"
                        viewModel.addSite(
                            SiteEntity(
                                name = exportSite.name,
                                url = finalUrl,
                                category = exportSite.category,
                                tags = exportSite.tags,
                                notes = exportSite.notes,
                                isFavorite = exportSite.isFavorite,
                                isPinned = exportSite.isPinned,
                                createdAt = exportSite.createdAt
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تصدير واستيراد", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══ تصدير ═══
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("📤 تصدير البيانات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${state.allSites.size} موقع محفوظ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))

                    // تصدير JSON
                    Surface(
                        onClick = {
                            val json = ExportImportUtils.exportToJson(state.allSites)
                            val uri = ExportImportUtils.saveToFile(context, json, "sites_export.json", "application/json")
                            uri?.let {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, it)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "تصدير JSON"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, null, Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("تصدير JSON", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("ملف واحد يحتوي كل البيانات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // تصدير CSV
                    Surface(
                        onClick = {
                            val csv = ExportImportUtils.exportToCsv(state.allSites)
                            val uri = ExportImportUtils.saveToFile(context, csv, "sites_export.csv", "text/csv")
                            uri?.let {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, it)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "تصدير CSV"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, null, Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("تصدير CSV", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("ملف يُفتح في Excel أو Google Sheets", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            // ═══ استيراد ═══
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("📥 استيراد البيانات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("استيراد من ملف محفوظ سابقاً", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))

                    // استيراد JSON
                    Surface(
                        onClick = { importJsonLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Upload, null, Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("استيراد JSON", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("من ملف تصدير سابق", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // استيراد CSV
                    Surface(
                        onClick = { importCsvLauncher.launch("text/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Upload, null, Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("استيراد CSV", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("من ملف Excel أو Sheets", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
