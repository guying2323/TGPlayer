package com.tgplayer.android.data

import androidx.compose.runtime.Immutable

/** What the sign-in surface should show right now. */
sealed interface AuthState {
    data object Starting : AuthState
    data class Qr(val link: String) : AuthState
    data class Phone(val wrong: Boolean = false) : AuthState
    data class Code(val wrong: Boolean = false) : AuthState
    data class Password(val hint: String? = null, val wrong: Boolean = false) : AuthState
    data object Ready : AuthState
    data class Failed(val message: String) : AuthState
}

/**
 * One row of the chat list. [photoFileId] is the small chat photo TDLib keeps
 * for the chat; 0 means the chat has no photo and the UI falls back to initials.
 */
enum class ChatKind { Channel, Group, Person, Other }

@Immutable
data class ChatSummary(
    val id: Long,
    val title: String,
    val photoFileId: Int = 0,
    val miniThumbnail: ByteArray? = null,
    val lastDate: Int = 0,
    val archived: Boolean = false,
    val kind: ChatKind = ChatKind.Other,
)

/** A playable video found in a Telegram message. */
@Immutable
data class MediaItem(
    val chatId: Long,
    val messageId: Long,
    val fileId: Int,
    val title: String,
    val fileName: String,
    val sizeBytes: Long,
    val durationSec: Int,
    val mimeType: String,
    val thumbnailFileId: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val date: Int = 0,
    val canBeSaved: Boolean = true,
    /** Lower-case hashtags from the caption, without the leading '#'. */
    val tags: List<String> = emptyList(),
)

/** The part of TDLib's file object playback and thumbnails need. */
data class TdFile(
    val id: Int,
    val size: Long,
    val expectedSize: Long,
    val localPath: String,
    val downloadedPrefix: Long,
    val downloadOffset: Long,
    val completed: Boolean,
) {
    val totalSize: Long get() = if (size > 0) size else expectedSize
}
