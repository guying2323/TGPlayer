package com.tgplayer.android.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local favourites for chats (channels/groups/private) and videos. Nothing is
 * sent to a server; it is one JSON blob in the same prefs file as the rest.
 */
object Favorites {

    @Immutable
    data class Chat(
        val id: Long,
        val title: String,
        val photoFileId: Int = 0,
    )

    @Immutable
    data class Video(
        val chatId: Long,
        val messageId: Long,
        val fileId: Int,
        val title: String,
        val chatTitle: String,
        val thumbnailFileId: Int = 0,
        val durationSec: Int = 0,
        val sizeBytes: Long = 0L,
    )

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        load()
    }

    fun isChatFavorite(id: Long): Boolean = _chats.value.any { it.id == id }

    fun isVideoFavorite(chatId: Long, messageId: Long): Boolean =
        _videos.value.any { it.chatId == chatId && it.messageId == messageId }

    fun toggleChat(chat: Chat?) {
        if (chat == null) return
        Log.d("Favorites", "toggleChat id=" + chat.id + " before=" + _chats.value.size)
        _chats.value = if (isChatFavorite(chat.id)) {
            _chats.value.filterNot { it.id == chat.id }
        } else {
            listOf(chat) + _chats.value
        }
        save()
    }

    fun toggleVideo(video: Video) {
        Log.d("Favorites", "toggleVideo chat=" + video.chatId + " msg=" + video.messageId + " before=" + _videos.value.size)
        _videos.value = if (isVideoFavorite(video.chatId, video.messageId)) {
            _videos.value.filterNot { it.chatId == video.chatId && it.messageId == video.messageId }
        } else {
            listOf(video) + _videos.value
        }
        save()
    }

    fun removeVideo(video: Video) {
        _videos.value = _videos.value.filterNot { it.chatId == video.chatId && it.messageId == video.messageId }
        save()
    }

    fun removeChat(chat: Chat) {
        _chats.value = _chats.value.filterNot { it.id == chat.id }
        save()
    }

    private fun load() {
        val prefs = appContext?.getSharedPreferences("tgplayer", Context.MODE_PRIVATE) ?: return
        val raw = prefs.getString("favorites", null) ?: return
        runCatching {
            val root = JSONObject(raw)
            val chatArray = root.optJSONArray("chats") ?: JSONArray()
            _chats.value = (0 until chatArray.length()).mapNotNull { i ->
                val o = chatArray.optJSONObject(i) ?: return@mapNotNull null
                Chat(o.optLong("id"), o.optString("title"), o.optInt("photoFileId"))
            }
            val videoArray = root.optJSONArray("videos") ?: JSONArray()
            _videos.value = (0 until videoArray.length()).mapNotNull { i ->
                val o = videoArray.optJSONObject(i) ?: return@mapNotNull null
                Video(
                    chatId = o.optLong("chatId"),
                    messageId = o.optLong("messageId"),
                    fileId = o.optInt("fileId"),
                    title = o.optString("title"),
                    chatTitle = o.optString("chatTitle"),
                    thumbnailFileId = o.optInt("thumbnailFileId"),
                    durationSec = o.optInt("durationSec"),
                    sizeBytes = o.optLong("sizeBytes"),
                )
            }
        }
    }

    private fun save() {
        val prefs = appContext?.getSharedPreferences("tgplayer", Context.MODE_PRIVATE) ?: return
        val root = JSONObject()
        root.put("chats", JSONArray().apply {
            _chats.value.forEach { chat ->
                put(JSONObject().apply {
                    put("id", chat.id)
                    put("title", chat.title)
                    put("photoFileId", chat.photoFileId)
                })
            }
        })
        root.put("videos", JSONArray().apply {
            _videos.value.forEach { video ->
                put(JSONObject().apply {
                    put("chatId", video.chatId)
                    put("messageId", video.messageId)
                    put("fileId", video.fileId)
                    put("title", video.title)
                    put("chatTitle", video.chatTitle)
                    put("thumbnailFileId", video.thumbnailFileId)
                    put("durationSec", video.durationSec)
                    put("sizeBytes", video.sizeBytes)
                })
            }
        })
        prefs.edit().putString("favorites", root.toString()).apply()
    }
}
