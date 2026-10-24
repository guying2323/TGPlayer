package com.tgplayer.android.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tgplayer.android.data.Settings
import com.tgplayer.android.ui.theme.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Player controls ported from NextPlayer's player layout: ControlsTopView,
 * ControlsMiddleView and ControlsBottomView, plus its SimpleSlider seekbar
 * (4dp inactive track, white 16dp thumb, primary played range) and the
 * double-tap / vertical gesture indicators. Playback stays on NextPlayerCore.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    chatId: Long,
    messageId: Long,
    fileId: Int = 0,
    uri: Uri? = null,
    title: String,
    chatTitle: String = "",
    thumbnailFileId: Int = 0,
    onClose: () -> Unit,
    onEnded: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onFavorite: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val settings = remember { Settings.install(context) }
    val core = remember { NextPlayerCore(context) }
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }

    var snapshot by remember { mutableStateOf(core.snapshot()) }
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var showUnlock by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var seekHint by remember { mutableStateOf(0) }
    var dragSide by remember { mutableStateOf(0) }
    var volumeHint by remember { mutableStateOf<Float?>(null) }
    var brightnessHint by remember { mutableStateOf<Float?>(null) }
    var volumeLevel by remember { mutableFloatStateOf(-1f) }
    var trackDialog by remember { mutableStateOf(0) }
    var scaleMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    val inPip by PlayerActivity.pip.collectAsStateWithLifecycle()

    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(picked, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            core.addSubtitle(picked)
            trackDialog = 0
        }
    }

    fun changeVolume(delta: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        if (volumeLevel < 0f) volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volumeLevel * max).roundToInt(), 0)
        volumeHint = volumeLevel
    }

    fun changeBrightness(delta: Float) {
        val window = activity?.window ?: return
        val attrs = window.attributes
        val current = if (attrs.screenBrightness < 0f) 0.5f else attrs.screenBrightness
        val next = (current + delta).coerceIn(0.05f, 1f)
        attrs.screenBrightness = next
        window.attributes = attrs
        brightnessHint = next
    }

    LaunchedEffect(core, fileId, messageId, uri) {
        error = null
        core.onError = { error = it }
        val resume = settings.resumeMs(chatId, messageId)
        if (uri != null) core.openLocal(uri, resume, settings.autoplay) else core.open(fileId, resume, settings.autoplay)
        core.setSpeed(settings.defaultSpeed)
    }

    LaunchedEffect(core, fileId, messageId) {
        var lastRecord = 0L
        var lastResume = 0L
        while (isActive) {
            val fresh = core.snapshot()
            if (fresh != snapshot) snapshot = fresh
            val position = core.player.currentPosition
            val now = System.currentTimeMillis()
            // Resume points are written at most every 2s; recording at most every 10s.
            if (now - lastResume > 2_000) {
                lastResume = now
                settings.setResume(chatId, messageId, position)
            }
            if (now - lastRecord > 10_000) {
                lastRecord = now
                settings.recordPlay(
                    chatId, messageId, fileId, title, chatTitle,
                    position, core.player.duration.coerceAtLeast(0L), thumbnailFileId,
                )
            }
            delay(500)
        }
    }

    // Pause playback when the UI is backgrounded (PiP keeps it running), which
    // frees the decoder, audio focus and the wake lock.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, core) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !PlayerActivity.pip.value) {
                core.player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(snapshot.ended) {
        if (snapshot.ended) onEnded?.invoke()
    }

    DisposableEffect(core) {
        onDispose {
            runCatching {
                settings.recordPlay(
                    chatId, messageId, fileId, title, chatTitle,
                    core.player.currentPosition, core.player.duration.coerceAtLeast(0L), thumbnailFileId,
                )
            }
            core.release()
        }
    }

    LaunchedEffect(controlsVisible, locked) {
        if (controlsVisible && !locked) {
            delay(4_000)
            controlsVisible = false
        }
    }

    LaunchedEffect(showUnlock) {
        if (showUnlock) {
            delay(3_000)
            showUnlock = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (locked) showUnlock = !showUnlock else controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        if (!locked) {
                            val third = size.width / 3f
                            when {
                                offset.x < third -> { core.seekBy(-10_000); seekHint = -10 }
                                offset.x > size.width - third -> { core.seekBy(10_000); seekHint = 10 }
                                else -> core.togglePlay()
                            }
                            controlsVisible = true
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (!locked) {
                            dragSide = if (offset.x < size.width / 2f) 2 else 1
                            controlsVisible = true
                        }
                    },
                    onDragEnd = {
                        dragSide = 0
                        volumeHint = null
                        brightnessHint = null
                        volumeLevel = -1f
                    },
                    onVerticalDrag = { change, dy ->
                        if (!locked) {
                            change.consume()
                            val delta = -dy / size.height
                            when (dragSide) {
                                1 -> changeVolume(delta)
                                2 -> changeBrightness(delta)
                            }
                        }
                    },
                )
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = scaleMode
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    player = core.player
                    playerViewRef = this
                }
            },
        )

        if (snapshot.isBuffering) {
            androidx.compose.material3.CircularProgressIndicator(
                Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (seekHint != 0) {
            val forward = seekHint > 0
            Box(
                Modifier
                    .align(if (forward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 32.dp)
                    .size(92.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (forward) AppIcons.Forward else AppIcons.Back, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    Text(abs(seekHint).toString() + " 秒", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            LaunchedEffect(seekHint) { delay(600); seekHint = 0 }
        }

        volumeHint?.let { SideIndicator(Alignment.CenterEnd, AppIcons.Volume, it) }
        brightnessHint?.let { SideIndicator(Alignment.CenterStart, AppIcons.Fullscreen, it) }

        if (locked) {
            if (showUnlock) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable { locked = false; controlsVisible = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("  解锁", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } else {
            AnimatedVisibility(
                visible = controlsVisible && !inPip,
                enter = fadeIn() + slideInVertically { it / 16 },
                exit = fadeOut() + slideOutVertically { it / 16 },
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f))) {
                    Column(Modifier.fillMaxSize()) {
                        // ControlsTopView
                        Row(
                            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onClose) { Icon(AppIcons.Back, contentDescription = "返回", tint = Color.White) }
                            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f))
                            if (onFavorite != null) {
                                IconButton(onClick = onFavorite) {
                                    Icon(
                                        AppIcons.Star,
                                        contentDescription = if (isFavorite) "取消收藏" else "收藏",
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                                    )
                                }
                            }
                            if (onDownload != null) {
                                IconButton(onClick = onDownload) {
                                    Icon(AppIcons.Download, contentDescription = "下载", tint = Color.White)
                                }
                            }
                            IconButton(onClick = { trackDialog = 1 }) { Icon(AppIcons.Volume, contentDescription = "音轨", tint = Color.White) }
                            IconButton(onClick = { trackDialog = 2 }) { Icon(AppIcons.Captions, contentDescription = "字幕", tint = Color.White) }
                            IconButton(onClick = {
                                activity?.requestedOrientation =
                                    if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                            }) { Icon(AppIcons.Refresh, contentDescription = "旋转", tint = Color.White) }
                            IconButton(onClick = { activity?.let { PlayerActivity.enterPip(it) } }) {
                                Icon(AppIcons.Pip, contentDescription = "画中画", tint = Color.White)
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // ControlsMiddleView
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { core.seekBy(-10_000) }, modifier = Modifier.size(56.dp)) {
                                Icon(AppIcons.Back, contentDescription = "回退 10 秒", tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.width(18.dp))
                            IconButton(onClick = { core.togglePlay() }, modifier = Modifier.size(72.dp)) {
                                Icon(
                                    if (snapshot.isPlaying) AppIcons.Pause else AppIcons.Play,
                                    contentDescription = if (snapshot.isPlaying) "暂停" else "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                            Spacer(Modifier.width(18.dp))
                            IconButton(onClick = { core.seekBy(10_000) }, modifier = Modifier.size(56.dp)) {
                                Icon(AppIcons.Forward, contentDescription = "快进 10 秒", tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // ControlsBottomView
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            NextPlayerSeekbar(
                                position = snapshot.positionMs.toFloat(),
                                duration = snapshot.durationMs.toFloat(),
                                onSeek = { value -> core.player.seekTo(value.toLong()) },
                                onSeekFinished = {},
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatTime(snapshot.positionMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                Text(formatTime(snapshot.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ControlButton(AppIcons.Gauge, "倍速 " + speedLabel(snapshot.speed)) { core.setSpeed(nextSpeed(snapshot.speed)) }
                                ControlButton(AppIcons.Fullscreen, "画面") { trackDialog = 3 }
                                ControlButton(AppIcons.Lock, "锁定") { locked = true; controlsVisible = false }
                                if (onNext != null) ControlButton(AppIcons.Next, "下一个", onNext)
                            }
                        }
                    }
                }
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp))
        }
    }

    if (trackDialog == 1) {
        TrackDialog(
            title = "音轨",
            options = core.audioTracks(),
            onPick = { core.switchTrack(C.TRACK_TYPE_AUDIO, it.index); trackDialog = 0 },
            onDismiss = { trackDialog = 0 },
        )
    }
    if (trackDialog == 2) {
        AlertDialog(
            onDismissRequest = { trackDialog = 0 },
            title = { Text("字幕") },
            text = {
                Column {
                    Text(
                        "关闭字幕",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().clickable { core.switchTrack(C.TRACK_TYPE_TEXT, -1); trackDialog = 0 }.padding(vertical = 10.dp),
                    )
                    core.textTracks().forEach { option ->
                        Text(
                            (if (option.selected) "✓ " else "   ") + option.label,
                            color = if (option.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable { core.switchTrack(C.TRACK_TYPE_TEXT, option.index); trackDialog = 0 }.padding(vertical = 10.dp),
                        )
                    }
                    Text(
                        "加载本地字幕（.srt / .vtt / .ass）",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().clickable { subtitlePicker.launch(arrayOf("application/x-subrip", "text/plain", "*/*")) }.padding(vertical = 10.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { trackDialog = 0 }) { Text("关闭") } },
        )
    }
    if (trackDialog == 3) {
        AlertDialog(
            onDismissRequest = { trackDialog = 0 },
            title = { Text("画面") },
            text = {
                Column {
                    listOf(
                        "适应屏幕" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        "填充屏幕" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
                        "裁剪铺满" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                    ).forEach { (label, mode) ->
                        Text(
                            (if (scaleMode == mode) "✓ " else "   ") + label,
                            color = if (scaleMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable { scaleMode = mode; playerViewRef?.resizeMode = mode; trackDialog = 0 }.padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { trackDialog = 0 }) { Text("关闭") } },
        )
    }
}

@Composable
private fun TrackDialog(title: String, options: List<NextPlayerCore.TrackOption>, onPick: (NextPlayerCore.TrackOption) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (options.isEmpty()) {
                    Text("没有可切换的轨道", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    options.forEach { option ->
                        Text(
                            (if (option.selected) "✓ " else "   ") + option.label,
                            color = if (option.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(option) }.padding(vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

/** NextPlayer's SimpleSlider seekbar: primary played range, white thumb. */
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextPlayerSeekbar(
    position: Float,
    duration: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
) {
    val thumbSize by animateDpAsState(16.dp, label = "thumbSize")
    Slider(
        value = position,
        valueRange = 0f..(duration.takeIf { it > 0 } ?: 1f),
        onValueChange = onSeek,
        onValueChangeFinished = onSeekFinished,
        modifier = Modifier.fillMaxWidth().height(24.dp),
        colors = SliderDefaults.colors(
            thumbColor = Color.Transparent,
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
        ),
        thumb = {
            Box(
                Modifier
                    .size(thumbSize)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape),
            )
        },
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.5f), MaterialTheme.shapes.extraSmall),
            ) {
                if (duration > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth((position / duration).coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        },
    )
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).heightIn(min = 48.dp).padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.SideIndicator(alignment: Alignment, icon: ImageVector, fraction: Float) {
    Column(
        modifier = Modifier
            .align(alignment)
            .padding(horizontal = 24.dp)
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.width(4.dp).height(110.dp).background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(99.dp)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(fraction.coerceIn(0f, 1f)).background(Color.White, RoundedCornerShape(99.dp)))
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() + ".0×" else speed.toString() + "×"

private fun nextSpeed(speed: Float): Float = when {
    speed < 1.1f -> 1.25f
    speed < 1.3f -> 1.5f
    speed < 1.6f -> 2f
    else -> 1f
}
