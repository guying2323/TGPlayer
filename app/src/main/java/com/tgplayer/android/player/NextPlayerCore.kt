package com.tgplayer.android.player

import android.app.ActivityManager
import android.content.Context
import com.tgplayer.android.BuildConfig
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

/**
 * Playback core adapted from NextPlayer
 * (https://github.com/anilbeesetti/nextplayer, GPL-3.0, commit e1f406f59288439cdef1607522bc4e2a8116c6d0).
 *
 * ExoPlayer wiring mirrors PlayerService: NextRenderersFactory with FFmpeg
 * fallback, DefaultTrackSelector, movie audio attributes with focus, noisy
 * handling, pause-at-end, wake mode. Track switching and side-loaded
 * subtitles follow Player.switchTrack / addAdditionalSubtitleConfiguration.
 */
@OptIn(UnstableApi::class)
class NextPlayerCore(context: Context) {

    private val appContext = context.applicationContext

    private lateinit var trackSelector: DefaultTrackSelector

    val player: ExoPlayer = build()

    var onError: ((String) -> Unit)? = null

    private var currentFileId: Int = 0
    private var currentLocalUri: Uri? = null
    private val subtitleUris = mutableListOf<Uri>()

    data class TrackOption(
        val trackType: Int,
        val index: Int,
        val label: String,
        val selected: Boolean,
    )

    private fun build(): ExoPlayer {
        val renderers = NextRenderersFactory(appContext)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        trackSelector = DefaultTrackSelector(appContext).apply {
            setParameters(
                buildUponParameters()
                    .setTunnelingEnabled(false)
                    .setPreferredAudioLanguage(DEFAULT_LANGUAGE)
                    .setPreferredTextLanguage(DEFAULT_LANGUAGE)
                    .build(),
            )
        }

        // Low-RAM devices get a smaller ceiling; the rest keep a longer runway.
        val lowRam = (appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
        val maxBuffer = if (lowRam) 60_000 else 120_000
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, maxBuffer, 2_500, 8_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(appContext, renderers)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                pauseAtEndOfMediaItems = true
                repeatMode = Player.REPEAT_MODE_OFF
                setWakeMode(C.WAKE_MODE_LOCAL)
                setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        onError?.invoke(error.message ?: "播放失败")
                    }
                })
                if (BuildConfig.DEBUG) addAnalyticsListener(EventLogger("NextPlayerCore"))
            }
    }

    fun open(fileId: Int, startPositionMs: Long, playWhenReady: Boolean) {
        currentFileId = fileId
        currentLocalUri = null
        subtitleUris.clear()
        prepare(startPositionMs, playWhenReady)
    }

    /** Local file / SAF pipeline, as NextPlayer opens device media. */
    fun openLocal(uri: Uri, startPositionMs: Long, playWhenReady: Boolean) {
        currentFileId = 0
        currentLocalUri = uri
        subtitleUris.clear()
        prepare(startPositionMs, playWhenReady)
    }

    private fun prepare(startPositionMs: Long, playWhenReady: Boolean) {
        val uri = currentLocalUri ?: TdDataSource.uriFor(currentFileId)
        val builder = MediaItem.Builder().setUri(uri)
        if (subtitleUris.isNotEmpty()) {
            builder.setSubtitleConfigurations(
                subtitleUris.map { subtitleUri ->
                    val extension = subtitleUri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
                    val mime = when (extension) {
                        "vtt" -> MimeTypes.TEXT_VTT
                        "ass", "ssa" -> MimeTypes.TEXT_SSA
                        else -> MimeTypes.APPLICATION_SUBRIP
                    }
                    MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                        .setMimeType(mime)
                        .setLanguage("und")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                },
            )
        }
        val item = builder.build()
        val source: MediaSource = if (currentLocalUri != null) {
            DefaultMediaSourceFactory(appContext).createMediaSource(item)
        } else {
            ProgressiveMediaSource.Factory(
                DefaultDataSource.Factory(appContext, TdDataSource.Factory(currentFileId)),
            ).createMediaSource(item)
        }
        player.setMediaSource(source)
        if (startPositionMs > 3_000) player.seekTo(startPositionMs)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    fun addSubtitle(uri: Uri) {
        subtitleUris.removeAll { it == uri }
        subtitleUris += uri
        prepare(player.currentPosition, player.playWhenReady)
    }

    fun audioTracks(): List<TrackOption> = tracksOf(C.TRACK_TYPE_AUDIO)

    fun textTracks(): List<TrackOption> = tracksOf(C.TRACK_TYPE_TEXT)

    private fun tracksOf(type: Int): List<TrackOption> {
        val groups = player.currentTracks.groups.filter { it.type == type }
        return groups.mapIndexed { index, group ->
            val format = group.getTrackFormat(0)
            val label = format.label
                ?: format.language?.uppercase()
                ?: (if (type == C.TRACK_TYPE_AUDIO) "音轨 " else "字幕 ") + (index + 1)
            TrackOption(type, index, label, group.isTrackSelected(0))
        }
    }

    /** Mirrors NextPlayer's Player.switchTrack; a negative index disables the type. */
    fun switchTrack(trackType: Int, trackIndex: Int) {
        val parameters = player.trackSelectionParameters
        player.trackSelectionParameters = if (trackIndex < 0) {
            parameters.buildUpon().setTrackTypeDisabled(trackType, true).build()
        } else {
            val groups = player.currentTracks.groups.filter { it.type == trackType }
            if (trackIndex >= groups.size) return
            parameters.buildUpon()
                .setTrackTypeDisabled(trackType, false)
                .setOverrideForType(TrackSelectionOverride(groups[trackIndex].mediaTrackGroup, 0))
                .build()
        }
    }

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs)
            .coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
    }

    fun seekToFraction(fraction: Float) {
        val duration = player.duration
        if (duration > 0) player.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun setSpeed(speed: Float) = player.setPlaybackSpeed(speed)

    data class Snapshot(
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val bufferedMs: Long,
        val speed: Float,
        val isBuffering: Boolean,
        val ended: Boolean,
    )

    fun snapshot(): Snapshot = Snapshot(
        isPlaying = player.isPlaying,
        positionMs = player.currentPosition.coerceAtLeast(0L),
        durationMs = player.duration.coerceAtLeast(0L),
        bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
        speed = player.playbackParameters.speed,
        isBuffering = player.playbackState == Player.STATE_BUFFERING,
        ended = player.playbackState == Player.STATE_ENDED,
    )

    fun release() = player.release()

    private companion object {
        const val DEFAULT_LANGUAGE = "und"
    }
}
