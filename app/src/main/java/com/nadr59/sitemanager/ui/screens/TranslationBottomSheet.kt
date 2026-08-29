package com.nadr59.sitemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationBottomSheet(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onTranslatePage: () -> Unit,
    onTranslateSelection: () -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "ar" to "العربية",
        "en" to "English",
        "fr" to "Français",
        "de" to "Deutsch",
        "es" to "Español",
        "tr" to "Türkçe",
        "ur" to "اردو",
        "fa                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ═══ أزرار الترجمة ═══
            Button(
                onClick = onTranslatePage,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Translate, null)
                Spacer(Modifier.width(8.dp))
                Text("ترجمة الصفحة كاملة", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onTranslateSelection,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.TextFields, null)
                Spacer(Modifier.width(8.dp))
                Text("ترجمة النص المحدد")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
      }
