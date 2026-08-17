package com.nadr59.sitemanager.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "غير متوفر"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}

fun formatRelativeTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "غير متوفر"
    val now = System.currentTimeMillis()
    val diff = now - epochMillis

    return when {
        diff < 60_000 -> "الآن"
        diff < 3_600_000 -> "منذ ${diff / 60_000} دقيقة"
        diff < 86_400_000 -> "منذ ${diff / 3_600_000} ساعة"
        diff < 604_800_000 -> "منذ ${diff / 86_400_000} يوم"
        else -> formatDate(epochMillis)
    }
}
