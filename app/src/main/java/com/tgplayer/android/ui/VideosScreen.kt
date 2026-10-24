package com.tgplayer.android.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tgplayer.android.data.Downloads
import com.tgplayer.android.data.Favorites
import com.tgplayer.android.data.MediaItem
import com.tgplayer.android.data.Settings
import com.tgplayer.android.player.PlayerActivity
import com.tgplayer.android.ui.common.EmptyView
import com.tgplayer.android.ui.common.ErrorView
import com.tgplayer.android.ui.common.LoadingView
import com.tgplayer.android.ui.common.Thumbnails
import com.tgplayer.android.ui.common.UiState
import com.tgplayer.android.ui.findroid.Direction
import com.tgplayer.android.ui.findroid.FindroidItemCard
import com.tgplayer.android.ui.theme.AppIcons
import com.tgplayer.android.ui.videos.VideosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(chatId: Long, title: String, photoFileId: Int, pagePadding: Dp, onBack: () -> Unit) {
    val context = LocalContext.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val settings = remember { Settings.get() }
    val vm: VideosViewModel = viewModel(key = "videos-" + chatId, factory = VideosViewModel.factory(chatId))
    val state by vm.state.collectAsStateWithLifecycle()
    val favoriteVideos by Favorites.videos.collectAsStateWithLifecycle()
    val favoriteChats by Favorites.chats.collectAsStateWithLifecycle()
    val favoriteKeys = remember(favoriteVideos) { favoriteVideos.map { it.chatId to it.messageId }.toSet() }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }

    val items = (state as? UiState.Ready)?.data ?: emptyList()
    LaunchedEffect(items, chatId) {
        if (items.isNotEmpty()) {
            Thumbnails.prefetch(context, items.map { it.thumbnailFileId })
            settings.apply {
                lastChatId = chatId
                lastChatTitle = title
                lastChatPhoto = photoFileId
            }
        }
    }

    val allTags = remember(items) {
        items.flatMap { it.tags }.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.map { it.key }
    }
    val visible = remember(items, query, selectedTag) {
        items.filter { item ->
            (selectedTag == null || item.tags.contains(selectedTag)) &&
                (query.isBlank() || item.title.contains(query, ignoreCase = true))
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(AppIcons.Back, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            title = {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(items.size.toString() + " 个视频", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            },
            actions = {
                val chatFavorite = favoriteChats.any { it.id == chatId }
                IconButton(onClick = {
                    Favorites.toggleChat(Favorites.Chat(chatId, title, photoFileId))
                    Toast.makeText(
                        context,
                        if (chatFavorite) "已取消收藏该聊天" else "已收藏该聊天",
                        Toast.LENGTH_SHORT,
                    ).show()
                }) {
                    Icon(
                        AppIcons.Star,
                        contentDescription = if (chatFavorite) "取消收藏该聊天" else "收藏该聊天",
                        tint = if (chatFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        Column(Modifier.fillMaxSize().padding(horizontal = pagePadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("在这个聊天里搜索视频") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            if (allTags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "all") {
                        FilterChip(selected = selectedTag == null, onClick = { selectedTag = null }, label = { Text("全部") })
                    }
                    allTags.forEach { tag ->
                        item(key = tag) {
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = if (selectedTag == tag) null else tag },
                                label = { Text("#" + tag) },
                            )
                        }
                    }
                }
            }

            when (val s = state) {
                is UiState.Loading -> LoadingView("正在整理视频…")
                is UiState.Failed -> ErrorView((s as UiState.Failed).message, onRetry = { vm.load() })
                is UiState.Ready -> if (items.isEmpty()) {
                    EmptyView("这个聊天里还没有可播放的视频", "确认频道/群组里有视频后再试") {
                        TextButton(onClick = { vm.load() }) { Text("重新加载") }
                    }
                } else if (visible.isEmpty()) {
                    EmptyView("没有匹配的视频", "换个关键词或标签试试")
                } else {
                    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp().value }
                    val columns = if (screenWidthDp >= 600) 3 else 2
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) {
                        items(visible, key = { it.messageId }, contentType = { "video" }) { item ->
                            val resume = settings.resumeMs(item.chatId, item.messageId)
                            val fraction = if (resume > 3_000 && item.durationSec > 0) {
                                (resume / 1000f / item.durationSec).coerceIn(0f, 1f)
                            } else 0f
                            FindroidItemCard(item, Direction.HORIZONTAL, onClick = {
                                context.startActivity(
                                    Intent(context, PlayerActivity::class.java).apply {
                                        putExtra(PlayerActivity.EXTRA_CHAT_ID, item.chatId)
                                        putExtra(PlayerActivity.EXTRA_MESSAGE_ID, item.messageId)
                                        putExtra(PlayerActivity.EXTRA_FILE_ID, item.fileId)
                                        putExtra(PlayerActivity.EXTRA_TITLE, item.title)
                                        putExtra(PlayerActivity.EXTRA_CHAT_TITLE, title)
                                        putExtra(PlayerActivity.EXTRA_THUMBNAIL, item.thumbnailFileId)
                                        putExtra(PlayerActivity.EXTRA_START_INDEX, visible.indexOf(item))
                                        putExtra(PlayerActivity.EXTRA_PLAYLIST_FILES, visible.map { it.fileId }.toIntArray())
                                        putExtra(PlayerActivity.EXTRA_PLAYLIST_MESSAGES, visible.map { it.messageId }.toLongArray())
                                        putExtra(PlayerActivity.EXTRA_PLAYLIST_TITLES, visible.map { it.title }.toTypedArray())
                                        putExtra(PlayerActivity.EXTRA_PLAYLIST_THUMBS, visible.map { it.thumbnailFileId }.toIntArray())
                                    },
                                )
                            }, width = null, progressFraction = fraction, onDownload = {
                                Downloads.enqueue(item)
                                Toast.makeText(context, "已加入下载队列", Toast.LENGTH_SHORT).show()
                            }, isFavorite = (item.chatId to item.messageId) in favoriteKeys,
                            onFavorite = {
                                val wasFavorite = (item.chatId to item.messageId) in favoriteKeys
                                Favorites.toggleVideo(
                                    Favorites.Video(
                                        chatId = item.chatId,
                                        messageId = item.messageId,
                                        fileId = item.fileId,
                                        title = item.title,
                                        chatTitle = title,
                                        thumbnailFileId = item.thumbnailFileId,
                                        durationSec = item.durationSec,
                                        sizeBytes = item.sizeBytes,
                                    ),
                                )
                                Toast.makeText(context, if (wasFavorite) "已取消收藏" else "已收藏", Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                }
            }
        }
    }
}
