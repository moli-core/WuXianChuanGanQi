package com.smarthome.app.core.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val shortTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatFull(date: Date): String = fullFormat.format(date)

    fun formatDate(date: Date): String = dateFormat.format(date)

    fun formatTime(date: Date): String = timeFormat.format(date)

    fun formatShortTime(date: Date): String = shortTimeFormat.format(date)

    fun parseFull(dateStr: String): Date? = try {
        fullFormat.parse(dateStr)
    } catch (e: Exception) {
        null
    }

    fun formatTimestamp(timestamp: Long): String = fullFormat.format(Date(timestamp))

    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86400_000 -> "${diff / 3600_000}小时前"
            else -> "${diff / 86400_000}天前"
        }
    }
}
