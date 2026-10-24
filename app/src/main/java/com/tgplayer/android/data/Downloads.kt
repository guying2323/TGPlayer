package com.tgplayer.android.data

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Keys shared with [DownloadWorker]. */
object DownloadContract {
    const val KEY_FILE_ID = "file_id"
    const val KEY_MESSAGE_ID = "message_id"
    const val KEY_TITLE = "title"
    const val KEY_CHAT_TITLE = "chat_title"
    const val KEY_DOWNLOADED = "downloaded"
    const val KEY_TOTAL = "total"
    const val KEY_PATH = "path"
    const val KEY_SPEED = "speed"
    const val TAG = "tm_download"

    fun workName(fileId: Int) = "tm_download_" + fileId
}

/**
 * Background download queue on WorkManager (foreground worker + notification),
 * modelled on NextPlayer's DownloadWorker/DownloadsRepository and Seal's task
 * list. TDLib still does the transfer; the worker survives the UI process.
 */
object Downloads {

    enum class State { Queued, Downloading, Paused, Done, Failed }

    data class Task(
        val messageId: Long,
        val chatId: Long,
        val fileId: Int,
        val title: String,
        val chatTitle: String,
        val totalBytes: Long = 0L,
        val downloadedBytes: Long = 0L,
        val speedBytesPerSec: Long = 0L,
        val state: State = State.Queued,
        val path: String = "",
        val error: String? = null,
    ) {
        val progress: Float
            get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    }

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks = _tasks

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val observed = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    /** At most two Telegram transfers at once, so one file does not crawl behind a large queue. */
    val workerPermits = kotlinx.coroutines.sync.Semaphore(2)

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        _tasks.value = loadTasks().map {
            if (it.state == State.Downloading) it.copy(state = State.Queued) else it
        }
        _tasks.value.forEach { observe(it.fileId) }
        // Auto-resume tasks that were queued before the process died. Paused and
        // finished tasks stay put.
        _tasks.value.filter { it.state == State.Queued }.forEach { autoResume(it) }
    }

    private fun autoResume(task: Task) {
        if (appContext == null) return
        startWork(task)
    }

    fun enqueue(item: MediaItem) {
        if (_tasks.value.any { it.fileId == item.fileId }) return
        val task = Task(
            messageId = item.messageId,
            chatId = item.chatId,
            fileId = item.fileId,
            title = item.title,
            chatTitle = "",
        )
        _tasks.value = listOf(task) + _tasks.value
        save()
        startWork(task)
    }

    private fun startWork(task: Task) {
        val context = appContext ?: return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (Settings.get().wifiOnlyDownloads) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadContract.KEY_FILE_ID to task.fileId,
                    DownloadContract.KEY_MESSAGE_ID to task.messageId,
                    DownloadContract.KEY_TITLE to task.title,
                    DownloadContract.KEY_CHAT_TITLE to task.chatTitle,
                ),
            )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(DownloadContract.TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(DownloadContract.workName(task.fileId), ExistingWorkPolicy.REPLACE, request)
        observe(task.fileId)
    }

    private fun observe(fileId: Int) {
        if (!observed.add(fileId)) return
        val context = appContext ?: return
        scope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(DownloadContract.workName(fileId))
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    applyWorkInfo(fileId, info)
                }
        }
    }

    private fun applyWorkInfo(fileId: Int, info: WorkInfo) {
        val downloaded = info.progress.getLong(DownloadContract.KEY_DOWNLOADED, 0L)
        val total = info.progress.getLong(DownloadContract.KEY_TOTAL, 0L)
        val outDownloaded = info.outputData.getLong(DownloadContract.KEY_DOWNLOADED, 0L)
        val outTotal = info.outputData.getLong(DownloadContract.KEY_TOTAL, 0L)
        val speed = info.progress.getLong(DownloadContract.KEY_SPEED, 0L)
        val path = info.outputData.getString(DownloadContract.KEY_PATH)
        update(fileId) { task ->
            when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                    task.copy(state = State.Queued, error = null)
                WorkInfo.State.RUNNING ->
                    task.copy(state = State.Downloading, downloadedBytes = downloaded, totalBytes = total, speedBytesPerSec = speed, error = null)
                WorkInfo.State.SUCCEEDED ->
                    task.copy(
                        state = State.Done,
                        downloadedBytes = outDownloaded,
                        totalBytes = outTotal,
                        path = path ?: task.path,
                    )
                WorkInfo.State.FAILED ->
                    task.copy(state = State.Failed, error = "下载失败")
                WorkInfo.State.CANCELLED ->
                    task.copy(state = State.Paused)
            }
        }
        save()
    }

    fun pause(task: Task) {
        val context = appContext ?: return
        WorkManager.getInstance(context).cancelUniqueWork(DownloadContract.workName(task.fileId))
        update(task.fileId) { it.copy(state = State.Paused) }
        save()
    }

    fun resume(task: Task) {
        update(task.fileId) { it.copy(state = State.Queued, error = null) }
        save()
        startWork(task)
    }

    fun remove(task: Task) {
        val context = appContext ?: return
        WorkManager.getInstance(context).cancelUniqueWork(DownloadContract.workName(task.fileId))
        if (task.path.isNotBlank()) runCatching { File(task.path).delete() }
        _tasks.value = _tasks.value.filterNot { it.fileId == task.fileId }
        observed.remove(task.fileId)
        save()
    }

    fun open(context: Context, task: Task): Intent? {
        if (task.path.isBlank()) return null
        val file = File(task.path)
        if (!file.isFile) return null
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        return Intent(context, com.tgplayer.android.player.PlayerActivity::class.java).apply {
            putExtra(com.tgplayer.android.player.PlayerActivity.EXTRA_URI, uri.toString())
            putExtra(com.tgplayer.android.player.PlayerActivity.EXTRA_TITLE, task.title)
            putExtra(com.tgplayer.android.player.PlayerActivity.EXTRA_CHAT_TITLE, "下载")
            putExtra(com.tgplayer.android.player.PlayerActivity.EXTRA_MESSAGE_ID, task.messageId)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Copies TDLib's finished file into the app's Downloads folder. */
    fun copyToDownloads(context: Context, fileId: Int, title: String, localPath: String): String? {
        if (localPath.isBlank()) return null
        val source = File(localPath)
        if (!source.isFile) return null
        val dir = context.getExternalFilesDir("Downloads") ?: File(context.filesDir, "Downloads")
        if (!dir.exists()) dir.mkdirs()
        val ext = source.extension.takeIf { it.isNotBlank() } ?: "mp4"
        val base = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "video" }.take(80)
        var target = File(dir, base + "." + ext)
        if (target.exists()) target = File(dir, base + "_" + fileId + "." + ext)
        return runCatching { source.copyTo(target, overwrite = true).absolutePath }.getOrNull()
    }

    fun usedBytes(): Long {
        val context = appContext ?: return 0L
        val dir = context.getExternalFilesDir("Downloads") ?: return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clearFinished() {
        _tasks.value = _tasks.value.filterNot { it.state == State.Done || it.state == State.Failed }
        save()
    }

    private fun update(fileId: Int, block: (Task) -> Task) {
        _tasks.value = _tasks.value.map { if (it.fileId == fileId) block(it) else it }
    }

    private fun save() {
        val prefs = appContext?.getSharedPreferences("tgplayer", Context.MODE_PRIVATE) ?: return
        val array = JSONArray()
        _tasks.value.forEach { t ->
            array.put(JSONObject().apply {
                put("messageId", t.messageId)
                put("chatId", t.chatId)
                put("fileId", t.fileId)
                put("title", t.title)
                put("chatTitle", t.chatTitle)
                put("totalBytes", t.totalBytes)
                put("downloadedBytes", t.downloadedBytes)
                put("speed", t.speedBytesPerSec)
                put("state", t.state.name)
                put("path", t.path)
                put("error", t.error)
            })
        }
        prefs.edit().putString("downloads", array.toString()).apply()
    }

    private fun loadTasks(): List<Task> {
        val context = appContext ?: return emptyList()
        val prefs = context.getSharedPreferences("tgplayer", Context.MODE_PRIVATE)
        val raw = prefs.getString("downloads", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                Task(
                    messageId = o.optLong("messageId"),
                    chatId = o.optLong("chatId"),
                    fileId = o.optInt("fileId"),
                    title = o.optString("title"),
                    chatTitle = o.optString("chatTitle"),
                    totalBytes = o.optLong("totalBytes"),
                    downloadedBytes = o.optLong("downloadedBytes"),
                    speedBytesPerSec = o.optLong("speed"),
                    state = runCatching { State.valueOf(o.optString("state")) }.getOrDefault(State.Queued),
                    path = o.optString("path"),
                    error = o.optString("error").ifBlank { null },
                )
            }
        }.getOrDefault(emptyList())
    }
}
