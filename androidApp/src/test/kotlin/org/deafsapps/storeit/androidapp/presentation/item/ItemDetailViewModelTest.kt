package org.deafsapps.storeit.androidapp.presentation.item

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.androidapp.fake.FakeAddItemUseCase
import org.deafsapps.storeit.androidapp.fake.FakeDeleteItemUseCase
import org.deafsapps.storeit.androidapp.fake.FakeGetItemByIdUseCase
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.presentation.item.viewmodel.ItemDetailViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ItemDetailViewModelTest {

    private lateinit var fakeGet: FakeGetItemByIdUseCase
    private lateinit var fakeAdd: FakeAddItemUseCase
    private lateinit var fakeDelete: FakeDeleteItemUseCase
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val itemId = "i1"
    private val item = Item(id = itemId, rackId = "r1", slotId = "s1", name = "Drill")

    @BeforeEach
    fun setUp() {
        fakeGet = FakeGetItemByIdUseCase()
        fakeAdd = FakeAddItemUseCase()
        fakeDelete = FakeDeleteItemUseCase()
    }

    @Test
    fun `GIVEN item exists WHEN ViewModel created THEN fields populated`() =
        runTest(testDispatcher) {
            fakeGet.invokeResult = item.ok()
            val sut = ItemDetailViewModel(
                itemId = itemId,
                coroutineScope = testScope,
                getItemByIdUseCase = fakeGet,
                addItemUseCase = fakeAdd,
                deleteItemUseCase = fakeDelete,
            )
            collectUiState(sut = sut)

            advanceUntilIdle()

            assertFalse(sut.uiState.value.isLoading)
            assertEquals("Drill", sut.uiState.value.name)
        }

    @Test
    fun `GIVEN loaded item WHEN onSave THEN add use case receives updated item`() =
        runTest(testDispatcher) {
            fakeGet.invokeResult = item.ok()
            val sut = ItemDetailViewModel(
                itemId = itemId,
                coroutineScope = testScope,
                getItemByIdUseCase = fakeGet,
                addItemUseCase = fakeAdd,
                deleteItemUseCase = fakeDelete,
            )
            collectUiState(sut = sut)

            advanceUntilIdle()
            sut.onUpdateName("Hammer")
            sut.onSave()
            advanceUntilIdle()

            assertEquals("Hammer", fakeAdd.lastItem?.name)
        }

    @Test
    fun `GIVEN loaded item WHEN onConfirmDelete THEN delete use case invoked`() =
        runTest(testDispatcher) {
            fakeGet.invokeResult = item.ok()
            val sut = ItemDetailViewModel(
                itemId = itemId,
                coroutineScope = testScope,
                getItemByIdUseCase = fakeGet,
                addItemUseCase = fakeAdd,
                deleteItemUseCase = fakeDelete,
            )
            collectUiState(sut = sut)

            advanceUntilIdle()
            sut.onConfirmDelete()
            advanceUntilIdle()

            assertEquals(itemId, fakeDelete.lastId)
        }

    private fun TestScope.collectUiState(sut: ItemDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sut.uiState.collect {}
        }
    }
}
