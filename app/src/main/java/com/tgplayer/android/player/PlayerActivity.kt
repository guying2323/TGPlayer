package com.tgplayer.android.player

import android.net.Uri
import android.os.Bundle
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.activity.ComponentActivity
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgplayer.android.data.Downloads
import com.tgplayer.android.data.Favorites
import com.tgplayer.android.data.MediaItem
import com.tgplayer.android.ui.theme.TmPlayerTheme

class PlayerActivity : ComponentActivity() {

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _pip.value = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val chatId = intent.getLongExtra(EXTRA_CHAT_ID, 0L)
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, 0L)
        val fileId = intent.getIntExtra(EXTRA_FILE_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val uri = intent.getStringExtra(EXTRA_URI)?.let(Uri::parse)
        val chatTitle = intent.getStringExtra(EXTRA_CHAT_TITLE).orEmpty()
        val thumbnailFileId = intent.getIntExtra(EXTRA_THUMBNAIL, 0)
        val playlistFiles = intent.getIntArrayExtra(EXTRA_PLAYLIST_FILES) ?: intArrayOf()
        val playlistMessages = intent.getLongArrayExtra(EXTRA_PLAYLIST_MESSAGES) ?: longArrayOf()
        val playlistTitles = intent.getStringArrayExtra(EXTRA_PLAYLIST_TITLES) ?: arrayOf()
        val playlistThumbs = intent.getIntArrayExtra(EXTRA_PLAYLIST_THUMBS) ?: intArrayOf()
        val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)

        setContent {
            TmPlayerTheme {
                val favorites by Favorites.videos.collectAsStateWithLifecycle()
                val hasPlaylist = playlistFiles.isNotEmpty()
                var index by remember {
                    mutableIntStateOf(
                        if (hasPlaylist) startIndex.coerceIn(0, playlistFiles.lastIndex) else 0,
                    )
                }
                val currentFile = if (hasPlaylist) playlistFiles[index] else fileId
                val currentMessage = if (hasPlaylist) playlistMessages.getOrElse(index) { messageId } else messageId
                val currentTitle = if (hasPlaylist) playlistTitles.getOrElse(index) { title } else title
                val currentThumb = if (hasPlaylist) playlistThumbs.getOrElse(index) { thumbnailFileId } else thumbnailFileId
                PlayerScreen(
                    chatId = chatId,
                    messageId = currentMessage,
                    fileId = currentFile,
                    uri = uri,
                    title = currentTitle,
                    chatTitle = chatTitle,
                    thumbnailFileId = currentThumb,
                    onClose = { finish() },
                    onEnded = { if (hasPlaylist && index < playlistFiles.lastIndex) index += 1 },
                    isFavorite = favorites.any { it.chatId == chatId && it.messageId == currentMessage },
                    onFavorite = if (currentFile != 0) {
                        {
                            Favorites.toggleVideo(
                                Favorites.Video(
                                    chatId = chatId,
                                    messageId = currentMessage,
                                    fileId = currentFile,
                                    title = currentTitle,
                                    chatTitle = chatTitle,
                                    thumbnailFileId = currentThumb,
                                ),
                            )
                            Toast.makeText(this@PlayerActivity, "收藏已更新", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        null
                    },
                    onDownload = if (currentFile != 0) {
                        {
                            Downloads.enqueue(
                                MediaItem(
                                    chatId = chatId,
                                    messageId = currentMessage,
                                    fileId = currentFile,
                                    title = currentTitle,
                                    fileName = "",
                                    sizeBytes = 0L,
                                    durationSec = 0,
                                    mimeType = "",
                                    thumbnailFileId = currentThumb,
                                ),
                            )
                            Toast.makeText(this@PlayerActivity, "已加入下载队列", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        null
                    },
                    onNext = if (hasPlaylist && index < playlistFiles.lastIndex) {
                        { index += 1 }
                    } else {
                        null
                    },
                )
            }
        }
    }

    companion object {
        private val _pip = MutableStateFlow(false)
        val pip: StateFlow<Boolean> = _pip.asStateFlow()

        fun enterPip(activity: android.app.Activity) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                runCatching {
                    activity.enterPictureInPictureMode(
                        PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build(),
                    )
                }
            }
        }
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_FILE_ID = "file_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_URI = "uri"
        const val EXTRA_CHAT_TITLE = "chat_title"
        const val EXTRA_THUMBNAIL = "thumbnail"
        const val EXTRA_PLAYLIST_FILES = "playlist_files"
        const val EXTRA_PLAYLIST_MESSAGES = "playlist_messages"
        const val EXTRA_PLAYLIST_TITLES = "playlist_titles"
        const val EXTRA_PLAYLIST_THUMBS = "playlist_thumbs"
        const val EXTRA_START_INDEX = "playlist_index"
    }
}
