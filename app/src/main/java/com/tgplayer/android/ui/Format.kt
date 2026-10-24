package com.tgplayer.android.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    if (gb >= 1.0) return "%.1f GB".format(gb)
    val mb = bytes / 1024.0 / 1024.0
    if (mb >= 1.0) return "%.0f MB".format(mb)
    return (bytes / 1024.0).roundToLong().toString() + " KB"
}

fun resolutionLabel(width: Int, height: Int): String = when {
    height >= 2000 -> "4K"
    height >= 1400 -> "1440p"
    height >= 1000 -> "1080p"
    height >= 700 -> "720p"
    height > 0 -> height.toString() + "p"
    else -> ""
}

fun formatDateShort(seconds: Int): String {
    if (seconds <= 0) return ""
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(seconds * 1000L))
}
