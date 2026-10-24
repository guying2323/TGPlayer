package com.tgplayer.android.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tgplayer.android.data.ChatKind
import com.tgplayer.android.data.ChatSummary
import com.tgplayer.android.data.repository.MediaRepository
import com.tgplayer.android.di.AppContainer
import com.tgplayer.android.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatsViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<ChatSummary>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ChatSummary>>> = _state.asStateFlow()

    private val _kindFilter = MutableStateFlow(0)
    val kindFilter: StateFlow<Int> = _kindFilter.asStateFlow()

    private val _sortByName = MutableStateFlow(false)
    val sortByName: StateFlow<Boolean> = _sortByName.asStateFlow()

    val visible: StateFlow<List<ChatSummary>> =
        combine(_state, _kindFilter, _sortByName) { state, kind, sort ->
            val all = (state as? UiState.Ready)?.data ?: emptyList()
            val filtered = when (kind) {
                1 -> all.filter { it.kind == ChatKind.Channel }
                2 -> all.filter { it.kind == ChatKind.Group }
                3 -> all.filter { it.kind == ChatKind.Person || it.kind == ChatKind.Other }
                else -> all
            }
            if (sort) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.lastDate }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
                UiState.Ready(repository.chats())
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                UiState.Failed(t.message?.takeIf { it.isNotBlank() } ?: "加载失败")
            }
        }
    }

    fun setKindFilter(value: Int) {
        _kindFilter.value = value
    }

    fun toggleSort() {
        _sortByName.value = !_sortByName.value
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ChatsViewModel(AppContainer.mediaRepository) }
        }
    }
}
