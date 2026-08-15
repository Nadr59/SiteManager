package com.example.sitemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitemanager.data.local.SiteEntity
import com.example.sitemanager.ui.components.AddSiteBottomSheet
import com.example.sitemanager.ui.components.EditNameBottomSheet
import com.example.sitemanager.ui.components.SiteCard
import com.example.sitemanager.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SiteViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val savedSites by viewModel.savedSites.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ═══ تأكيد الحذف ═══
    var showDeleteDialog by remember { mutableStateOf<SiteEntity?>(null) }

    // ═══ القائمة المنبثقة (Long Press) ═══
    var showContextMenu by remember { mutableStateOf<SiteEntity?>(null) }

    // ═══ رسالة ═══
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSearching) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("ابحث عن موقع...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text(
                            text = "مدير المواقع",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onSearchToggle() }) {
                        Icon(
                            if (uiState.isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "بحث"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ═══ عرض نتائج البحث ═══
            if (uiState.isSearching && uiState.searchQuery.isNotBlank()) {
                SearchResultsList(
                    results = searchResults,
                    onSiteClick = { viewModel.onSiteOpened(it) },
                    onEdit = { viewModel.onEditSite(it) },
                    onMove = { viewModel.onMoveToTab(it) },
                    onDelete = { showDeleteDialog = it },
                    onLongClick = { showContextMenu = it }
                )
            } else {
                // ═══ التبويبات ═══
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.onTabSelected(0) },
                        text = {
                            Text("الأكثر استخداماً (${favorites.size})")
                        },
                        icon = { Icon(Icons.Default.Favorite, null) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.onTabSelected(1) },
                        text = {
                            Text("المحفوظات (${savedSites.size})")
                        },
                        icon = { Icon(Icons.Default.Bookmark, null) }
                    )
                }

                // ═══ محتوى التبويب ═══
                val currentList = if (uiState.selectedTab == 0) favorites else savedSites

                if (currentList.isEmpty()) {
                    EmptyState(
                        if (uiState.selectedTab == 0)
                            "لا توجد مواقع في الأكثر استخداماً"
                        else
                            "لا توجد مواقع محفوظة"
                    )
                } else {
                    SiteList(
                        sites = currentList,
                        onSiteClick = { viewModel.onSiteOpened(it) },
                        onEdit = { viewModel.onEditSite(it) },
                        onMove = { viewModel.onMoveToTab(it) },
                        onDelete = { showDeleteDialog = it },
                        onLongClick = { showContextMenu = it }
                    )
                }
            }
        }
    }

    // ═══ في دالة HomeScreen، أضف المعامل الجديد ═══
@Composable
fun HomeScreen(
    viewModel: SiteViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalysis: (Int, String, String) -> Unit  // ← جديد: (siteId, name, url)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { /* ... كما هو ... */ },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Filled.Add, "إضافة موقع")
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
            // ... شريط البحث والفلاتر كما هو ...

            // ═══ قائمة المواقع — مع زر التحليل ═══
            items(
                items = state.filteredSites,
                key = { it.id }
            ) { site ->
                SiteCard(
                    site = site,
                    onVisit = { viewModel.incrementVisit(site.id) },
                    onAnalyze = {  // ← جديد
                        onNavigateToAnalysis(site.id, site.name, site.url)
                    },
                    onEdit = { /* ... */ },
                    onDelete = { viewModel.deleteSite(site) }
                )
            }
        }
    }
}
    // ═══ Bottom Sheet: إضافة موقع (من المشاركة) ═══
    if (uiState.showAddSheet && uiState.sharedUrl != null) {
        AddSiteBottomSheet(
            url = uiState.sharedUrl!!,
            title = uiState.sharedTitle ?: "",
            onAddToFavorites = { title ->
                viewModel.addSite("favorites", title)
            },
            onAddToSaved = { title ->
                viewModel.addSite("saved", title)
            },
            onDismiss = { viewModel.onAddSheetDismissed() }
        )
    }

    // ═══ Bottom Sheet: تعديل الاسم ═══
    if (uiState.editingSite != null) {
        EditNameBottomSheet(
            site = uiState.editingSite!!,
            onSave = { viewModel.onTitleUpdated(it) },
            onDismiss = { viewModel.onEditDismissed() }
        )
    }

    // ═══ مربع حوار تأكيد الحذف ═══
    showDeleteDialog?.let { site ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("حذف الموقع", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف \"${site.title}\"؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteSite(site)
                    showDeleteDialog = null
                }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ═══ قائمة منبثقة (Long Press) ═══
    showContextMenu?.let { site ->
        AlertDialog(
            onDismissRequest = { showContextMenu = null },
            title = { Text(site.title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ContextMenuItem("🌐 فتح في المتصفح") {
                        viewModel.onSiteOpened(site)
                        showContextMenu = null
                    }
                    ContextMenuItem("✏️ تعديل الاسم") {
                        viewModel.onEditSite(site)
                        showContextMenu = null
                    }
                    ContextMenuItem(
                        if (site.tabType == "favorites") "↔️ نقل إلى المحفوظات"
                        else "↔️ نقل إلى الأكثر استخداماً"
                    ) {
                        viewModel.onMoveToTab(site)
                        showContextMenu = null
                    }
                    ContextMenuItem("🗑️ حذف") {
                        showDeleteDialog = site
                        showContextMenu = null
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showContextMenu = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun SiteList(
    sites: List<SiteEntity>,
    onSiteClick: (SiteEntity) -> Unit,
    onEdit: (SiteEntity) -> Unit,
    onMove: (SiteEntity) -> Unit,
    onDelete: (SiteEntity) -> Unit,
    onLongClick: (SiteEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = sites, key = { it.id }) { site ->
            SiteCard(
                site = site,
                onOpen = { onSiteClick(site) },
                onEdit = { onEdit(site) },
                onMove = { onMove(site) },
                onDelete = { onDelete(site) },
                onClick = { onSiteClick(site) },
                onLongClick = { onLongClick(site) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SiteEntity>,
    onSiteClick: (SiteEntity) -> Unit,
    onEdit: (SiteEntity) -> Unit,
    onMove: (SiteEntity) -> Unit,
    onDelete: (SiteEntity) -> Unit,
    onLongClick: (SiteEntity) -> Unit
) {
    if (results.isEmpty()) {
        EmptyState("لا توجد نتائج")
    } else {
        SiteList(results, onSiteClick, onEdit, onMove, onDelete, onLongClick)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📂", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "شارك رابطاً من المتصفح لإضافته هنا",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ContextMenuItem(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, modifier = Modifier.fillMaxWidth())
    }
}
