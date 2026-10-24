package com.tgplayer.android.data

import dev.g000sha256.tdl.dto.Message
import dev.g000sha256.tdl.dto.MessageAnimation
import dev.g000sha256.tdl.dto.MessageDocument
import dev.g000sha256.tdl.dto.MessageVideo
import dev.g000sha256.tdl.dto.MessageVideoNote

/**
 * Turns Telegram messages into playable entries, dropping everything that is
 * not a video. Videos arrive both as videos and as documents, so both are read.
 */
object MediaMapper {

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "mov", "m4v", "avi", "ts", "flv", "3gp", "m2ts",
    )

    fun fromMessage(message: Message): MediaItem? {
        if (message.selfDestructType != null) return null
        return when (val content = message.content) {
            is MessageVideo -> video(message, content)
            is MessageDocument -> document(message, content)
            is MessageAnimation -> animation(message, content)
            is MessageVideoNote -> videoNote(message, content)
            else -> null
        }
    }

    private fun video(message: Message, content: MessageVideo): MediaItem? {
        val video = content.video
        return MediaItem(
            chatId = message.chatId,
            messageId = message.id,
            fileId = video.video.id,
            title = titleOf(video.fileName, content.caption?.text),
            fileName = video.fileName.orEmpty(),
            sizeBytes = sizeOf(video.video.size, video.video.expectedSize),
            durationSec = video.duration,
            mimeType = video.mimeType.orEmpty(),
            thumbnailFileId = video.thumbnail?.file?.id ?: 0,
            width = video.width,
            height = video.height,
            date = message.date,
            canBeSaved = message.canBeSaved,
            tags = tagsOf(content.caption?.text),
        )
    }

    private fun document(message: Message, content: MessageDocument): MediaItem? {
        val document = content.document
        val name = document.fileName.orEmpty()
        val mime = document.mimeType.orEmpty()
        if (!looksLikeVideo(name, mime)) return null
        return MediaItem(
            chatId = message.chatId,
            messageId = message.id,
            fileId = document.document.id,
            title = titleOf(name, content.caption?.text),
            fileName = name,
            sizeBytes = sizeOf(document.document.size, document.document.expectedSize),
            durationSec = 0,
            mimeType = mime,
            thumbnailFileId = document.thumbnail?.file?.id ?: 0,
            date = message.date,
            canBeSaved = message.canBeSaved,
            tags = tagsOf(content.caption?.text),
        )
    }

    private fun animation(message: Message, content: MessageAnimation): MediaItem {
        val animation = content.animation
        return MediaItem(
            chatId = message.chatId,
            messageId = message.id,
            fileId = animation.animation.id,
            title = titleOf(animation.fileName, content.caption?.text),
            fileName = animation.fileName.orEmpty(),
            sizeBytes = sizeOf(animation.animation.size, animation.animation.expectedSize),
            durationSec = animation.duration,
            mimeType = animation.mimeType.orEmpty(),
            thumbnailFileId = animation.thumbnail?.file?.id ?: 0,
            width = animation.width,
            height = animation.height,
            date = message.date,
            canBeSaved = message.canBeSaved,
            tags = tagsOf(content.caption?.text),
        )
    }

    private fun videoNote(message: Message, content: MessageVideoNote): MediaItem? {
        val note = content.videoNote
        if (note.video.id == 0) return null
        return MediaItem(
            chatId = message.chatId,
            messageId = message.id,
            fileId = note.video.id,
            title = "视频消息",
            fileName = "",
            sizeBytes = sizeOf(note.video.size, note.video.expectedSize),
            durationSec = note.duration,
            mimeType = "video/mp4",
            thumbnailFileId = note.thumbnail?.file?.id ?: 0,
            width = note.length,
            height = note.length,
            date = message.date,
            canBeSaved = message.canBeSaved,
        )
    }

    private val HASHTAG = Regex("#([\\p{L}\\p{N}_]+)")

    /** Hashtags a viewer typed in the caption, de-duplicated and lower-cased. */
    internal fun tagsOf(text: String?): List<String> {
        val source = text ?: return emptyList()
        return HASHTAG.findAll(source)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .take(8)
            .toList()
    }

    private fun looksLikeVideo(name: String, mime: String): Boolean {
        if (mime.startsWith("video/")) return true
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTENSIONS
    }

    private fun titleOf(fileName: String?, caption: String?): String =
        caption?.trim()?.takeIf { it.isNotEmpty() }
            ?: fileName?.trim()?.takeIf { it.isNotEmpty() }
            ?: "未命名视频"

    private fun sizeOf(size: Long, expected: Long): Long = if (size > 0) size else expected
}
