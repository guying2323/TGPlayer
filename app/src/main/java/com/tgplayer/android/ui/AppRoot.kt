package com.tgplayer.android.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tgplayer.android.data.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgplayer.android.data.AuthState
import com.tgplayer.android.data.ChatSummary
import com.tgplayer.android.data.Td
import com.tgplayer.android.ui.theme.AppIcons
import com.tgplayer.android.ui.theme.TmSpacing

sealed interface Dest {
    data object Home : Dest
    data object Chats : Dest
    data object Downloads : Dest
    data object Search : Dest
    data class Videos(val chatId: Long, val title: String, val photoFileId: Int = 0) : Dest
    data object Settings : Dest
    data object Favorites : Dest
}

private data class Tab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val dest: Dest,
)

@Composable
fun AppRoot() {
    val settings = remember { Settings.get() }
    var onboardingDone by remember { mutableStateOf(settings.onboardingDone) }
    if (!onboardingDone) {
        OnboardingScreen(settings) { onboardingDone = true }
        return
    }
    val auth by Td.auth.collectAsStateWithLifecycle()
    if (auth is AuthState.Ready) Shell() else SignInScreen(auth)
}

@Composable
private fun Shell() {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp().value }
    val isTv = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val wide = screenWidthDp >= 600 || isTv
    val pagePadding = if (isTv) TmSpacing.tvPage else TmSpacing.page

    val stack = remember { mutableStateListOf<Dest>(Dest.Home) }
    val current = stack.last()
    // Findroid keeps three top-level tabs: Home, Media, Downloads; Search and
    // Settings are pushed from the top app bars.
    val tabs = listOf(
        Tab("首页", AppIcons.Home, Dest.Home),
        Tab("媒体", AppIcons.ListVideo, Dest.Chats),
        Tab("下载", AppIcons.Download, Dest.Downloads),
    )
    val atTabRoot = current is Dest.Home || current is Dest.Chats || current is Dest.Downloads

    fun open(dest: Dest) = stack.add(dest)
    fun select(dest: Dest) {
        if (current::class != dest::class) {
            stack.clear()
            stack.add(dest)
        }
    }
    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { back() }

    val content = @Composable {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally(tween(240)) { it / 12 }) togetherWith
                    (fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { -it / 12 })
            },
            label = "screen",
        ) { dest ->
            Content(
                current = dest,
                pagePadding = pagePadding,
                onOpenChat = { open(Dest.Videos(it.id, it.title, it.photoFileId)) },
                onSearch = { open(Dest.Search) },
                onSettings = { open(Dest.Settings) },
                onFavorites = { open(Dest.Favorites) },
                onBack = ::back,
            )
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                tabs.forEach { tab ->
                    NavigationRailItem(
                        selected = current::class == tab.dest::class,
                        onClick = { select(tab.dest) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxSize()) { content() }
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                AnimatedVisibility(
                    visible = atTabRoot,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it },
                    exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { it },
                ) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = current::class == tab.dest::class,
                                onClick = { select(tab.dest) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { inner ->
            Box(Modifier.padding(inner)) { content() }
        }
    }
}

@Composable
private fun Content(
    current: Dest,
    pagePadding: Dp,
    onOpenChat: (ChatSummary) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onFavorites: () -> Unit,
    onBack: () -> Unit,
) {
    when (current) {
        is Dest.Home -> HomeScreen(pagePadding, onOpenChat, onSearch, onSettings, onFavorites)
        is Dest.Chats -> ChatsScreen(pagePadding, onOpenChat, onSearch, onSettings)
        is Dest.Downloads -> DownloadsScreen(pagePadding)
        is Dest.Search -> SearchScreen(pagePadding, onOpenChat, onBack)
        is Dest.Videos -> VideosScreen(current.chatId, current.title, current.photoFileId, pagePadding, onBack)
        is Dest.Settings -> SettingsScreen(pagePadding, onBack)
        is Dest.Favorites -> FavoritesScreen(pagePadding, onBack, onOpenChat)
    }
}
