package com.tgplayer.android.ui.chats

import com.tgplayer.android.data.ChatSummary
import com.tgplayer.android.data.MediaItem
import com.tgplayer.android.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow

open class FakeMediaRepository(
    var chats: List<ChatSummary> = emptyList(),
    var videos: List<MediaItem> = emptyList(),
) : MediaRepository {
    val connectedFlow = MutableStateFlow(true)
    override val connected = connectedFlow
    override suspend fun chats(limit: Int): List<ChatSummary> = chats
    override suspend fun videos(chatId: Long, limit: Int): List<MediaItem> = videos
    override suspend fun searchChats(query: String): List<ChatSummary> = chats
}
