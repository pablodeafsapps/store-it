package org.deafsapps.storeit.androidapp.presentation.item

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.androidapp.fake.FakeGetItemsBySlotUseCase
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.presentation.item.model.SlotItemsUiState
import org.deafsapps.storeit.presentation.item.viewmodel.SlotItemsViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SlotItemsViewModelTest {

    private lateinit var fakeGetItemsBySlot: FakeGetItemsBySlotUseCase
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val rackId = "r1"
    private val slotId = "s1"

    @BeforeEach
    fun setUp() {
        fakeGetItemsBySlot = FakeGetItemsBySlotUseCase()
    }

    @Test
    fun `GIVEN use case returns items WHEN ViewModel created THEN uiState lists them`() =
        runTest(testDispatcher) {
            fakeGetItemsBySlot.invokeResult = listOf(
                Item(id = "i1", rackId = rackId, slotId = slotId, name = "Drill"),
            ).ok()
            val sut = SlotItemsViewModel(
                rackId = rackId,
                slotId = slotId,
                coroutineScope = testScope,
                getItemsBySlotUseCase = fakeGetItemsBySlot,
            )
            val states = mutableListOf<SlotItemsUiState>()
            val job: Job = launch { sut.uiState.collect { states.add(it) } }
            advanceUntilIdle()

            val loaded = states.first { !it.isLoading }
            assertEquals("Drill", loaded.items.single().name)
            assertNull(loaded.error)
            job.cancel()
        }

    @Test
    fun `GIVEN use case returns empty WHEN ViewModel created THEN uiState has no items`() =
        runTest(testDispatcher) {
            fakeGetItemsBySlot.invokeResult = emptyList<Item>().ok()
            val sut = SlotItemsViewModel(
                rackId = rackId,
                slotId = slotId,
                coroutineScope = testScope,
                getItemsBySlotUseCase = fakeGetItemsBySlot,
            )
            collectUiState(sut = sut)

            advanceUntilIdle()

            assertTrue(sut.uiState.value.items.isEmpty())
            assertFalse(sut.uiState.value.isLoading)
        }

    private fun TestScope.collectUiState(sut: SlotItemsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sut.uiState.collect {}
        }
    }
}
