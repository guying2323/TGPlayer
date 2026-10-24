package com.tgplayer.android.di

import com.tgplayer.android.data.repository.MediaRepository
import com.tgplayer.android.data.repository.TdMediaRepository

/**
 * Manual application container. Small enough that Hilt would only add build
 * time; ViewModels take the repository through their factories, so tests can
 * inject a fake.
 */
object AppContainer {
    val mediaRepository: MediaRepository by lazy { TdMediaRepository() }
}
