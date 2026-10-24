package com.tgplayer.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgplayer.android.data.ChatSummary
import com.tgplayer.android.data.RecentPlay
import com.tgplayer.android.data.Settings
import com.tgplayer.android.data.Td
import com.tgplayer.android.player.PlayerActivity
import com.tgplayer.android.ui.common.ChatAvatar
import com.tgplayer.android.ui.findroid.FindroidRecentCard
import com.tgplayer.android.ui.findroid.HomeSection
import com.tgplayer.android.ui.common.Thumbnail
import com.tgplayer.android.ui.common.pressScale
import com.tgplayer.android.ui.theme.AppIcons

/**
 * Findroid-style home: a full-width resume hero, then vertical sections of
 * horizontally scrolling cards (continue watching, chats), with the title bar
 * floating over the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(pagePadding: Dp, onOpenChat: (ChatSummary) -> Unit, onSearch: () -> Unit = {}, onSettings: () -> Unit = {}, onFavorites: () -> Unit = {}) {
    val context = LocalContext.current
    val settings = remember { Settings.get() }
    var chats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var recent by remember { mutableStateOf<List<RecentPlay>>(emptyList()) }

    val openLocal = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            context.startActivity(
                Intent(context, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_URI, uri.toString())
                    putExtra(PlayerActivity.EXTRA_TITLE, displayName(context, uri))
                    putExtra(PlayerActivity.EXTRA_CHAT_TITLE, "本地视频")
                    putExtra(PlayerActivity.EXTRA_CHAT_ID, 0L)
                    putExtra(PlayerActivity.EXTRA_MESSAGE_ID, uri.toString().hashCode().toLong())
                },
            )
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(Unit) {
        chats = runCatching { Td.chats(20) }.getOrDefault(emptyList())
        recent = settings.recentPlays()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            contentPadding = PaddingValues(top = 64.dp + statusBarTop, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            recent.firstOrNull()?.let { hero ->
                item {
                    ResumeHero(hero) {
                        context.startActivity(
                            Intent(context, PlayerActivity::class.java).apply {
                                putExtra(PlayerActivity.EXTRA_CHAT_ID, hero.chatId)
                                putExtra(PlayerActivity.EXTRA_MESSAGE_ID, hero.messageId)
                                putExtra(PlayerActivity.EXTRA_FILE_ID, hero.fileId)
                                putExtra(PlayerActivity.EXTRA_TITLE, hero.title)
                                putExtra(PlayerActivity.EXTRA_CHAT_TITLE, hero.chatTitle)
                            },
                        )
                    }
                }
            }

            if (recent.size > 1) {
                item {
                    HomeSection("继续观看", itemsPadding = PaddingValues(horizontal = pagePadding)) {
                        items(recent.drop(1), key = { it.chatId.toString() + "-" + it.messageId }) { play ->
                            val fraction = if (play.durationMs > 0L) {
                                (play.positionMs.toFloat() / play.durationMs).coerceIn(0f, 1f)
                            } else 0f
                            FindroidRecentCard(
                                thumbnailFileId = play.thumbnailFileId,
                                seed = play.fileId,
                                title = play.title,
                                subtitle = play.chatTitle,
                                progressFraction = fraction,
                            ) {
                                context.startActivity(
                                    Intent(context, PlayerActivity::class.java).apply {
                                        putExtra(PlayerActivity.EXTRA_CHAT_ID, play.chatId)
                                        putExtra(PlayerActivity.EXTRA_MESSAGE_ID, play.messageId)
                                        putExtra(PlayerActivity.EXTRA_FILE_ID, play.fileId)
                                        putExtra(PlayerActivity.EXTRA_TITLE, play.title)
                                        putExtra(PlayerActivity.EXTRA_CHAT_TITLE, play.chatTitle)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (chats.isNotEmpty()) {
                item {
                    HomeSection("聊天", itemsPadding = PaddingValues(horizontal = pagePadding)) {
                        items(chats, key = { it.id }) { chat ->
                            ChatCard(chat) { onOpenChat(chat) }
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .padding(horizontal = pagePadding)
                        .fillMaxWidth()
                        .clickable { openLocal.launch(arrayOf("video/*")) },
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(AppIcons.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("打开本地视频", style = MaterialTheme.typography.titleMedium)
                            Text("从系统文件选择器（SAF）播放", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                        Icon(AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Findroid-style transparent TopAppBar over the content
        TopAppBar(
            title = {
                Text(
                    "TGPlayer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            actions = {
                IconButton(onClick = onSearch) {
                    Icon(AppIcons.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { openLocal.launch(arrayOf("video/*")) }) {
                    Icon(AppIcons.Folder, contentDescription = "打开本地视频", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onFavorites) {
                    Icon(AppIcons.Star, contentDescription = "收藏", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onSettings) {
                    Icon(AppIcons.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun ResumeHero(play: RecentPlay, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .pressScale(interaction, pressedScale = 0.98f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick),
    ) {
        Thumbnail(play.thumbnailFileId, Modifier.fillMaxSize(), seed = play.fileId)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)), startY = 120f),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(play.chatTitle.ifBlank { "继续观看" }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            Text(
                play.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (play.durationMs > 0L) {
                val fraction = (play.positionMs.toFloat() / play.durationMs).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(99.dp)),
                ) {
                    Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp)))
                }
            }
        }
        Box(
            Modifier.align(Alignment.Center).size(54.dp).clip(RoundedCornerShape(99.dp))
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Play, contentDescription = "继续播放", tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun ChatCard(chat: ChatSummary, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(220.dp)
            .pressScale(remember { MutableInteractionSource() })
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ChatAvatar(chat, 44.dp)
            Column(Modifier.padding(start = 10.dp)) {
                Text(chat.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("打开查看视频", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String {
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index) ?: "本地视频"
            }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "本地视频"
}
