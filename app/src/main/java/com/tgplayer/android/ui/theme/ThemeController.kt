package com.tgplayer.android.ui.theme

import com.tgplayer.android.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow

/** Observable theme preferences, read by MainActivity. */
object ThemeController {
    val mode = MutableStateFlow(0) // 0 system, 1 dark, 2 light
    val dynamic = MutableStateFlow(true)

    fun install(settings: Settings) {
        mode.value = settings.themeMode
        dynamic.value = settings.dynamicColor
    }

    fun setMode(value: Int) {
        mode.value = value
        Settings.get().themeMode = value
    }

    fun setDynamic(value: Boolean) {
        dynamic.value = value
        Settings.get().dynamicColor = value
    }
}
