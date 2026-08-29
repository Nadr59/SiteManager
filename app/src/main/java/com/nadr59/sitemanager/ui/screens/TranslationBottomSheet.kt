package com.nadr59.sitemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        "fa" to "فارسی",
        "id" to "Indonesia",
        "zh" to "中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "hi" to "हिन्दी",
        "ru" to "Русский"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "ترجمة الصفحة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "اللغة المستهدفة:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val rows = languages.chunked(3)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (code, name) ->
                        val selected = currentLanguage == code
                        FilterChip(
                            selected = selected,
                            onClick = { onLanguageSelected(code) },
                            label = {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onTranslatePage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Translate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ترجمة الصفحة كاملة",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onTranslateSelection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.TextFields, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "ترجمة النص المحدد")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
