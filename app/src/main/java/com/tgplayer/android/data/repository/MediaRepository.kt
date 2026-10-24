package com.tgplayer.android.data.repository

import com.tgplayer.android.data.ChatSummary
import com.tgplayer.android.data.MediaItem
import com.tgplayer.android.data.Td
import kotlinx.coroutines.flow.StateFlow

/** The data the UI is allowed to ask for; hides the Td/TDLib singleton. */
interface MediaRepository {
    val connected: StateFlow<Boolean>
    suspend fun chats(limit: Int = 60): List<ChatSummary>
    suspend fun videos(chatId: Long, limit: Int = 200): List<MediaItem>
    suspend fun searchChats(query: String): List<ChatSummary>
}

class TdMediaRepository : MediaRepository {
    override val connected: StateFlow<Boolean> = Td.connected

    override suspend fun chats(limit: Int): List<ChatSummary> = Td.chats(limit)

    override suspend fun videos(chatId: Long, limit: Int): List<MediaItem> = Td.videos(chatId, limit)

    override suspend fun searchChats(query: String): List<ChatSummary> = Td.searchChats(query)
}
