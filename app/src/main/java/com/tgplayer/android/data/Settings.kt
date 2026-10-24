package com.tgplayer.android.data

import androidx.compose.runtime.Immutable

import android.content.Context
import org.json.JSONArray

/** Small, synchronous preference store: resume points and playback switches. */
class Settings private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("tgplayer", Context.MODE_PRIVATE)

    var rememberPosition: Boolean
        get() = prefs.getBoolean("remember_position", true)
        set(value) = prefs.edit().putBoolean("remember_position", value).apply()

    var defaultSpeed: Float
        get() = prefs.getFloat("default_speed", 1f)
        set(value) = prefs.edit().putFloat("default_speed", value).apply()

    var wifiOnlyDownloads: Boolean
        get() = prefs.getBoolean("wifi_only_downloads", true)
        set(value) = prefs.edit().putBoolean("wifi_only_downloads", value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) = prefs.edit().putBoolean("onboarding_done", value).apply()

    var themeMode: Int
        get() = prefs.getInt("theme_mode", 0) // 0 system, 1 dark, 2 light
        set(value) = prefs.edit().putInt("theme_mode", value).apply()

    var dynamicColor: Boolean
        get() = prefs.getBoolean("dynamic_color", true)
        set(value) = prefs.edit().putBoolean("dynamic_color", value).apply()

    var hdThumbnails: Boolean
        get() = prefs.getBoolean("hd_thumbs", true)
        set(value) = prefs.edit().putBoolean("hd_thumbs", value).apply()

    fun resumeMs(chatId: Long, messageId: Long): Long =
        if (!rememberPosition) 0L else prefs.getLong("resume:$chatId:$messageId", 0L)

    fun setResume(chatId: Long, messageId: Long, ms: Long) {
        prefs.edit().putLong("resume:$chatId:$messageId", ms).apply()
    }

    var saveData: Boolean
        get() = prefs.getBoolean("save_data", false)
        set(value) = prefs.edit().putBoolean("save_data", value).apply()

    var autoplay: Boolean
        get() = prefs.getBoolean("autoplay", true)
        set(value) = prefs.edit().putBoolean("autoplay", value).apply()

    var lastChatId: Long
        get() = prefs.getLong("last_chat", 0L)
        set(value) = prefs.edit().putLong("last_chat", value).apply()

    var lastChatTitle: String?
        get() = prefs.getString("last_chat_title", null)
        set(value) = prefs.edit().putString("last_chat_title", value).apply()

    var lastChatPhoto: Int
        get() = prefs.getInt("last_chat_photo", 0)
        set(value) = prefs.edit().putInt("last_chat_photo", value).apply()

    fun recordPlay(chatId: Long, messageId: Long, fileId: Int, title: String, chatTitle: String, positionMs: Long, durationMs: Long = 0L, thumbnailFileId: Int = 0) {
        if (messageId == 0L) return
        val list = recentPlays().toMutableList()
        list.removeAll { it.chatId == chatId && it.messageId == messageId }
        list.add(0, RecentPlay(chatId, messageId, fileId, title, chatTitle, positionMs, System.currentTimeMillis(), durationMs, thumbnailFileId))
        val json = JSONArray()
        list.take(24).forEach { p ->
            json.put(
                org.json.JSONObject().apply {
                    put("chatId", p.chatId)
                    put("messageId", p.messageId)
                    put("fileId", p.fileId)
                    put("title", p.title)
                    put("chatTitle", p.chatTitle)
                    put("positionMs", p.positionMs)
                    put("updatedAt", p.updatedAt)
                    put("durationMs", p.durationMs)
                    put("thumbnailFileId", p.thumbnailFileId)
                },
            )
        }
        prefs.edit().putString("recent_plays", json.toString()).apply()
    }

    fun recentPlays(): List<RecentPlay> {
        val raw = prefs.getString("recent_plays", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val o = array.optJSONObject(index) ?: return@mapNotNull null
                RecentPlay(
                    chatId = o.optLong("chatId"),
                    messageId = o.optLong("messageId"),
                    fileId = o.optInt("fileId"),
                    title = o.optString("title"),
                    chatTitle = o.optString("chatTitle"),
                    positionMs = o.optLong("positionMs"),
                    updatedAt = o.optLong("updatedAt"),
                    durationMs = o.optLong("durationMs"),
                    thumbnailFileId = o.optInt("thumbnailFileId"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun recentSearches(): List<String> {
        val raw = prefs.getString("recent_searches", null) ?: return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }.take(8)
    }

    fun addSearch(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        val all = (listOf(q) + recentSearches().filterNot { it.equals(q, ignoreCase = true) }).take(8)
        prefs.edit().putString("recent_searches", all.joinToString("\n")).apply()
    }

    fun clearSearches() = prefs.edit().remove("recent_searches").apply()

    fun clear() = prefs.edit().clear().apply()

    companion object {
        @Volatile
        private var instance: Settings? = null

        fun install(context: Context): Settings =
            instance ?: synchronized(this) {
                instance ?: Settings(context).also { instance = it }
            }

        fun get(): Settings = instance ?: error("Settings.install(context) was not called")
    }
}
@Immutable
data class RecentPlay(
    val chatId: Long,
    val messageId: Long,
    val fileId: Int,
    val title: String,
    val chatTitle: String,
    val positionMs: Long,
    val updatedAt: Long,
    val durationMs: Long = 0L,
    val thumbnailFileId: Int = 0,
)
