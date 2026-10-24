package com.tgplayer.android.player

import com.tgplayer.android.data.Td
import kotlinx.coroutines.delay
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Streaming window over TDLib's partial file.
 *
 * The first read aims TDLib at the requested byte with high priority and then
 * lets it download contiguously; we only re-aim on a real seek (position far
 * outside the downloaded prefix). Reads wait for the prefix to cover them and
 * reuse one RandomAccessFile, which keeps ExoPlayer's thread from stalling on
 * per-read file opens.
 */
class TdByteWindow(private val fileId: Int) {

    private var fileSize = 0L
    private var requestedAt = -1L
    private var closed = false
    private var raf: RandomAccessFile? = null
    private var rafPath: String? = null

    suspend fun open(position: Long): Long {
        closed = false
        val file = Td.file(fileId) ?: throw IOException("文件不可用")
        fileSize = file.totalSize
        if (fileSize <= 0) throw IOException("文件大小未知")
        val seek = position.coerceIn(0L, fileSize - 1)
        requestFrom(seek, file)
        return fileSize - position
    }

    private suspend fun requestFrom(position: Long, current: com.tgplayer.android.data.TdFile?) {
        requestedAt = position
        Td.cancelDownload(fileId)
        Td.download(fileId, offset = position, priority = 32)
    }

    suspend fun read(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
        if (closed) throw IOException("窗口已关闭")
        val deadline = System.currentTimeMillis() + 45_000
        var file = Td.file(fileId) ?: throw IOException("文件不可用")
        var lastFileRefresh = System.currentTimeMillis()
        while (true) {
            val start = file.downloadOffset
            val end = start + file.downloadedPrefix
            val farBehind = position < start
            val farAhead = position > end + LOOKAHEAD_SLACK_BYTES
            if ((farBehind || farAhead) && requestedAt != position) {
                requestFrom(position, file)
            }
            val ready = position in start until end || (file.completed && position < file.totalSize)
            if (ready) {
                return readFromDisk(position, buffer, offset, length, file)
            }
            val now = System.currentTimeMillis()
            if (now > deadline) throw IOException("Telegram 没有送来数据")
            delay(POLL_MS)
            if (now - lastFileRefresh > FILE_REFRESH_MS) {
                lastFileRefresh = now
                file = Td.file(fileId) ?: throw IOException("文件不可用")
            }
        }
    }

    private fun readFromDisk(
        position: Long,
        buffer: ByteArray,
        offset: Int,
        length: Int,
        file: com.tgplayer.android.data.TdFile,
    ): Int {
        if (rafPath != file.localPath) {
            raf?.close()
            raf = RandomAccessFile(file.localPath, "r")
            rafPath = file.localPath
        }
        val input = raf ?: throw IOException("文件不可用")
        input.seek(position)
        val available = (file.downloadOffset + file.downloadedPrefix - position).toInt()
        return input.read(buffer, offset, minOf(length, maxOf(available, 0)))
    }

    suspend fun close() {
        closed = true
        runCatching { raf?.close() }
        raf = null
        rafPath = null
    }

    private companion object {
        const val POLL_MS = 120L
        const val FILE_REFRESH_MS = 300L

        /** Re-aim only when the reader is more than this far past the prefix. */
        const val LOOKAHEAD_SLACK_BYTES = 8L * 1024 * 1024
    }
}
