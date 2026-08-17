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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nadr59.sitemanager.data.local.SiteEntity
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import com.nadr59.sitemanager.viewmodel.SortOption
import androidx.compose.foundation.layout.Box

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: SiteViewModel,
    sharedUrl: String?,
    onSharedUrlConsumed: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAddWithUrl: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalysis: (Int, String, String) -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var siteToDelete by remember { mutableStateOf<SiteEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // معالجة الرابط المُشارك
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = "تم استلام رابط",
                actionLabel = "إضافة",
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.ActionPerformed -> onNavigateToAddWithUrl(sharedUrl)
                SnackbarResult.Dismissed -> onSharedUrlConsumed()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("مدير المواقع", fontWeight = FontWeight.Black, fontSize = 22.sp)
                },
                actions = {
                    // المفضلة
                    IconButton(onClick = { viewModel.toggleFavoritesOnly() }) {
                        Icon(
                            if (state.showFavoritesOnly) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (state.showFavoritesOnly)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Dashboard
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(Icons.Default.BarChart, contentDescription = "إحصائيات")
                    }
                    // الفرز
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "فرز")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.label,
                                            fontWeight = if (state.sortOption == option)
                                                FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    // الإعدادات
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة موقع")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // شريط البحث
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("البحث الذكي...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "مسح")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // فلاتر التصنيفات
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allCats = listOf("الكل") + state.categories
                    items(allCats) { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat, fontSize = 13.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // عدد النتائج
            item {
                Text(
                    "${state.filteredSites.size} موقع",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // قائمة المواقع
            items(items = state.filteredSites, key = { it.id }) { site ->
                SiteCardEnhanced(
                    site = site,
                    onVisit = {
                        viewModel.incrementVisit(site.id)
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(site.url))
                            )
                        } catch (_: Exception) {}
                    },
                    onAnalyze = { onNavigateToAnalysis(site.id, site.name, site.url) },
                    onEdit = { onNavigateToEdit(site.id) },
                    onDelete = { siteToDelete = site },
                    onFavorite = { viewModel.toggleFavorite(site.id, site.isFavorite) },
                    onPin = { viewModel.togglePinned(site.id, site.isPinned) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("url", site.url))
                        Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${site.name}\n${site.url}")
                        }
                        context.startActivity(Intent.createChooser(intent, "مشاركة"))
                    },
                    onClick = { onNavigateToDetail(site.id) }
                )
            }

            // حالة فارغة
            if (state.filteredSites.isEmpty() && !state.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("\uD83C\uDF10", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (state.showFavoritesOnly) "لا توجد مفضلات"
                            else if (state.searchQuery.isNotBlank()) "لا توجد نتائج"
                            else "لا توجد مواقع",
                            fontWeight = FontWeight.Bold, fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }

    // حوار تأكيد الحذف
    siteToDelete?.let { site ->
        AlertDialog(
            onDismissRequest = { siteToDelete = null },
            title = { Text("حذف الموقع") },
            text = { Text("هل أنت متأكد من حذف \"${site.name}\"؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSite(site)
                    siteToDelete = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { siteToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// بطاقة الموقع المحسّنة
// ═══════════════════════════════════════════════════════════
@Composable
fun SiteCardEnhanced(
    site: SiteEntity,
    onVisit: () -> Unit,
    onAnalyze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onPin: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // الصف الأول
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (site.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            site.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        site.url,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    // المفضلة
                    IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (site.isFavorite) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (site.isFavorite) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // القائمة
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تثبيت") },
                                leadingIcon = { Icon(Icons.Default.PushPin, null, Modifier.size(18.dp)) },
                                onClick = { onPin(); showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("نسخ الرابط") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp)) },
                                onClick = { onCopy(); showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("مشاركة") },
                                leadingIcon = { Icon(Icons.Default.Share, null, Modifier.size(18.dp)) },
                                onClick = { onShare(); showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("تعديل") },
                                leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) },
                                onClick = { onEdit(); showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                                onClick = { onDelete(); showMenu = false }
                            )
                        }
                    }
                }
            }

            // الوسوم
            if (site.tags.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    site.tags.split(",").filter { it.isNotBlank() }.take(5).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                tag.trim(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // معاينة التحليل
            if (site.cachedOverview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Text(
                            site.cachedOverview.take(120) +
                                if (site.cachedOverview.length > 120) "..." else "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                        if (site.aiRating > 0f) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    " ${String.format("%.0f", site.aiRating)}/10 ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // أزرار الإجراءات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // زيارة
                Surface(
                    onClick = onVisit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Language, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("زيارة", fontSize = 13.sp)
                    }
                }

                // شرح
                Surface(
                    onClick = onAnalyze,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("شرح", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
