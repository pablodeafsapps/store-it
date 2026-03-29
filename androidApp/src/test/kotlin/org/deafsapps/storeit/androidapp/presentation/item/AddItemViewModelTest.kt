package org.deafsapps.storeit.androidapp.presentation.item

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.androidapp.fake.FakeAddItemUseCase
import org.deafsapps.storeit.androidapp.fake.FakeGetRacksFlowUseCase
import org.deafsapps.storeit.androidapp.fake.FakeSaveSlotUseCase
import org.deafsapps.storeit.presentation.item.model.AddItemSlotVo
import org.deafsapps.storeit.presentation.item.viewmodel.AddItemViewModel
import org.deafsapps.storeit.presentation.rack.model.SlotPlacementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddItemViewModelTest {

    private lateinit var fakeAddItem: FakeAddItemUseCase
    private lateinit var fakeGetRacks: FakeGetRacksFlowUseCase
    private lateinit var fakeSaveSlot: FakeSaveSlotUseCase
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setUp() {
        fakeAddItem = FakeAddItemUseCase()
        fakeGetRacks = FakeGetRacksFlowUseCase()
        fakeSaveSlot = FakeSaveSlotUseCase()
    }

    @Test
    fun `GIVEN draft slot initial params WHEN onSaveItem THEN slot and item are saved`() = runTest(testDispatcher) {
        val sut = AddItemViewModel(
            initialRackId = "r1",
            addItemSlot = AddItemSlotVo(
                id = "s1",
                placementType = SlotPlacementType.DRAFT,
                xRel = 0.3f,
                yRel = 0.4f,
            ),
            coroutineScope = testScope,
            addItemUseCase = fakeAddItem,
            getRacksFlowUseCase = fakeGetRacks,
            saveSlotUseCase = fakeSaveSlot,
        )
        sut.onUpdateName("Box")
        sut.onSaveItem()
        advanceUntilIdle()

        assertEquals(1, fakeSaveSlot.invokeCount)
        assertEquals("s1", fakeSaveSlot.lastSlot?.id)
        assertEquals(0.3f, fakeSaveSlot.lastSlot?.position?.xRel)
        assertEquals(1, fakeAddItem.lastItem?.let { 1 } ?: 0)
        assertEquals("s1", fakeAddItem.lastItem?.slotId)
    }

    @Test
    fun `GIVEN persisted slot initial params WHEN onSaveItem THEN only item is saved`() = runTest(testDispatcher) {
        val sut = AddItemViewModel(
            initialRackId = "r1",
            addItemSlot = AddItemSlotVo(
                id = "s1",
                placementType = SlotPlacementType.EXISTING,
                xRel = null,
                yRel = null,
            ),
            coroutineScope = testScope,
            addItemUseCase = fakeAddItem,
            getRacksFlowUseCase = fakeGetRacks,
            saveSlotUseCase = fakeSaveSlot,
        )
        sut.onUpdateName("Box")
        sut.onSaveItem()
        advanceUntilIdle()

        assertEquals(0, fakeSaveSlot.invokeCount)
        assertEquals("s1", fakeAddItem.lastItem?.slotId)
    }
}
