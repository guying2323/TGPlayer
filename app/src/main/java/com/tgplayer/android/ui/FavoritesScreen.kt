package com.tgplayer.android.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgplayer.android.data.ChatSummary
import com.tgplayer.android.data.Favorites
import com.tgplayer.android.player.PlayerActivity
import com.tgplayer.android.ui.common.ChatAvatar
import com.tgplayer.android.ui.common.EmptyView
import com.tgplayer.android.ui.common.Thumbnail
import com.tgplayer.android.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(pagePadding: Dp, onBack: () -> Unit, onOpenChat: (ChatSummary) -> Unit) {
    val context = LocalContext.current
    val chats by Favorites.chats.collectAsStateWithLifecycle()
    val videos by Favorites.videos.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(AppIcons.Back, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            title = { Text("收藏", style = MaterialTheme.typography.titleLarge) },
        )
        Column(Modifier.fillMaxSize().padding(horizontal = pagePadding)) {
            if (chats.isEmpty() && videos.isEmpty()) {
                EmptyView("还没有收藏", "在聊天或视频卡上点星标")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (chats.isNotEmpty()) {
                        item(key = "chat-header") { SectionLabel("收藏的聊天") }
                        items(chats, key = { "fav-chat-" + it.id }) { chat ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChat(ChatSummary(chat.id, chat.title, chat.photoFileId)) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ChatAvatar(ChatSummary(chat.id, chat.title, chat.photoFileId), 42.dp)
                                Text(
                                    chat.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                )
                                IconButton(onClick = { Favorites.removeChat(chat) }) {
                                    Icon(AppIcons.Star, contentDescription = "取消收藏", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    if (videos.isNotEmpty()) {
                        item(key = "video-header") { SectionLabel("收藏的视频") }
                        items(videos, key = { "fav-video-" + it.chatId + "-" + it.messageId }) { video ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        context.startActivity(
                                            Intent(context, PlayerActivity::class.java).apply {
                                                putExtra(PlayerActivity.EXTRA_CHAT_ID, video.chatId)
                                                putExtra(PlayerActivity.EXTRA_MESSAGE_ID, video.messageId)
                                                putExtra(PlayerActivity.EXTRA_FILE_ID, video.fileId)
                                                putExtra(PlayerActivity.EXTRA_TITLE, video.title)
                                                putExtra(PlayerActivity.EXTRA_CHAT_TITLE, video.chatTitle)
                                                putExtra(PlayerActivity.EXTRA_THUMBNAIL, video.thumbnailFileId)
                                            },
                                        )
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Thumbnail(
                                    video.thumbnailFileId,
                                    Modifier.padding(end = 10.dp).size(width = 66.dp, height = 44.dp),
                                    seed = video.fileId,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(video.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    Text(
                                        video.chatTitle.ifBlank { "视频" },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                IconButton(onClick = { Favorites.removeVideo(video) }) {
                                    Icon(AppIcons.Star, contentDescription = "取消收藏", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}
