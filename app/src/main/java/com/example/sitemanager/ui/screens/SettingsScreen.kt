package com.nadr59.sitemanager.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("sitemanager_prefs", Context.MODE_PRIVATE)
    }

    var provider by remember { mutableStateOf(prefs.getString("ai_provider", "groq") ?: "groq") }
    var apiKey by remember { mutableStateOf(prefs.getString("ai_key", "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("ai_model", "") ?: "") }
    var baseUrl by remember { mutableStateOf(prefs.getString("ai_base_url", "") ?: "") }
    var expanded by remember { mutableStateOf(false) }

    val providers = listOf(
        "groq" to "Groq (مجاني)",
        "hcnsec" to "HCNSEC",
        "openrouter" to "OpenRouter",
        "openai" to "OpenAI",
        "gemini" to "Google Gemini",
        "custom" to "مزود مخصص"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("مزود الذكاء الاصطناعي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = providers.find { it.first == provider }?.second ?: provider,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        providers.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    provider = key
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text("مفتاح API", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("sk-...") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                Text(
                    "النموذج (اختياري)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            when (provider) {
                                "groq" -> "llama-3.3-70b-versatile"
                                "openrouter" -> "google/gemini-2.0-flash-exp"
                                "gemini" -> "gemini-2.0-flash"
                                "hcnsec" -> "auto"
                                else -> "اسم النموذج"
                            }
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (provider == "custom") {
                item {
                    Text("رابط API", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://api.example.com") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        prefs.edit().apply {
                            putString("ai_provider", provider)
                            putString("ai_key", apiKey.trim())
                            putString("ai_model", model.trim())
                            putString("ai_base_url", baseUrl.trim())
                            apply()
                        }
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("حفظ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("نصائح", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Groq: مجاني — console.groq.com", fontSize = 13.sp, lineHeight = 20.sp)
                        Text("Gemini: مجاني — ai.google.dev", fontSize = 13.sp, lineHeight = 20.sp)
                        Text("OpenRouter: openrouter.ai", fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}
