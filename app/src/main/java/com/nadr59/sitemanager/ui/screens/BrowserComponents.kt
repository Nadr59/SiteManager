package com.nadr59.sitemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopBar(
    title: String,
    isLoading: Boolean,
    isTranslationMode: Boolean,
    onBack: () -> Unit,
    onTranslate: () -> Unit,
    onTranslateSelection: () -> Unit,
    onResetTranslation: () -> Unit,
    onReload: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text            if (text.isBlank()) return

            _selectedText.value = text

            viewModelScope.launch {
                val result = translationRepository.translateText(
                    text = text,
                    targetLanguage = _uiState.value.targetLanguage
                )
                result.fold(
                    onSuccess = { translated ->
                        _selectedTranslation.value = translated
                        val replaceScript = pageTranslator.buildReplaceSelectionScript(translated)
                        _pendingJs.value = replaceScript
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(error = "فشل ترجمة النص: ${e.message}")
                        }
                    }
                )
            }
        } catch (_: Exception) {}
    }

    // ═══ إلغاء الترجمة ═══
    fun resetTranslation() {
        _uiState.update {
            it.copy(
                isTranslationMode = false,
                isTranslating = false,
                translationProgress = 0f
            )
        }
        _extractedNodes.value = emptyList()
        _translatedNodes.value = emptyList()
    }

 = title.ifBlank { "المتصفح" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                if (isTranslationMode) {
                    Text(
                        text = "مترجم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
            }
        },
        actions = {
            if (isTranslationMode) {
                IconButton(onClick = onTranslateSelection) {
                    Icon(Icons.Default.TextFields, "ترجمة النص المحدد")
                }
                IconButton(onClick = onResetTranslation) {
                    Icon(Icons.Default.Undo, "إلغاء الترجمة")
                }
            } else {
                IconButton(onClick = onTranslate) {
                    Icon(Icons.Default.Translate, "ترجمة الصفحة")
                }
            }
            IconButton(onClick = onReload) {
                Icon(Icons.Default.Refresh, "تحديث")
            }
        }
    )
}

@Composable
fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    url: String,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit
) {
    NavigationBar {
        IconButton(enabled = canGoBack, onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
        }
        IconButton(enabled = canGoForward, onClick = onForward) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "تقدم")
        }
        IconButton(onClick = onReload) {
            Icon(Icons.Default.Refresh, "تحديث")
        }
        IconButton(onClick = onHome) {
            Icon(Icons.Default.Home, "الرئيسية")
        }
    }
}

@Composable
fun TranslationProgressBar(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "جاري الترجمة...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
        )
    }
}
