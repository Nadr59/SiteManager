package com.nadr59.sitemanager.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nadr59.sitemanager.data.local.SiteEntity
import java.io.BufferedReader
import java.io.InputStreamReader

data class ExportSite(
    val name: String,
    val url: String,
    val category: String,
    val tags: String,
    val notes: String,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val visitCount: Int,
    val createdAt: Long
)

object ExportImportUtils {

    private val gson = Gson()

    // ═══ تصدير كـ JSON ═══
    fun exportToJson(sites: List<SiteEntity>): String {
        val exportList = sites.map { site ->
            ExportSite(
                name = site.name,
                url = site.url,
                category = site.category,
                tags = site.tags,
                notes = site.notes,
                isFavorite = site.isFavorite,
                isPinned = site.isPinned,
                visitCount = site.visitCount,
                createdAt = site.createdAt
            )
        }
        return gson.toJson(exportList)
    }

    // ═══ تصدير كـ CSV ═══
    fun exportToCsv(sites: List<SiteEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("name,url,category,tags,notes,isFavorite,isPinned,visitCount,createdAt")
        sites.forEach { site ->
            sb.appendLine(
                "\"${escapeCsv(site.name)}\"," +
                "\"${escapeCsv(site.url)}\"," +
                "\"${escapeCsv(site.category)}\"," +
                "\"${escapeCsv(site.tags)}\"," +
                "\"${escapeCsv(site.notes)}\"," +
                "${site.isFavorite}," +
                "${site.isPinned}," +
                "${site.visitCount}," +
                "${site.createdAt}"
            )
        }
        return sb.toString()
    }

    // ═══ استيراد من JSON ═══
    fun importFromJson(json: String): List<ExportSite> {
        val type = object : TypeToken<List<ExportSite>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // ═══ استيراد من CSV ═══
    fun importFromCsv(csv: String): List<ExportSite> {
        val result = mutableListOf<ExportSite>()
        val lines = csv.lines().drop(1) // تخطي العنوان
        lines.forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = parseCsvLine(line)
            if (parts.size >= 6) {
                result.add(
                    ExportSite(
                        name = parts[0],
                        url = parts[1],
                        category = parts.getOrElse(2) { "عام" },
                        tags = parts.getOrElse(3) { "" },
                        notes = parts.getOrElse(4) { "" },
                        isFavorite = parts.getOrElse(5) { "false" }.toBoolean(),
                        isPinned = parts.getOrElse(6) { "false" }.toBoolean(),
                        visitCount = parts.getOrElse(7) { "0" }.toIntOrNull() ?: 0,
                        createdAt = parts.getOrElse(8) { "${System.currentTimeMillis()}" }.toLongOrNull() ?: System.currentTimeMillis()
                    )
                )
            }
        }
        return result
    }

    // ═══ حفظ الملف ═══
    fun saveToFile(context: Context, content: String, filename: String, mimeType: String): Uri? {
        return try {
            val file = java.io.File(context.cacheDir, filename)
            file.writeText(content)
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    // ═══ قراءة من ملف ═══
    fun readFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            reader.close()
            content
        } catch (_: Exception) {
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "")
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
