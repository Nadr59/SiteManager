package com.example.sitemanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sitemanager.data.local.SiteDatabase
import com.example.sitemanager.data.local.SiteEntity
import com.example.sitemanager.data.repository.SiteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SiteManagerUiState(
    val selectedTab: Int = 0,       // 0 = favorites, 1 = saved
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val showAddSheet: Boolean = false,
    val sharedUrl: String? = null,
    val sharedTitle: String? = null,
    val editingSite: SiteEntity? = null,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SiteViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SiteDatabase.getDatabase(application)
    private val repository = SiteRepository(db.siteDao())

    private val _uiState = MutableStateFlow(SiteManagerUiState())
    val uiState: StateFlow<SiteManagerUiState> = _uiState.asStateFlow()

    // ═══ بحث ═══
    private val _searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<SiteEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                repository.search(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ═══ التبويبات ═══
    val favorites: StateFlow<List<SiteEntity>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSites: StateFlow<List<SiteEntity>> = repository.getSaved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ═══ اختيار التبويب ═══
    fun onTabSelected(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    // ═══ البحث ═══
    fun onSearchToggle() {
        val current = _uiState.value
        if (current.isSearching) {
            _uiState.value = current.copy(isSearching = false, searchQuery = "")
            _searchQuery.value = ""
        } else {
            _uiState.value = current.copy(isSearching = true)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchQuery.value = query
    }

    // ═══ استقبال المشاركة ═══
    fun onShareReceived(url: String?, title: String?) {
        if (url.isNullOrBlank()) return
        _uiState.value = _uiState.value.copy(
            showAddSheet = true,
            sharedUrl = url,
            sharedTitle = title ?: extractDomain(url)
        )
    }

    fun onAddSheetDismissed() {
        _uiState.value = _uiState.value.copy(
            showAddSheet = false,
            sharedUrl = null,
            sharedTitle = null
        )
    }

    fun addSite(tabType: String, customTitle: String? = null) {
        val url = _uiState.value.sharedUrl ?: return
        val title = customTitle?.ifBlank { null }
            ?: _uiState.value.sharedTitle
            ?: extractDomain(url)

        val faviconUrl = "https://www.google.com/s2/favicons?domain=${extractDomain(url)}&sz=64"

        viewModelScope.launch {
            val site = SiteEntity(
                url = url,
                title = title,
                tabType = tabType,
                faviconUrl = faviconUrl
            )
            val added = repository.addSite(site)
            _uiState.value = _uiState.value.copy(
                showAddSheet = false,
                sharedUrl = null,
                sharedTitle = null,
                message = if (added) "تمت الإضافة" else "الموقع موجود مسبقاً"
            )
        }
    }

    // ═══ فتح الموقع ═══
    fun onSiteOpened(site: SiteEntity) {
        viewModelScope.launch {
            repository.incrementClick(site.id)
        }
    }

    // ═══ تعديل الاسم ═══
    fun onEditSite(site: SiteEntity) {
        _uiState.value = _uiState.value.copy(editingSite = site)
    }

    fun onEditDismissed() {
        _uiState.value = _uiState.value.copy(editingSite = null)
    }

    fun onTitleUpdated(newTitle: String) {
        val site = _uiState.value.editingSite ?: return
        viewModelScope.launch {
            repository.updateTitle(site.id, newTitle)
            _uiState.value = _uiState.value.copy(
                editingSite = null,
                message = "تم التعديل"
            )
        }
    }

    // ═══ نقل بين التبويبات ═══
    fun onMoveToTab(site: SiteEntity) {
        val newTab = if (site.tabType == "favorites") "saved" else "favorites"
        viewModelScope.launch {
            repository.moveToTab(site.id, newTab)
            _uiState.value = _uiState.value.copy(
                message = if (newTab == "favorites") "نُقل إلى الأكثر استخداماً" else "نُقل إلى المحفوظات"
            )
        }
    }

    // ═══ حذف ═══
    fun onDeleteSite(site: SiteEntity) {
        viewModelScope.launch {
            repository.delete(site)
            _uiState.value = _uiState.value.copy(message = "تم الحذف")
        }
    }

    // ═══ رسالة ═══
    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    // ═══ أداة مساعدة ═══
    private fun extractDomain(url: String): String {
        return try {
            val host = java.net.URI(url).host ?: url
            host.removePrefix("www.")
        } catch (_: Exception) {
            url.take(30)
        }
    }
}
