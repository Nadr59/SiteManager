package com.nadr59.sitemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nadr59.sitemanager.data.local.AnalysisType
import com.nadr59.sitemanager.data.local.CategoryCount
import com.nadr59.sitemanager.data.local.SiteAnalysisEntity
import com.nadr59.sitemanager.data.local.SiteDatabase
import com.nadr59.sitemanager.data.local.SiteEntity
import com.nadr59.sitemanager.data.remote.AiConfig
import com.nadr59.sitemanager.data.remote.AiService
import com.nadr59.sitemanager.data.remote.AnalysisResult
import com.nadr59.sitemanager.data.remote.WebScraper
import com.nadr59.sitemanager.data.repository.AnalyzerRepository
import com.nadr59.sitemanager.data.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption(val label: String) {
    NEWEST("الأحدث"),
    OLDEST("الأقدم"),
    NAME("الاسم"),
    CATEGORY("التصنيف"),
    MOST_USED("الأكثر استخداماً"),
    LAST_OPENED("آخر فتح")
}

data class HomeUiState(
    val allSites: List<SiteEntity> = emptyList(),
    val filteredSites: List<SiteEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "الكل",
    val sortOption: SortOption = SortOption.NEWEST,
    val showFavoritesOnly: Boolean = false,
    val isLoading: Boolean = false
)

data class DashboardStats(
    val totalCount: Int = 0,
    val favoriteCount: Int = 0,
    val categoryCount: Int = 0,
    val visitedCount: Int = 0,
    val analyzedCount: Int = 0,
    val topCategories: List<CategoryCount> = emptyList(),
    val topVisited: List<SiteEntity> = emptyList()
)

@HiltViewModel
class SiteViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val database = SiteDatabase.getDatabase(application)
    private val dao = database.siteDao()
    private val repository = SiteRepository(dao)
    private val scraper = WebScraper()
    private val aiService = AiService(Gson())
    val analyzerRepository = AnalyzerRepository(scraper, aiService, dao)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("الكل")
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    private val _showFavoritesOnly = MutableStateFlow(false)

    // ═══ combine بأبعاد منفصلة وواضحة ═══
    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllSites(),
        _searchQuery,
        _selectedCategory,
        _sortOption,
        _showFavoritesOnly
    ) { allSites: List<SiteEntity>,
        query: String,
        category: String,
        sort: SortOption,
        favOnly: Boolean ->

        val filtered = applyFilters(allSites, query, category, favOnly, sort)
        val cats = allSites.map { it.category }.distinct().sorted()

        HomeUiState(
            allSites = allSites,
            filteredSites = filtered,
            categories = cats,
            searchQuery = query,
            selectedCategory = category,
            sortOption = sort,
            showFavoritesOnly = favOnly
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    // ═══ Dashboard combine — كل flow على حدة ═══
    val dashboardStats: StateFlow<DashboardStats> = combine(
        repository.getTotalCount(),
        repository.getFavoriteCount(),
        repository.getCategoryCount()
    ) { total: Int, favs: Int, cats: Int ->
        Triple(total, favs, cats)
    }.combine(
        combine(
            repository.getVisitedCount(),
            repository.getAnalyzedCount()
        ) { visited: Int, analyzed: Int ->
            Pair(visited, analyzed)
        )
    ) { first: Triple<Int, Int, Int>,
        second: Pair<Int, Int> ->

        DashboardStats(
            totalCount = first.first,
            favoriteCount = first.second,
            categoryCount = first.third,
            visitedCount = second.first,
            analyzedCount = second.second
        )
    }.combine(
        repository.getTopCategories()
    ) { stats: DashboardStats, topCats: List<CategoryCount> ->
        stats.copy(topCategories = topCats)
    }.combine(
        repository.getTopVisited()
    ) { stats: DashboardStats, topSites: List<SiteEntity> ->
        stats.copy(topVisited = topSites)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    // ═══ فلاتر ═══
    private fun applyFilters(
        sites: List<SiteEntity>,
        query: String,
        category: String,
        favoritesOnly: Boolean,
        sort: SortOption
    ): List<SiteEntity> {
        var result = sites

        if (favoritesOnly) result = result.filter { it.isFavorite }
        if (category != "الكل") result = result.filter { it.category == category }

        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter { site ->
                site.name.lowercase().contains(q) ||
                site.url.lowercase().contains(q) ||
                site.notes.lowercase().contains(q) ||
                site.description.lowercase().contains(q) ||
                site.tags.lowercase().contains(q) ||
                site.category.lowercase().contains(q) ||
                site.cachedOverview.lowercase().contains(q) ||
                site.pageTitle.lowercase().contains(q) ||
                site.pageDescription.lowercase().contains(q)
            }
        }

        val pinned = result.filter { it.isPinned }
        val unpinned = result.filter { !it.isPinned }

        val sortedUnpinned = when (sort) {
            SortOption.NEWEST -> unpinned.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> unpinned.sortedBy { it.createdAt }
            SortOption.NAME -> unpinned.sortedBy { it.name.lowercase() }
            SortOption.CATEGORY -> unpinned.sortedBy { it.category }
            SortOption.MOST_USED -> unpinned.sortedByDescending { it.visitCount }
            SortOption.LAST_OPENED -> unpinned.sortedByDescending { it.lastVisited }
        }

        return pinned + sortedUnpinned
    }

    // ═══ أحداث ═══
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun selectCategory(category: String) { _selectedCategory.value = category }
    fun setSortOption(option: SortOption) { _sortOption.value = option }
    fun toggleFavoritesOnly() { _showFavoritesOnly.value = !_showFavoritesOnly.value }

    // ═══ CRUD ═══
    fun addSite(site: SiteEntity) {
        viewModelScope.launch { repository.insertSite(site) }
    }

    fun updateSite(site: SiteEntity) {
        viewModelScope.launch { repository.updateSite(site) }
    }

    fun deleteSite(site: SiteEntity) {
        viewModelScope.launch { repository.deleteSite(site) }
    }

    // ═══ إجراءات ═══
    fun incrementVisit(id: Int) {
        viewModelScope.launch { repository.incrementVisit(id) }
    }

    fun toggleFavorite(id: Int, currentValue: Boolean) {
        viewModelScope.launch { repository.setFavorite(id, !currentValue) }
    }

    fun togglePinned(id: Int, currentValue: Boolean) {
        viewModelScope.launch { repository.setPinned(id, !currentValue) }
    }

    suspend fun checkDuplicate(url: String): Boolean {
        return repository.countByUrl(url) > 0
    }

    // ═══ تدفقات ═══
    fun getSiteById(id: Int): Flow<SiteEntity?> = repository.getSiteByIdFlow(id)

    fun getAnalysesForSite(siteId: Int): Flow<List<SiteAnalysisEntity>> =
        repository.getAnalysesForSite(siteId)

    fun getAllCategories(): Flow<List<String>> = repository.getAllCategories()

    // ═══ الإعدادات ═══
    fun loadAiConfig(): AiConfig {
        val prefs = getApplication<Application>()
            .getSharedPreferences("sitemanager_prefs", android.content.Context.MODE_PRIVATE)
        return AiConfig(
            provider = prefs.getString("ai_provider", "groq") ?: "groq",
            apiKey = prefs.getString("ai_key", "") ?: "",
            model = prefs.getString("ai_model", "") ?: "",
            baseUrl = prefs.getString("ai_base_url", "") ?: ""
        )
    }
        }
