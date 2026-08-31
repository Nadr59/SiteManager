package com.nadr59.sitemanager.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.nadr59.sitemanager.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    application: Application,
    private val prefs: SharedPreferences
) : AndroidViewModel(application) {

    private val _currentTheme = MutableStateFlow(loadTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow(loadDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        prefs.edit().putBoolean("dark_mode", newValue).apply()
    }

    private fun loadTheme(): AppTheme {
        val saved = prefs.getString("app_theme", AppTheme.BLUE.name)
        return AppTheme.entries.find { it.name == saved } ?: AppTheme.BLUE
    }

    private fun loadDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", false)
    }
}
