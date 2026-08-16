package com.nadr59.sitemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nadr59.sitemanager.data.local.SiteEntity
import com.nadr59.sitemanager.data.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SiteUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "الكل",
    val allSites: List<SiteEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = true
) {
    val filteredSites: List<SiteEntity>
        get() {
            var result = allSites
            if (selectedCategory != "الكل") {
                result = result.filter { it.category == selectedCategory }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase()
                result = result.filter {
                    it.name.lowercase().contains(q) ||
                    it.url.lowercase().contains(q) ||
                    it.notes.lowercase().contains(q)
                }
            }
            return result
        }
}

@HiltViewModel
class SiteViewModel @Inject constructor(
    private val repository: SiteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("الكل")

    private val _uiState = MutableStateFlow(SiteUiState())
    val uiState: StateFlow<SiteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllSites(),
                repository.getAllCategories(),
                _searchQuery,
                _selectedCategory
            ) { sites, categories, query, category ->
                SiteUiState(
                    searchQuery = query,
                    selectedCategory = category,
                    allSites = sites,
                    categories = categories,
                    isLoading = false
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SiteUiState()
            ).collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun addSite(site: SiteEntity) {
        viewModelScope.launch {
            repository.insertSite(site)
        }
    }

    fun updateSite(site: SiteEntity) {
        viewModelScope.launch {
            repository.updateSite(site)
        }
    }

    fun deleteSite(site: SiteEntity) {
        viewModelScope.launch {
            repository.deleteSite(site)
        }
    }
    // ═══ أضف هذه الدالة ═══
fun updateSite(site: SiteEntity) {
    viewModelScope.launch {
        try {
            repository.updateSite(site)
            loadSites()
        } catch (_: Exception) {}
    }
}

    fun incrementVisit(id: Int) {
        viewModelScope.launch {
            repository.incrementVisit(id)
        }
    }
}
