package com.tgplayer.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tgplayer.android.data.ChatKind
import com.tgplayer.android.data.ChatSummary
import com.tgplayer.android.data.Favorites
import com.tgplayer.android.ui.common.ChatAvatar
import com.tgplayer.android.ui.common.EmptyView
import com.tgplayer.android.ui.common.ErrorView
import com.tgplayer.android.ui.common.SkeletonList
import com.tgplayer.android.ui.common.UiState
import com.tgplayer.android.ui.common.pressScale
import com.tgplayer.android.ui.chats.ChatsViewModel
import com.tgplayer.android.ui.theme.AppIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(pagePadding: Dp, onOpenChat: (ChatSummary) -> Unit, onSearch: () -> Unit = {}, onSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val vm: ChatsViewModel = viewModel(factory = ChatsViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val visible by vm.visible.collectAsStateWithLifecycle()
    val kindFilter by vm.kindFilter.collectAsStateWithLifecycle()
    val sortByName by vm.sortByName.collectAsStateWithLifecycle()
    val favoriteChats by Favorites.chats.collectAsStateWithLifecycle()
    val favoriteIds = remember(favoriteChats) { favoriteChats.map { it.id }.toSet() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("媒体", style = MaterialTheme.typography.titleLarge) },
            actions = {
                IconButton(onClick = onSearch) {
                    Icon(AppIcons.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onSettings) {
                    Icon(AppIcons.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
        )
        Column(Modifier.fillMaxSize().padding(horizontal = pagePadding)) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val labels = listOf("全部", "频道", "群组", "私聊")
                items(labels.size) { index ->
                    FilterChip(selected = kindFilter == index, onClick = { vm.setKindFilter(index) }, label = { Text(labels[index]) })
                }
            }
            TextButton(onClick = { vm.toggleSort() }) {
                Text(if (sortByName) "排序：名称" else "排序：最近")
            }

            when (val s = state) {
                is UiState.Loading -> SkeletonList()
                is UiState.Failed -> ErrorView((s as UiState.Failed).message, onRetry = { vm.load() })
                is UiState.Ready -> if (visible.isEmpty()) {
                    EmptyView("没有匹配的聊天", "试试切换筛选或重新加载") {
                        TextButton(onClick = { vm.load() }) { Text("重新加载") }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(visible, key = { it.id }, contentType = { "chat" }) { chat ->
                            val interaction = remember { MutableInteractionSource() }
                            Row(
                                Modifier.fillMaxWidth().animateItem(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .pressScale(interaction)
                                        .clickable(interactionSource = interaction, indication = LocalIndication.current) { onOpenChat(chat) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ChatAvatar(chat, 46.dp)
                                    Column(Modifier.weight(1f).padding(start = 13.dp)) {
                                        Text(chat.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                        Text("打开查看视频", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(formatDate(chat.lastDate), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = {
                                    Favorites.toggleChat(Favorites.Chat(chat.id, chat.title, chat.photoFileId))
                                    Toast.makeText(
                                        context,
                                        if (chat.id in favoriteIds) "已取消收藏" else "已收藏",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }) {
                                    Icon(
                                        AppIcons.Star,
                                        contentDescription = if (chat.id in favoriteIds) "取消收藏" else "收藏",
                                        tint = if (chat.id in favoriteIds) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(epochSeconds: Int): String {
    if (epochSeconds <= 0) return ""
    val now = System.currentTimeMillis()
    val then = epochSeconds.toLong() * 1000L
    val days = (now - then) / 86_400_000L
    return if (days < 1) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(then))
    } else {
        SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(then))
    }
}
