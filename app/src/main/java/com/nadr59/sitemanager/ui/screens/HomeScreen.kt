package com.nadr59.sitemanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadr59.sitemanager.data.local.SiteEntity
import com.nadr59.sitemanager.viewmodel.SiteViewModel
import com.nadr59.sitemanager.viewmodel.SortOption
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun HomeScreen(
    viewModel: SiteViewModel,
    sharedUrl: String = "",
    onSharedUrlConsumed: () -> Unit = {},
    onNavigateToAdd: () -> Unit = {},
    onNavigateToAddWithUrl: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToAnalysis: (Int, String, String) -> Unit = { _, _, _ -> },
    onNavigateToEdit: (Int) -> Unit = {},
    onNavigateToDetail: (Int) -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isGridView by remember { mutableStateOf(false) }

    LaunchedEffect(sharedUrl) {
        if (sharedUrl.isNotBlank()) {
            onNavigateToAddWithUrl(sharedUrl)
            onSharedUrlConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "S",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Site Manager",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                lineHeight = 20.sp
                            )
                            Text(
                                "${uiState.filteredSites.size} موقع",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 13.sp
                            )
                        }
                    }
                },
                actions = {
                    // ═══ تبديل العرض ═══
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList
                            else Icons.Default.GridView,
                            contentDescription = "تبديل العرض"
                        )
                    }

                    // ═══ الفرز ═══
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, "فرز")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (uiState.sortOption == option) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(8.dp)
                                                ) {}
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text(
                                                option.label,
                                                fontWeight = if (uiState.sortOption == option)
                                                    FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // ═══ المفضلة ═══
                    IconButton(onClick = { viewModel.toggleFavoritesOnly() }) {
                        Icon(
                            if (uiState.showFavoritesOnly) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            "المفضلة",
                            tint = if (uiState.showFavoritesOnly)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // ═══ الإعدادات ═══
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "الإعدادات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.filteredSites.isNotEmpty()) {
                                    Badge {
                                        Text(
                                            "${uiState.filteredSites.size}",
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Category, null)
                        }
                    },
                    label = { Text("المواقع") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onNavigateToDashboard()
                    },
                    icon = { Icon(Icons.Default.Dashboard, null) },
                    label = { Text("لوحة التحكم") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        onNavigateToExport()
                    },
                    icon = { Icon(Icons.Default.Category, null) },
                    label = { Text("تصدير") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    "إضافة",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ═══ شريط البحث ═══
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("ابحث في مواقعك...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // ═══ التصنيفات ═══
            AnimatedVisibility(visible = uiState.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    val allCats = listOf("الكل") + uiState.categories
                    items(allCats) { cat ->
                        val selected = uiState.selectedCategory == cat
                        Surface(
                            onClick = { viewModel.selectCategory(cat) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ),
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ═══ المحتوى ═══
            AnimatedContent(
                targetState = uiState.isLoading,
                modifier = Modifier.fillMaxSize()
            ) { loading ->
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("جارٍ التحميل...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (uiState.filteredSites.isEmpty()) {
                    EmptyState(
                        showFavoritesOnly = uiState.showFavoritesOnly,
                        hasSearch = uiState.searchQuery.isNotBlank(),
                        onAdd = onNavigateToAdd
                    )
                } else {
                    AnimatedContent(targetState = isGridView) { grid ->
                        if (grid) {
                            // ═══ عرض الشبكة ═══
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.filteredSites,
                                    key = { it.id }
                                ) { site ->
                                    SiteGridCard(
                                        site = site,
                                        onNavigateToDetail = onNavigateToDetail,
                                        onNavigateToAnalysis = onNavigateToAnalysis,
                                        onToggleFavorite = {
                                            viewModel.toggleFavorite(site.id, site.isFavorite)
                                        },
                                        onDelete = { viewModel.deleteSite(site) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        } else {
                            // ═══ عرض القائمة ═══
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.filteredSites,
                                    key = { it.id }
                                ) { site ->
                                    SiteListCard(
                                        site = site,
                                        onNavigateToDetail = onNavigateToDetail,
                                        onNavigateToEdit = onNavigateToEdit,
                                        onNavigateToAnalysis = onNavigateToAnalysis,
                                        onToggleFavorite = {
                                            viewModel.toggleFavorite(site.id, site.isFavorite)
                                        },
                                        onTogglePinned = {
                                            viewModel.togglePinned(site.id, site.isPinned)
                                        },
                                        onDelete = { viewModel.deleteSite(site) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// بطاقة القائمة
// ═══════════════════════════════════════════════
@Composable
fun SiteListCard(
    site: SiteEntity,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToAnalysis: (Int, String, String) -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePinned: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh)
    )

    Card(
        onClick = {
            isPressed = true
            onNavigateToDetail(site.id)
        },
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (site.isPinned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (site.isPinned) 4.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ═══ أيقونة الموقع ═══
                SiteFavicon(
                    name = site.name,
                    url = site.url,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (site.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                null,
                                Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            site.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        site.url
                            .removePrefix("https://")
                            .removePrefix("http://")
                            .removePrefix("www."),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (site.aiRating > 0f) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 10.sp)
                            Text(
                                " ${site.aiRating}/10",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (site.isFavorite) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                        null,
                        Modifier.size(18.dp),
                        tint = if (site.isFavorite)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ═══ الوصف ═══
            if (site.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    site.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            // ═══ التصنيف والوسوم ═══
            if (site.category.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            site.category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (site.visitCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                "👁 ${site.visitCount}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // ═══ أزرار الإجراءات ═══
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionIconButton(
                        icon = Icons.Default.Language,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = { onNavigateToDetail(site.id) }
                    )
                    ActionIconButton(
                        icon = Icons.Default.OpenInNew,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(site.url))
                                )
                            } catch (_: Exception) {}
                        }
                    )
                    ActionIconButton(
                        icon = Icons.Default.Analytics,
                        tint = MaterialTheme.colorScheme.tertiary,
                        onClick = { onNavigateToAnalysis(site.id, site.name, site.url) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionIconButton(
                        icon = Icons.Default.Edit,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onNavigateToEdit(site.id) }
                    )
                    ActionIconButton(
                        icon = Icons.Default.PushPin,
                        tint = if (site.isPinned)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onTogglePinned
                    )
                    ActionIconButton(
                        icon = Icons.Default.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// بطاقة الشبكة
// ═══════════════════════════════════════════════
@Composable
fun SiteGridCard(
    site: SiteEntity,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAnalysis: (Int, String, String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onNavigateToDetail(site.id) },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ═══ المفضلة ═══
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (site.isFavorite) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                        null,
                        Modifier.size(14.dp),
                        tint = if (site.isFavorite)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ═══ أيقونة الموقع ═══
            SiteFavicon(
                name = site.name,
                url = site.url,
                modifier = Modifier.size(56.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                site.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                site.url
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .take(20),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            if (site.category.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        site.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ═══ أزرار سريعة ═══
            // ═══ أزرار سريعة في SiteGridCard - استبدل الكود القديم بهذا ═══
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    Surface(
        onClick = { onNavigateToDetail(site.id) },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.weight(1f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Language,
                null,
                Modifier
                    .padding(8.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    Surface(
        onClick = { onNavigateToAnalysis(site.id, site.name, site.url) },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.weight(1f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Analytics,
                null,
                Modifier
                    .padding(8.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
    Surface(
        onClick = onDelete,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.weight(1f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Delete,
                null,
                Modifier
                    .padding(8.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ═══════════════════════════════════════════════
// مكونات مساعدة
// ═══════════════════════════════════════════════
@Composable
fun SiteFavicon(
    name: String,
    url: String,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A),
        Color(0xFFE65100), Color(0xFFC62828), Color(0xFF00695C),
        Color(0xFF0277BD), Color(0xFF558B2F)
    )
    val color = colors[name.length % colors.size]
    val letter = name.firstOrNull()?.uppercase() ?: "?"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = letter,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = when {
                    modifier == Modifier -> 20.sp
                    else -> 22.sp
                }
            )
        }
    }
}

@Composable
fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            icon,
            null,
            Modifier.size(18.dp),
            tint = tint
        )
    }
}

@Composable
fun EmptyState(
    showFavoritesOnly: Boolean,
    hasSearch: Boolean,
    onAdd: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                when {
                    showFavoritesOnly -> "⭐"
                    hasSearch -> "🔍"
                    else -> "🌐"
                },
                fontSize = 64.sp
            )
            Text(
                when {
                    showFavoritesOnly -> "لا توجد مفضلات"
                    hasSearch -> "لا توجد نتائج"
                    else -> "لا توجد مواقع بعد"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                when {
                    showFavoritesOnly -> "أضف مواقع للمفضلة"
                    hasSearch -> "جرب كلمة بحث أخرى"
                    else -> "اضغط + لإضافة موقعك الأول"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            if (!showFavoritesOnly && !hasSearch) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = onAdd,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "إضافة موقع",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
