package com.tgplayer.android.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import kotlinx.coroutines.runBlocking

/** Media3 data source over [TdByteWindow]; the URI carries the TDLib file id. */
@OptIn(UnstableApi::class)
class TdDataSource(private val fileId: Int) : BaseDataSource(true) {

    private var window: TdByteWindow? = null
    private var readPosition = 0L
    private var bytesRemaining = 0L
    private var uri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        uri = dataSpec.uri
        val w = TdByteWindow(fileId)
        readPosition = dataSpec.position
        bytesRemaining = runBlocking { w.open(dataSpec.position) }
        window = w
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        val n = runBlocking { window?.read(readPosition, buffer, offset, toRead) ?: -1 }
        if (n < 0) return C.RESULT_END_OF_INPUT
        readPosition += n
        bytesRemaining -= n
        bytesTransferred(n)
        return n
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        val w = window
        window = null
        if (w != null) runBlocking { runCatching { w.close() } }
        transferEnded()
    }

    @OptIn(UnstableApi::class)
    class Factory(private val fileId: Int) : DataSource.Factory {
        override fun createDataSource(): DataSource = TdDataSource(fileId)
    }

    companion object {
        fun uriFor(fileId: Int): Uri = Uri.parse("tdfile://$fileId")

        fun fileIdOf(uri: Uri): Int = uri.host?.toIntOrNull() ?: uri.toString().removePrefix("tdfile://").toInt()
    }
}
