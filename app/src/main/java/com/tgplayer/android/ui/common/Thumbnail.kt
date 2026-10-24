package com.tgplayer.android.ui.common

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.tgplayer.android.data.Td
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

/**
 * Two-level thumbnail cache (memory + disk) with a small concurrency budget and
 * prefetch, so poster rows decode once and re-appear instantly when scrolled.
 */
object Thumbnails {

    @Volatile
    var highQuality: Boolean = true

    private val maxBytes = (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(64L * 1024 * 1024)
    private val memory = object : LruCache<Int, ImageBitmap>(maxBytes.toInt()) {
        override fun sizeOf(key: Int, value: ImageBitmap): Int = value.width * value.height * 4
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val permits = Semaphore(3)

    fun cached(fileId: Int): ImageBitmap? = memory.get(fileId)

    private fun diskFile(context: Context, fileId: Int): File =
        File(File(context.cacheDir, "thumbs"), "$fileId.jpg")

    suspend fun load(context: Context, fileId: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        if (fileId == 0) return@withContext null
        memory.get(fileId)?.let { return@withContext it }
        val disk = diskFile(context, fileId)
        if (disk.isFile) {
            decodeScaled(runCatching { disk.readBytes() }.getOrNull())?.let {
                memory.put(fileId, it)
                return@withContext it
            }
        }
        val bytes = Td.fileBytes(fileId) ?: return@withContext null
        val bitmap = decodeScaled(bytes) ?: return@withContext null
        runCatching {
            disk.parentFile?.mkdirs()
            disk.writeBytes(bytes)
        }
        memory.put(fileId, bitmap)
        bitmap
    }

    fun prefetch(context: Context, ids: List<Int>) {
        ids.asSequence().filter { it != 0 }.distinct().take(24).forEach { id ->
            scope.launch {
                permits.withPermit { runCatching { load(context, id) } }
            }
        }
    }

    fun clearDisk(context: Context) {
        memory.evictAll()
        runCatching { File(context.cacheDir, "thumbs").deleteRecursively() }
    }

    private fun decodeScaled(bytes: ByteArray?): ImageBitmap? {
        if (bytes == null || bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > if (highQuality) 720 else 360) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
fun Thumbnail(fileId: Int, modifier: Modifier = Modifier, seed: Int = 0) {
    val context = LocalContext.current
    var image by remember(fileId) { mutableStateOf(Thumbnails.cached(fileId)) }
    LaunchedEffect(fileId) {
        if (fileId == 0 || image != null) return@LaunchedEffect
        image = Thumbnails.load(context, fileId)
    }
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(gradient(seed), gradient(seed + 1))),
        ),
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun gradient(seed: Int): Color {
    val palette = listOf(
        Color(0xFF1E3A46), Color(0xFF101820), Color(0xFF3C2A22), Color(0xFF1A1210),
        Color(0xFF2A2A52), Color(0xFF12121F), Color(0xFF1F3B37), Color(0xFF0F1A18),
    )
    return palette[((seed % palette.size) + palette.size) % palette.size]
}
