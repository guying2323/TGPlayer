package com.tgplayer.android.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tgplayer.android.data.MediaItem
import com.tgplayer.android.data.repository.MediaRepository
import com.tgplayer.android.di.AppContainer
import com.tgplayer.android.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideosViewModel(
    private val repository: MediaRepository,
    private val chatId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<MediaItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MediaItem>>> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            repository.connected.collect { connected -> if (connected) load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Ready(repository.videos(chatId))
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                UiState.Failed(t.message?.takeIf { it.isNotBlank() } ?: "加载失败")
            }
        }
    }

    companion object {
        fun factory(chatId: Long) = viewModelFactory {
            initializer { VideosViewModel(AppContainer.mediaRepository, chatId) }
        }
    }
}
