package com.tgplayer.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tgplayer.android.data.ChatKind
import com.tgplayer.android.data.ChatSummary
import com.tgplayer.android.data.Settings
import com.tgplayer.android.data.Td
import com.tgplayer.android.ui.common.ChatAvatar
import com.tgplayer.android.ui.theme.AppIcons
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(pagePadding: Dp, onOpenChat: (ChatSummary) -> Unit, onBack: () -> Unit = {}) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var recent by remember { mutableStateOf(Settings.get().recentSearches()) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            results = emptyList(); searching = false; error = null; return@LaunchedEffect
        }
        searching = true
        error = null
        delay(300)
        runCatching { Td.searchChats(q) }
            .onSuccess {
                results = it
                if (it.isNotEmpty()) {
                    Settings.get().addSearch(q)
                    recent = Settings.get().recentSearches()
                }
            }
            .onFailure { results = emptyList(); error = it.message }
        searching = false
    }

    val channels = results.filter { it.kind == ChatKind.Channel }
    val groups = results.filter { it.kind == ChatKind.Group }
    val people = results.filter { it.kind == ChatKind.Person || it.kind == ChatKind.Other }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(AppIcons.Back, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            title = { Text("搜索", style = MaterialTheme.typography.titleLarge) },
        )
        Column(Modifier.fillMaxSize().padding(horizontal = pagePadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("搜索频道、群组或聊天名称") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (searching) {
                Spacer(Modifier.height(10.dp))
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
            }
            if (query.isBlank() && recent.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(recent, key = { it }) { term ->
                        AssistChip(onClick = { query = term }, label = { Text(term) })
                    }
                }
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                if (query.isNotBlank() && !searching && results.isEmpty()) {
                    item { Text("没有找到结果", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp)) }
                }
                if (channels.isNotEmpty()) {
                    item { SectionLabel("频道") }
                    items(channels, key = { "ch-" + it.id }) { chat -> ChatHit(chat, onOpenChat, Modifier.animateItem()) }
                }
                if (groups.isNotEmpty()) {
                    item { SectionLabel("群组") }
                    items(groups, key = { "gr-" + it.id }) { chat -> ChatHit(chat, onOpenChat, Modifier.animateItem()) }
                }
                if (people.isNotEmpty()) {
                    item { SectionLabel("聊天") }
                    items(people, key = { "pe-" + it.id }) { chat -> ChatHit(chat, onOpenChat, Modifier.animateItem()) }
                }
            }
        }
    }
}

@Composable
private fun ChatHit(chat: ChatSummary, onOpenChat: (ChatSummary) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clickable { onOpenChat(chat) }.padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(chat, 42.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(chat.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text("打开查看视频", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
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
