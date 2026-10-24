package com.tgplayer.android.ui.chats

import com.tgplayer.android.data.ChatKind
import com.tgplayer.android.data.ChatSummary
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
class ChatsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsChatsAndFiltersByKind() = runTest(dispatcher) {
        val repo = FakeMediaRepository(
            chats = listOf(
                ChatSummary(1, "News", lastDate = 10, kind = ChatKind.Channel),
                ChatSummary(2, "Friends", lastDate = 20, kind = ChatKind.Group),
            ),
        )
        val vm = ChatsViewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.state.value is UiState.Ready)
        assertEquals(2, vm.visible.value.size)

        vm.setKindFilter(1)
        advanceUntilIdle()
        assertEquals(listOf(1L), vm.visible.value.map { it.id })

        vm.setKindFilter(0)
        vm.toggleSort()
        advanceUntilIdle()
        assertEquals(listOf(2L, 1L), vm.visible.value.map { it.id })
    }

    @Test
    fun repositoryFailureBecomesFailedState() = runTest(dispatcher) {
        val repo = object : FakeMediaRepository() {
            override suspend fun chats(limit: Int): List<ChatSummary> = error("offline")
        }
        val vm = ChatsViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Failed)
    }
}
