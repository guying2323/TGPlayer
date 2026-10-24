package com.tgplayer.android.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.tgplayer.android.BuildConfig
import dev.g000sha256.tdl.TdlClient
import dev.g000sha256.tdl.TdlResult
import dev.g000sha256.tdl.dto.AuthorizationState
import dev.g000sha256.tdl.dto.AuthorizationStateClosed
import dev.g000sha256.tdl.dto.AuthorizationStateClosing
import dev.g000sha256.tdl.dto.AuthorizationStateLoggingOut
import dev.g000sha256.tdl.dto.AuthorizationStateReady
import dev.g000sha256.tdl.dto.AuthorizationStateWaitCode
import dev.g000sha256.tdl.dto.AuthorizationStateWaitOtherDeviceConfirmation
import dev.g000sha256.tdl.dto.AuthorizationStateWaitPassword
import dev.g000sha256.tdl.dto.AuthorizationStateWaitPhoneNumber
import dev.g000sha256.tdl.dto.AuthorizationStateWaitTdlibParameters
import dev.g000sha256.tdl.dto.ChatTypeBasicGroup
import dev.g000sha256.tdl.dto.ChatTypePrivate
import dev.g000sha256.tdl.dto.ChatTypeSecret
import dev.g000sha256.tdl.dto.ChatTypeSupergroup
import dev.g000sha256.tdl.dto.MessageAnimation
import dev.g000sha256.tdl.dto.MessageDocument
import dev.g000sha256.tdl.dto.MessageVideo
import dev.g000sha256.tdl.dto.ChatListArchive
import dev.g000sha256.tdl.dto.ChatListMain
import dev.g000sha256.tdl.dto.ConnectionStateReady
import dev.g000sha256.tdl.dto.MessageText
import dev.g000sha256.tdl.dto.MessagePhoto
import dev.g000sha256.tdl.dto.SearchMessagesFilterEmpty
import dev.g000sha256.tdl.dto.SearchMessagesFilterAnimation
import dev.g000sha256.tdl.dto.SearchMessagesFilterDocument
import dev.g000sha256.tdl.dto.SearchMessagesFilterVideo
import dev.g000sha256.tdl.dto.NetworkType
import dev.g000sha256.tdl.dto.NetworkTypeMobile
import dev.g000sha256.tdl.dto.NetworkTypeOther
import dev.g000sha256.tdl.dto.NetworkTypeWiFi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * The single TDLib connection for the process.
 *
 * Owns the client lifecycle, the authorization state machine, chat and video
 * queries, and the file primitives that streaming playback and avatar loading
 * build on. TDLib itself keeps the session on disk.
 */
object Td {

    private const val TAG = "Td"
    private const val START_TIMEOUT_MS = 45_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generation = java.util.concurrent.atomic.AtomicInteger(0)

    @Volatile
    private var client: TdlClient? = null

    @Volatile
    private var starting = false

    private var appContext: Context? = null

    @Volatile
    private var chatsLoaded = false

    private val _auth = MutableStateFlow<AuthState>(AuthState.Starting)
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _me = MutableStateFlow<String?>(null)
    val me: StateFlow<String?> = _me.asStateFlow()

    @Volatile
    private var lastAccountId: String? = null

    @Volatile
    private var pendingNewAccountId: String? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        Accounts.install(context)
        start()
    }

    // ---------------- accounts ----------------

    fun accounts(): List<AccountSlot> = Accounts.list()

    fun currentAccountId(): String = Accounts.currentId()

    fun switchAccount(id: String) {
        if (Accounts.currentId() == id) return
        lastAccountId = Accounts.currentId()
        pendingNewAccountId = null
        Accounts.setCurrent(id)
        resetClient()
    }

    fun addAccount(): AccountSlot {
        // Ignore a second tap while an added account is still waiting for login.
        pendingNewAccountId?.let { pending ->
            Accounts.list().firstOrNull { it.id == pending }?.let { return it }
        }
        val previous = Accounts.currentId()
        val slot = Accounts.add()
        lastAccountId = previous
        pendingNewAccountId = slot.id
        Accounts.setCurrent(slot.id)
        resetClient()
        return slot
    }

    fun removeAccount(id: String) {
        val remaining = Accounts.list().filterNot { it.id == id }
        if (remaining.isEmpty()) return
        if (Accounts.currentId() == id) switchAccount(remaining.first().id)
        Accounts.remove(id)
        lastAccountId = null
    }

    /** True while an account add/switch can still be cancelled back to the previous account. */
    fun canCancelAccount(): Boolean {
        val previous = lastAccountId ?: return false
        return previous != Accounts.currentId() && Accounts.list().any { it.id == previous }
    }

    /** Restores the account that was active before "add account" / a switch. */
    fun cancelAccountSwitch(): Boolean {
        val previous = lastAccountId ?: return false
        val created = pendingNewAccountId
        lastAccountId = null
        pendingNewAccountId = null
        if (created != null && created != previous) {
            Accounts.remove(created)
            appContext?.let { context ->
                scope.launch(Dispatchers.IO) {
                    runCatching { File(File(context.filesDir, "tdlib"), created).deleteRecursively() }
                }
            }
        }
        if (Accounts.list().none { it.id == previous }) return false
        Accounts.setCurrent(previous)
        resetClient()
        return true
    }

    private fun forceSwitchTo(id: String) {
        Accounts.setCurrent(id)
        lastAccountId = null
        pendingNewAccountId = null
        resetClient()
    }

    private fun resetClient() {
        generation.incrementAndGet()
        val old = client
        client = null
        synchronized(this) { starting = false }
        chatsLoaded = false
        _connected.value = false
        _me.value = null
        _auth.value = AuthState.Starting
        scope.launch { runCatching { old?.close() } }
        start()
    }

    fun start() {
        synchronized(this) {
            if (starting) return
            starting = true
        }
        scope.launch { runClient() }
    }

    /** Used by background workers that may start in a fresh process. */
    suspend fun awaitReady(timeoutMs: Long): Boolean {
        if (_auth.value is AuthState.Ready) return true
        return withTimeoutOrNull(timeoutMs) {
            auth.first { it is AuthState.Ready }
            true
        } ?: false
    }

    /** Call after a failed start (missing native library, no credentials, ...). */
    fun retry() {
        client = null
        synchronized(this) { starting = false }
        _auth.value = AuthState.Starting
        start()
    }

    private suspend fun runClient() {
        val gen = generation.get()
        try {
            val context = appContext ?: run {
                _auth.value = AuthState.Failed("应用尚未初始化")
                return
            }
            val td = TdlClient.create()
            if (gen != generation.get()) {
                runCatching { td.close() }
                return
            }
            client = td
            val jobs = listOf(
                scope.launch {
                    td.authorizationStateUpdates.collect {
                        if (gen == generation.get()) apply(td, it.authorizationState)
                    }
                },
                scope.launch {
                    td.connectionStateUpdates.collect {
                        if (gen == generation.get()) _connected.value = it.state is ConnectionStateReady
                    }
                },
            )

            runCatching { td.setLogVerbosityLevel(1) }
            when (val now = td.getAuthorizationState()) {
                is TdlResult.Success -> apply(td, now.result)
                is TdlResult.Failure -> Log.w(TAG, "getAuthorizationState failed: ${now.message}")
            }

            jobs.forEach { it.join() }
            kotlinx.coroutines.awaitCancellation()
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Log.e(TAG, "TDLib client could not start", t)
            client = null
            _connected.value = false
            _auth.value = AuthState.Failed(
                t.message?.takeIf { it.isNotBlank() } ?: "TDLib 无法启动（原生库或凭据缺失）",
            )
            starting = false
            val dirs = appContext
            Log.i(TAG, "files dir: ${dirs?.filesDir}")
        }
    }

    private suspend fun apply(td: TdlClient, state: AuthorizationState) {
        when (state) {
            is AuthorizationStateWaitTdlibParameters -> sendParameters(td)
            is AuthorizationStateWaitPhoneNumber -> _auth.value = AuthState.Phone()
            is AuthorizationStateWaitCode -> _auth.value = AuthState.Code()
            is AuthorizationStateWaitPassword -> _auth.value = AuthState.Password(state.passwordHint)
            is AuthorizationStateWaitOtherDeviceConfirmation -> _auth.value = AuthState.Qr(state.link)
            is AuthorizationStateReady -> {
                chatsLoaded = false
                pendingNewAccountId = null
                lastAccountId = null
                _auth.value = AuthState.Ready
                onReady(td)
            }
            is AuthorizationStateLoggingOut, is AuthorizationStateClosing -> _auth.value = AuthState.Starting
            is AuthorizationStateClosed -> _auth.value = AuthState.Starting
            else -> Unit
        }
    }

    private suspend fun sendParameters(td: TdlClient) {
        val context = appContext ?: return
        val apiId = BuildConfig.TG_API_ID
        val apiHash = BuildConfig.TG_API_HASH
        if (apiId == 0 || apiHash.isBlank()) {
            _auth.value = AuthState.Failed("未配置 API 凭据：请在 local.properties 填写 TG_API_ID / TG_API_HASH 后重新构建")
            return
        }
        val root = File(context.filesDir, Accounts.current()?.rootDir ?: "tdlib").apply { mkdirs() }
        val dbDir = File(root, "db").apply { mkdirs() }
        val filesDir = File(root, "files").apply { mkdirs() }

        val result = td.setTdlibParameters(
            useTestDc = false,
            databaseDirectory = dbDir.absolutePath,
            filesDirectory = filesDir.absolutePath,
            databaseEncryptionKey = ByteArray(0),
            useFileDatabase = true,
            useChatInfoDatabase = true,
            useMessageDatabase = true,
            useSecretChats = false,
            apiId = apiId,
            apiHash = apiHash,
            systemLanguageCode = "zh",
            deviceModel = Build.MODEL ?: "Android",
            systemVersion = Build.VERSION.RELEASE ?: "Android",
            applicationVersion = BuildConfig.VERSION_NAME,
        )
        if (result is TdlResult.Failure) {
            _auth.value = AuthState.Failed("TDLib 参数被拒绝：${result.message}")
        }
    }

    private fun currentNetworkType(): NetworkType {
        val manager = appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = manager?.getNetworkCapabilities(manager.activeNetwork)
        return when {
            caps == null -> NetworkTypeOther()
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTypeWiFi()
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTypeMobile()
            else -> NetworkTypeOther()
        }
    }

    private fun onReady(td: TdlClient) {
        scope.launch {
            runCatching { td.setNetworkType(currentNetworkType()) }
            runCatching { td.setOption("ignore_inline_thumbnails", dev.g000sha256.tdl.dto.OptionValueBoolean(false)) }
            runCatching { td.setOption("disable_animated_emoji", dev.g000sha256.tdl.dto.OptionValueBoolean(true)) }
            runCatching { td.setOption("ignore_background_updates", dev.g000sha256.tdl.dto.OptionValueBoolean(true)) }
            runCatching {
                td.getMe().valueOrNull?.let { user ->
                    val name = listOfNotNull(
                        user.firstName?.takeIf { it.isNotBlank() },
                        user.lastName?.takeIf { it.isNotBlank() },
                    ).joinToString(" ").ifBlank { "Telegram" }
                    _me.value = name
                    val currentId = Accounts.currentId()
                    val duplicate = Accounts.findByUserId(user.id)
                        ?.takeIf { it.id != currentId }
                    when {
                        duplicate == null -> Accounts.updateIdentity(currentId, user.id, name)
                        // The slot just logged in is a fresh one: drop it and reuse the old slot.
                        Accounts.current()?.rootDir != "tdlib" -> {
                            val stale = Accounts.current()
                            Accounts.remove(currentId)
                            appContext?.let { context ->
                                stale?.let { slot ->
                                    scope.launch(Dispatchers.IO) {
                                        runCatching { File(context.filesDir, slot.rootDir).deleteRecursively() }
                                    }
                                }
                            }
                            forceSwitchTo(duplicate.id)
                        }
                        // Current is the legacy default: drop the duplicate slot instead.
                        else -> {
                            Accounts.remove(duplicate.id)
                            appContext?.let { context ->
                                scope.launch(Dispatchers.IO) {
                                    runCatching { File(context.filesDir, duplicate.rootDir).deleteRecursively() }
                                }
                            }
                            Accounts.updateIdentity(currentId, user.id, name)
                        }
                    }
                }
            }
        }
    }

    // ---------------- sign in ----------------

    suspend fun submitQr(): String? = call { it.requestQrCodeAuthentication(longArrayOf()) }

    suspend fun submitPhone(phoneNumber: String): String? =
        call { it.setAuthenticationPhoneNumber(phoneNumber) }

    suspend fun submitCode(code: String): String? = call { it.checkAuthenticationCode(code) }

    suspend fun submitPassword(password: String): String? = call { it.checkAuthenticationPassword(password) }

    private suspend fun call(block: suspend (TdlClient) -> TdlResult<*>): String? {
        val td = client ?: return "正在连接 Telegram，请稍候"
        return try {
            val result = withTimeout(START_TIMEOUT_MS) { block(td) }
            when (result) {
                is TdlResult.Success -> null
                is TdlResult.Failure -> result.message
                else -> null
            }
        } catch (t: Throwable) {
            t.message?.takeIf { it.isNotBlank() } ?: "请求失败"
        }
    }

    // ---------------- chat and video queries ----------------

    suspend fun chats(limit: Int = 60): List<ChatSummary> = withContext(Dispatchers.IO) {
        val td = client ?: return@withContext emptyList()
        if (!chatsLoaded) {
            loadChatPages(td)
        }
        var main: dev.g000sha256.tdl.dto.Chats? = null
        var attempt = 0
        while (attempt < 5) {
            attempt++
            main = runCatching { td.getChats(ChatListMain(), limit).value() }.getOrNull()
            if (main != null && main.chatIds.isNotEmpty()) break
            if (attempt < 5) delay(600)
        }
        val chatList = main ?: run {
            _lastError.value = "读取聊天列表失败"
            return@withContext emptyList()
        }
        val archived = runCatching { td.getChats(ChatListArchive(), 20).valueOrNull }.getOrNull()
        val archivedIds = archived?.chatIds?.toList() ?: emptyList()
        val ids = (chatList.chatIds.toList() + archivedIds).distinct()
        if (ids.isNotEmpty()) chatsLoaded = true
        ids.mapNotNull { id ->
            runCatching { td.getChat(id).valueOrNull }.getOrNull()?.let { chat ->
                ChatSummary(
                    id = chat.id,
                    title = chat.title.ifBlank { "未命名聊天" },
                    photoFileId = chat.photo?.small?.id ?: 0,
                    miniThumbnail = chat.photo?.minithumbnail?.data,
                    lastDate = chat.lastMessage?.date ?: 0,
                    archived = id in archivedIds,
                    kind = kindOf(chat),
                )
            }
        }
    }

    /**
     * Videos in a chat, fetched the way the original client fetches them: three TDLib
     * searches (video messages, documents that look like video, and animations) run
     * concurrently, paginated through nextFromMessageId and merged by message id.
     * Scanning the plain message history misses videos that sit behind newer text.
     */
    suspend fun videos(chatId: Long, limit: Int = 200): List<MediaItem> = withContext(Dispatchers.IO) {
        val td = client ?: return@withContext emptyList()
        val pageSize = 40
        val pages = ((limit + pageSize - 1) / pageSize).coerceIn(1, 6)
        val filters = listOf(
            "video" to SearchMessagesFilterVideo(),
            "doc" to SearchMessagesFilterDocument(),
            "anim" to SearchMessagesFilterAnimation(),
        )
        val failures = java.util.Collections.synchronizedList(ArrayList<String>())
        val perFilter = coroutineScope {
            filters.map { (name, filter) ->
                async {
                    val found = LinkedHashMap<Long, MediaItem>()
                    var raw = 0
                    var from = 0L
                    var page = 0
                    while (page < pages) {
                        page++
                        val result = runCatching {
                            td.searchChatMessages(
                                chatId = chatId,
                                topicId = null,
                                query = "",
                                senderId = null,
                                fromMessageId = from,
                                offset = 0,
                                limit = pageSize,
                                filter = filter,
                            ).valueOrNull
                        }
                        val batch = result.getOrNull()
                        if (batch == null) {
                            result.exceptionOrNull()?.message?.let { failures.add(name + ": " + it) }
                            break
                        }
                        if (batch.messages.isEmpty()) break
                        raw += batch.messages.size
                        batch.messages.filterNotNull().forEach { message ->
                            runCatching { MediaMapper.fromMessage(message) }.getOrNull()
                                ?.let { found[it.messageId] = it }
                        }
                        val next = batch.nextFromMessageId
                        if (next == 0L || next == from) break
                        from = next
                    }
                    name to (found.values.toList() to raw)
                }
            }.awaitAll()
        }
        val merged = perFilter.flatMap { it.second.first }
            .distinctBy { it.messageId }
            .sortedByDescending { it.messageId }
            .take(limit)

        val result = if (merged.isNotEmpty()) merged else historyScan(td, chatId, limit)
        val counts = perFilter.joinToString(" / ") { it.first + " " + it.second.second }
        if (result.isEmpty()) {
            val reason = failures.firstOrNull()
            _lastError.value = "TDLib 返回 0 条（" + counts + "）" + if (reason != null) " · " + reason else ""
        }
        Log.d("Td", "videos(chat=" + chatId + ") -> " + result.size + " (" + counts + ")" + if (failures.isNotEmpty()) " err=" + failures else "")
        result
    }

    /** Last-resort fallback: read pages of plain history and filter locally. */
    private suspend fun historyScan(td: TdlClient, chatId: Long, limit: Int): List<MediaItem> {
        val out = LinkedHashMap<Long, MediaItem>()
        var from = 0L
        var page = 0
        while (page < 4 && out.size < limit) {
            page++
            val batch = runCatching {
                td.getChatHistory(chatId, from, 0, 100, false).valueOrNull
            }.getOrNull() ?: break
            if (batch.messages.isEmpty()) break
            batch.messages.filterNotNull().forEach { message ->
                runCatching { MediaMapper.fromMessage(message) }.getOrNull()?.let { out[it.messageId] = it }
            }
            val oldest = batch.messages.filterNotNull().minOfOrNull { it.id } ?: break
            if (oldest == from) break
            from = oldest
        }
        return out.values.sortedByDescending { it.messageId }.take(limit)
    }

    private fun kindOf(chat: dev.g000sha256.tdl.dto.Chat): ChatKind = when (val type = chat.type) {
        is ChatTypeSupergroup -> if (type.isChannel) ChatKind.Channel else ChatKind.Group
        is ChatTypeBasicGroup -> ChatKind.Group
        is ChatTypePrivate, is ChatTypeSecret -> ChatKind.Person
        else -> ChatKind.Other
    }

    /**
     * Pulls chat pages from the server before the local list is read. TDLib starts with
     * only the chats it already knows; loadChats returns 404 once everything is local,
     * which is the signal to stop, exactly as the original client does.
     */
    private suspend fun loadChatPages(td: TdlClient) {
        repeat(3) {
            val result = runCatching { td.loadChats(ChatListMain(), 100) }.getOrNull()
            if (result !is TdlResult.Success) return
        }
        repeat(2) {
            val result = runCatching { td.loadChats(ChatListArchive(), 100) }.getOrNull()
            if (result !is TdlResult.Success) return
        }
    }

    /**
     * Global search across every chat, the way the original does it: TDLib's own
     * searchMessages over the main list, with the same video/document/animation filters.
     * This also matches caption text, so "#tag" queries work.
     */
    suspend fun searchAllVideos(query: String, limit: Int = 120): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val td = client ?: return@withContext emptyList()
            val text = query.trim()
            if (text.isEmpty()) return@withContext emptyList()
            val filters = listOf(
                SearchMessagesFilterVideo(),
                SearchMessagesFilterDocument(),
                SearchMessagesFilterAnimation(),
            )
            val found = coroutineScope {
                filters.map { filter ->
                    async {
                        runCatching {
                            td.searchMessages(ChatListMain(), text, "", limit, filter, null, 0, 0)
                                .valueOrNull?.messages?.filterNotNull().orEmpty()
                        }.onFailure { _lastError.value = it.message }.getOrDefault(emptyList())
                    }
                }.awaitAll()
            }.flatten()
            found.mapNotNull { runCatching { MediaMapper.fromMessage(it) }.getOrNull() }
                .distinctBy { it.messageId }
                .sortedByDescending { it.messageId }
                .take(limit)
        }

    /** Chats whose title matches [query], optionally restricted to a chat kind. */
    suspend fun searchChats(query: String, limit: Int = 30): List<ChatSummary> = withContext(Dispatchers.IO) {
        val td = client ?: return@withContext emptyList()
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val ids = runCatching {
            td.searchChats(q, null, limit).valueOrNull?.chatIds?.toList()
        }.getOrNull() ?: chats(limit).filter { it.title.contains(q, ignoreCase = true) }.map { it.id }
        ids.mapNotNull { id ->
            runCatching { td.getChat(id).valueOrNull }.getOrNull()?.let { chat ->
                ChatSummary(
                    id = chat.id,
                    title = chat.title.ifBlank { "未命名聊天" },
                    photoFileId = chat.photo?.small?.id ?: 0,
                    miniThumbnail = chat.photo?.minithumbnail?.data,
                    lastDate = chat.lastMessage?.date ?: 0,
                    kind = kindOf(chat),
                )
            }
        }
    }

    /** Videos whose title matches [query], scanning a bounded set of chats. */
    suspend fun searchVideos(query: String, maxChats: Int = 12, perChat: Int = 120): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isEmpty()) return@withContext emptyList()
            val list = chats(maxChats)
            val found = coroutineScope {
                list.map { chat ->
                    async { runCatching { videos(chat.id, perChat) }.getOrDefault(emptyList()) }
                }.awaitAll()
            }.flatten()
            val needle = q.removePrefix("#")
            found.filter { item ->
                item.title.contains(needle, ignoreCase = true) ||
                    item.tags.any { tag -> tag.contains(needle, ignoreCase = true) }
            }.distinctBy { it.messageId }
        }

    /** A short, screenshot-friendly report of what TDLib is actually returning. */
    suspend fun diagnose(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("client=" + (client != null) + " connected=" + _connected.value)
        sb.appendLine("auth=" + _auth.value::class.simpleName)
        sb.appendLine("lastError=" + (_lastError.value ?: "-"))
        val list = runCatching { chats(5) }.getOrElse { emptyList() }
        sb.appendLine("chats=" + list.size)
        val td = client
        if (td == null) return@withContext sb.toString()
        list.take(3).forEach { chat ->
            sb.appendLine("chat " + chat.id + " " + chat.title)
            val kinds = listOf(
                "video" to SearchMessagesFilterVideo(),
                "doc" to SearchMessagesFilterDocument(),
                "anim" to SearchMessagesFilterAnimation(),
            )
            kinds.forEach { (name, filter) ->
                val res = runCatching {
                    td.searchChatMessages(
                        chatId = chat.id, topicId = null, query = "", senderId = null,
                        fromMessageId = 0, offset = 0, limit = 10, filter = filter,
                    ).valueOrNull
                }
                val batch = res.getOrNull()
                if (batch == null) {
                    sb.appendLine("  " + name + " FAIL " + (res.exceptionOrNull()?.message ?: "null"))
                } else {
                    val mapped = batch.messages.filterNotNull().count { MediaMapper.fromMessage(it) != null }
                    val first = batch.messages.filterNotNull().firstOrNull()?.content?.let { it::class.simpleName }
                    sb.appendLine("  " + name + " msgs=" + batch.messages.size + " mapped=" + mapped + " first=" + first)
                }
            }
        }
        sb.toString()
    }

    suspend fun chatTitle(chatId: Long): String? = withContext(Dispatchers.IO) {
        val td = client ?: return@withContext null
        runCatching { td.getChat(chatId).valueOrNull?.title }.getOrNull()
    }

    // ---------------- files ----------------

    suspend fun file(fileId: Int): TdFile? = withContext(Dispatchers.IO) {
        val td = client ?: return@withContext null
        runCatching {
            td.getFile(fileId).valueOrNull?.let { file ->
                val local = file.local
                TdFile(
                    id = file.id,
                    size = file.size,
                    expectedSize = file.expectedSize,
                    localPath = local?.path.orEmpty(),
                    downloadedPrefix = local?.downloadedPrefixSize ?: 0L,
                    downloadOffset = local?.downloadOffset ?: 0L,
                    completed = local?.isDownloadingCompleted == true,
                )
            }
        }.getOrNull()
    }

    suspend fun download(fileId: Int, offset: Long = 0L, limit: Long = 0L, priority: Int = 16): String? =
        withContext(Dispatchers.IO) {
            val td = client ?: return@withContext "未连接 Telegram"
            runCatching {
                td.downloadFile(
                    fileId = fileId,
                    priority = priority,
                    offset = offset,
                    limit = limit,
                    synchronous = false,
                ).errorMessage
            }.getOrNull()
        }

    suspend fun cancelDownload(fileId: Int) = withContext(Dispatchers.IO) {
        val td = client ?: return@withContext
        runCatching { td.cancelDownloadFile(fileId, onlyIfPending = false) }
        Unit
    }

    /** Downloads a whole (small) file such as a chat photo and returns its bytes. */
    suspend fun fileBytes(fileId: Int, timeoutMs: Long = 8_000L): ByteArray? {
        if (fileId == 0) return null
        download(fileId)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val f = file(fileId)
            if (f != null && f.localPath.isNotEmpty() && (f.downloadedPrefix > 0 || f.completed)) {
                val onDisk = File(f.localPath)
                if (onDisk.isFile && onDisk.length() > 0) return runCatching { onDisk.readBytes() }.getOrNull()
            }
            delay(120)
        }
        return null
    }

    /** Ends the Telegram session on this device; TDLib deletes the local database. */
    suspend fun logOut() {
        val td = client ?: return
        runCatching { td.logOut() }
    }

    fun close() {
        val current = client
        client = null
        scope.launch { runCatching { current?.close() } }
    }
}