package com.tgplayer.android

import android.app.Application
import com.tgplayer.android.data.Settings
import com.tgplayer.android.data.Downloads
import com.tgplayer.android.data.Favorites
import com.tgplayer.android.data.Td
import com.tgplayer.android.ui.common.Thumbnails

class TmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Settings.install(this)
        Td.install(this)
        Downloads.install(this)
        Favorites.install(this)
        Thumbnails.highQuality = Settings.get().hdThumbnails
    }
}
