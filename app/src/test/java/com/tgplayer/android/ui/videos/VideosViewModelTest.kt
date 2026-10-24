package com.tgplayer.android.ui.videos

import com.tgplayer.android.data.MediaItem
import com.tgplayer.android.ui.chats.FakeMediaRepository
import com.tgplayer.android.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideosViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsVideosForChat() = runTest(dispatcher) {
        val item = MediaItem(
            chatId = 7,
            messageId = 99,
            fileId = 3,
            title = "Clip",
            fileName = "clip.mp4",
            sizeBytes = 10,
            durationSec = 5,
            mimeType = "video/mp4",
        )
        val repo = FakeMediaRepository(videos = listOf(item))
        val vm = VideosViewModel(repo, chatId = 7)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UiState.Ready)
        assertEquals(listOf(99L), (state as UiState.Ready).data.map { it.messageId })
    }

    @Test
    fun failedRepositorySurfacesMessage() = runTest(dispatcher) {
        val repo = object : FakeMediaRepository() {
            override suspend fun videos(chatId: Long, limit: Int): List<MediaItem> = error("boom")
        }
        val vm = VideosViewModel(repo, chatId = 7)
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Failed)
    }
}
