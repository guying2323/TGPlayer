package com.tgplayer.android.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgplayer.android.data.ChatSummary
import androidx.compose.ui.platform.LocalContext

/**
 * The chat's real Telegram photo. Falls back to the chat title's first
 * character only when the chat has no photo at all.
 */
@Composable
fun ChatAvatar(summary: ChatSummary, size: Dp = 48.dp) {
    val shape = RoundedCornerShape(15.dp)
    val context = LocalContext.current
    var image by remember(summary.photoFileId) { mutableStateOf(Thumbnails.cached(summary.photoFileId)) }

    LaunchedEffect(summary.photoFileId) {
        if (summary.photoFileId == 0 || image != null) return@LaunchedEffect
        image = Thumbnails.load(context, summary.photoFileId)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(if (image == null) fallbackColor(summary.id) else TmAvatarPlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = summary.title,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = summary.title.firstOrNull()?.toString() ?: "?",
                color = Color(0xFF0B0D10),
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private val TmAvatarPlaceholder = Color(0xFF1A1F26)

private fun fallbackColor(seed: Long): Color {
    val palette = listOf(
        Color(0xFF6FD9CE), Color(0xFFE7C877), Color(0xFFA9A6F5),
        Color(0xFFE9A6A6), Color(0xFF8FC7E8), Color(0xFFC6E39B),
    )
    return palette[((seed % palette.size).toInt() + palette.size) % palette.size]
}
