package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {

    fun formatLastSeen(isOnline: Boolean, lastSeenTimestamp: Long): String {
        if (isOnline) return "متصل الآن"
        
        val now = System.currentTimeMillis()
        val diffMs = now - lastSeenTimestamp
        val diffMinutes = diffMs / (1000 * 60)
        val diffHours = diffMs / (1000 * 60 * 60)
        val diffDays = diffMs / (1000 * 60 * 60 * 24)

        return when {
            diffMinutes < 1 -> "آخر ظهور منذ لحظات"
            diffMinutes < 60 -> "آخر ظهور منذ $diffMinutes دقيقة"
            diffHours < 24 -> "آخر ظهور منذ $diffHours ساعة"
            diffDays == 1L -> "آخر ظهور أمس"
            else -> {
                val sdf = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
                "آخر ظهور " + sdf.format(Date(lastSeenTimestamp))
            }
        }
    }

    fun formatMessageTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
        return sdf.format(Date(timestamp))
    }

    fun formatConversationTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp
        val diffHours = diffMs / (1000 * 60 * 60)
        val diffDays = diffMs / (1000 * 60 * 60 * 24)

        return when {
            diffHours < 24 -> {
                val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
                sdf.format(Date(timestamp))
            }
            diffDays == 1L -> "أمس"
            diffDays < 7 -> {
                val sdf = SimpleDateFormat("EEEE", Locale("ar"))
                sdf.format(Date(timestamp))
            }
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))
                sdf.format(Date(timestamp))
            }
        }
    }
}
