package com.tgplayer.android.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AccountSlot(
    val id: String,
    val name: String,
    val rootDir: String,
    val userId: Long = 0L,
)

/**
 * Account slots for multi-account support. Each slot owns its own TDLib
 * database/files directory; only one client is active at a time in Td.
 * The first slot keeps the historical "tdlib" root so existing logins survive.
 */
object Accounts {

    private const val LIST_KEY = "accounts"
    private const val CURRENT_KEY = "current_account"

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        if (list().isEmpty()) {
            save(listOf(AccountSlot("default", "账号 1", "tdlib")))
            setCurrent("default")
        }
    }

    fun list(): List<AccountSlot> {
        val prefs = prefs() ?: return emptyList()
        val raw = prefs.getString(LIST_KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                AccountSlot(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    rootDir = o.optString("rootDir", "tdlib"),
                    userId = o.optLong("userId"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun currentId(): String {
        val prefs = prefs() ?: return "default"
        return prefs.getString(CURRENT_KEY, null) ?: list().firstOrNull()?.id ?: "default"
    }

    fun current(): AccountSlot? = list().firstOrNull { it.id == currentId() } ?: list().firstOrNull()

    fun updateIdentity(id: String, userId: Long, name: String) {
        val updated = list().map {
            if (it.id == id) it.copy(userId = userId, name = name) else it
        }
        save(updated)
    }

    fun findByUserId(userId: Long): AccountSlot? =
        list().firstOrNull { it.userId != 0L && it.userId == userId }

    fun setCurrent(id: String) {
        prefs()?.edit()?.putString(CURRENT_KEY, id)?.apply()
    }

    fun add(): AccountSlot {
        val all = list()
        val id = "acct-" + System.currentTimeMillis()
        val slot = AccountSlot(id, "账号 " + (all.size + 1), "tdlib/" + id)
        save(all + slot)
        return slot
    }

    fun rename(id: String, name: String) {
        val updated = list().map { if (it.id == id) it.copy(name = name) else it }
        save(updated)
    }

    fun remove(id: String): Boolean {
        val all = list()
        if (all.size <= 1) return false
        save(all.filterNot { it.id == id })
        if (currentId() == id) setCurrent(all.first { it.id != id }.id)
        return true
    }

    private fun save(slots: List<AccountSlot>) {
        val array = JSONArray()
        slots.forEach { slot ->
            array.put(JSONObject().apply {
                put("id", slot.id)
                put("name", slot.name)
                put("rootDir", slot.rootDir)
                put("userId", slot.userId)
            })
        }
        prefs()?.edit()?.putString(LIST_KEY, array.toString())?.apply()
    }

    private fun prefs() = appContext?.getSharedPreferences("tgplayer", Context.MODE_PRIVATE)
}
