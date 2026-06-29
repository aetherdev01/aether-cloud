package com.aether.cloud.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "Unknown"
    val units = arrayOf("B", "KB", "MB", "GB")
    var s = size.toDouble()
    var unitIndex = 0
    while (s >= 1024 && unitIndex < units.size - 1) {
        s /= 1024
        unitIndex++
    }
    return String.format("%.1f %s", s, units[unitIndex])
}
