package com.nadr59.sitemanager.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadr59.sitemanager.data.local.AnalysisType
import com.nadr59.sitemanager.viewmodel.SiteViewModel


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.nadr59.sitemanager.utils.formatDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SiteDetailScreen(
    siteId: Int,
    viewModel: SiteViewModel,
    onBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToAnalysis: (Int, String, String, String) -> Unit
) {
    val site by viewModel.getSiteById(siteId).collectAsState(initial = null)
    val analyses by viewModel.getAnalysesForSite(siteId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentSite = site

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        currentSite?.name ?: "تفاصيل الموقع",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    currentSite?.let { s ->
                        IconButton(onClick = {
                            viewModel.toggleFavorite(s.id, s.isFavorite)
                        }) {
                            Icon(
                                if (s.isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                "مفضلة",
                                tint = if (s.isFavorite) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            viewModel.togglePinned(s.id, s.isPinned)
                        }) {
                            Icon(
                                Icons.Default.PushPin,
                                "تثبيت",
                                tint = if (s.isPinned) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (currentSite == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("جارٍ التحميل...")
            }
        } else {
            val s = currentSite!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ═══ بطاقة المعلومات الرئيسية ═══
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(s.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                s.url,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            if (s.pageTitle.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    s.pageTitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // ═══ معلومات تفصيلية ═══
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DetailRow("التصنيف", s.category)
                            if (s.tags.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text("الوسوم", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    s.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                tag.trim(),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                            if (s.notes.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                DetailRow("ملاحظات", s.notes)
                            }
                            DetailRow("تاريخ الإضافة", formatDate(s.createdAt))
                            if (s.lastVisited > 0) DetailRow("آخر زيارة", formatDate(s.lastVisited))
                            DetailRow("عدد الزيارات", "${s.visitCount}")
                            if (s.httpStatus > 0) DetailRow("حالة HTTP", "${s.httpStatus}")
                            if (s.lastChecked > 0) DetailRow("آخر فحص", formatDate(s.lastChecked))
                        }
                    }
                }

                // ═══ أزرار الإجراءات ═══
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton("زيارة", Icons.Default.Language, Modifier.weight(1f)) {
                            viewModel.incrementVisit(s.id)
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.url)))
                            } catch (_: Exception) {}
                        }
                        ActionButton("تعديل", Icons.Default.Edit, Modifier.weight(1f)) {
                            onNavigateToEdit(s.id)
                        }
                        ActionButton("نسخ", Icons.Default.ContentCopy, Modifier.weight(1f)) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("url", s.url))
                            Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                        }
                        ActionButton("مشاركة", Icons.Default.Share, Modifier.weight(1f)) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${s.name}\n${s.url}")
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة"))
                        }
                    }
                }

                // ═══ أزرار التحليل ═══
                item {
                    Text(
                        "التحليل بالذكاء الاصطناعي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalysisType.entries.forEach { type ->
                            Surface(
                                onClick = {
                                    onNavigateToAnalysis(s.id, s.name, s.url, type.key)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(type.emoji, fontSize = 16.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(type.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // ═══ سجل التحليلات ═══
                if (analyses.isNotEmpty()) {
                    item {
                        Text(
                            "سجل التحليلات (${analyses.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(items = analyses.take(5)) { analysis ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val type = AnalysisType.fromKey(analysis.analysisType)
                                    Text(
                                        "${type.emoji} ${type.displayName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        formatDate(analysis.createdAt),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    analysis.result.take(200) +
                                        if (analysis.result.length > 200) "..." else "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // ═══ زر حذف ═══
                item {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "حذف الموقع",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    if (showDeleteDialog && currentSite != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف الموقع") },
            text = { Text("هل أنت متأكد من حذف \"${currentSite!!.name}\"؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSite(currentSite!!)
                    showDeleteDialog = false
                    onBack()
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(100.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, fontSize = 13.sp)
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp)
        }
    }
}
