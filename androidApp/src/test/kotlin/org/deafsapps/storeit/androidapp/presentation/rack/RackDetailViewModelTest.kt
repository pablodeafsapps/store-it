package org.deafsapps.storeit.androidapp.presentation.rack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.androidapp.fake.FakeDeleteRackUseCase
import org.deafsapps.storeit.androidapp.fake.FakeGetRackDataByRackIdUseCase
import org.deafsapps.storeit.androidapp.fake.FakeSaveRackUseCase
import org.deafsapps.storeit.androidapp.fake.FakeSaveSlotUseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.RackData
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiState
import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RackDetailViewModelTest {

    private lateinit var sut: RackDetailViewModel
    private lateinit var fakeGetRackDataByRackId: FakeGetRackDataByRackIdUseCase
    private lateinit var fakeSaveSlot: FakeSaveSlotUseCase
    private lateinit var fakeSaveRack: FakeSaveRackUseCase
    private lateinit var fakeDeleteRack: FakeDeleteRackUseCase
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val dummyRackId = "rack-1"
    private val dummyRack = Rack(id = dummyRackId, name = "Rack 1")

    @Before
    fun setUp() {
        fakeGetRackDataByRackId = FakeGetRackDataByRackIdUseCase()
        fakeSaveSlot = FakeSaveSlotUseCase()
        fakeSaveRack = FakeSaveRackUseCase()
        fakeDeleteRack = FakeDeleteRackUseCase()
    }

    @Test
    fun `GIVEN getRack and getSlots succeed WHEN ViewModel created THEN uiState has rack and slots`() =
        runTest(testDispatcher) {
            val slot = ShelfSlot(id = "s1", rackId = dummyRackId, position = SlotPosition(0f, 0f, 0.5f, 0.5f))
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = listOf(slot),
                items = emptyList(),
            ).ok()

            sut = getDummyRackDetailViewModel()
            val collectedStates = mutableListOf<RackDetailUiState>()
            val collectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
            advanceUntilIdle()

            val state = collectedStates.firstOrNull { !it.isLoading } ?: collectedStates.last()
            assertEquals(dummyRack, state.rack)
            assertEquals(1, state.slots.size)
            assertEquals(0.5f, state.slots.first().xRel)
            assertEquals(0.5f, state.slots.first().yRel)
            assertNull(state.error)
            collectJob.cancel()
        }

    @Test
    fun `GIVEN getRack fails WHEN ViewModel created THEN uiState has error`() = runTest(testDispatcher) {
        fakeGetRackDataByRackId.invokeResult = DomainError.NotFound(resource = "Rack", id = dummyRackId).err()

        sut = getDummyRackDetailViewModel()
        val collectedStates = mutableListOf<RackDetailUiState>()
        val collectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
        advanceUntilIdle()

        val state = collectedStates.firstOrNull { !it.isLoading } ?: collectedStates.last()
        assertNull(state.rack)
        assertTrue(state.error?.contains("not found") == true)
        collectJob.cancel()
    }

    @Test
    fun `GIVEN getRackData fails WHEN ViewModel created THEN uiState has error`() =
        runTest(testDispatcher) {
            fakeGetRackDataByRackId.invokeResult = DomainError.NotFound(resource = "Slot", id = null).err()

            sut = getDummyRackDetailViewModel()
            val collectedStates = mutableListOf<RackDetailUiState>()
            val collectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
            advanceUntilIdle()

            val state = collectedStates.firstOrNull { !it.isLoading } ?: collectedStates.last()
            assertNull(state.rack)
            assertTrue(state.error != null)
            collectJob.cancel()
        }

    @Test
    fun `GIVEN rack loaded WHEN onEditSelect THEN edit dialog opens with rack data`() =
        runTest(testDispatcher) {
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = emptyList(),
                items = emptyList(),
            ).ok()
            sut = getDummyRackDetailViewModel()
            val collectedStates = mutableListOf<RackDetailUiState>()
            val collectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
            advanceUntilIdle()

            sut.onEditSelect()
            advanceUntilIdle()

            val state = collectedStates.last()
            assertEquals(dummyRack, state.rack)
            assertTrue(state.showEditDialog)
            assertEquals(dummyRack.name, state.editName)
            assertEquals(dummyRack.description, state.editDescription)
            assertEquals(dummyRack.location, state.editLocation)
            collectJob.cancel()
        }

    @Test
    fun `GIVEN edit dialog open WHEN dismissEditDialog THEN showEditDialog false`() = runTest(testDispatcher) {
        fakeGetRackDataByRackId.invokeResult = RackData(
            id = dummyRackId,
            rack = dummyRack,
            shelfSlots = emptyList(),
            items = emptyList(),
        ).ok()
        sut = getDummyRackDetailViewModel()
        val collectedStates = mutableListOf<RackDetailUiState>()
        val collectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
        advanceUntilIdle()
        sut.onEditSelect()
        advanceUntilIdle()

        sut.onDismissEditDialog()
        advanceUntilIdle()

        assertFalse(collectedStates.last().showEditDialog)
        collectJob.cancel()
    }

    @Test
    fun `GIVEN rack loaded WHEN saveRackEdits called THEN uiState has updated rack and dialog closed`() =
        runTest(testDispatcher) {
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = emptyList(),
                items = emptyList(),
            ).ok()
            sut = getDummyRackDetailViewModel()
            val collectedStates = mutableListOf<RackDetailUiState>()
            val collectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
            advanceUntilIdle()
            sut.onEditSelect()
            advanceUntilIdle()
            sut.onUpdateEditName("New Name")
            val updatedRack = dummyRack.copy(name = "New Name")
            fakeSaveRack.invokeResult = updatedRack.ok()

            sut.onSaveRackEdits()
            advanceUntilIdle()

            val state = collectedStates.last()
            assertEquals(updatedRack, state.rack)
            assertFalse(state.showEditDialog)
            collectJob.cancel()
        }

    @Test
    fun `GIVEN rack loaded WHEN onRemoveRackSelect THEN uiState rack and slots unchanged`() = runTest(testDispatcher) {
        fakeGetRackDataByRackId.invokeResult = RackData(
            id = dummyRackId,
            rack = dummyRack,
            shelfSlots = emptyList(),
            items = emptyList(),
        ).ok()
        sut = getDummyRackDetailViewModel()
        val collectedStates = mutableListOf<RackDetailUiState>()
        val collectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
        advanceUntilIdle()

        sut.onRemoveRackSelect()
        advanceUntilIdle()

        val state = collectedStates.last()
        assertEquals(dummyRack, state.rack)
        assertTrue(state.slots.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `GIVEN delete confirm shown WHEN dismissDeleteConfirm THEN showDeleteConfirm false`() =
        runTest(testDispatcher) {
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = emptyList(),
                items = emptyList(),
            ).ok()
            sut = getDummyRackDetailViewModel()
            advanceUntilIdle()
            sut.onRemoveRackSelect()

            sut.onDismissDeleteConfirm()

            assertFalse(sut.uiState.value.showDeleteConfirm)
        }

    @Test
    fun `GIVEN delete confirm and delete succeeds WHEN confirmDeleteRack THEN uiEvent NavigateBack`() =
        runTest(testDispatcher) {
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = emptyList(),
                items = emptyList(),
            ).ok()
            fakeDeleteRack.invokeResult = Unit.ok()
            sut = getDummyRackDetailViewModel()
            advanceUntilIdle()
            sut.onRemoveRackSelect()
            val collectedEvents = mutableListOf<RackDetailUiEvent?>()
            val collectJob: Job = launch { sut.uiEvent.collect { collectedEvents.add(it) } }
            advanceUntilIdle()

            sut.onConfirmDeleteRack()
            advanceUntilIdle()

            assertFalse(sut.uiState.value.showDeleteConfirm)
            val event = collectedEvents.filterNotNull().single()
            assertTrue(event is RackDetailUiEvent.NavigateBack)
            collectJob.cancel()
        }

    @Test
    fun `GIVEN rack loaded and saveSlot succeeds WHEN onImageTap THEN uiState has new slot and slot selected`() =
        runTest(testDispatcher) {
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = emptyList(),
                items = emptyList(),
            ).ok()
            val savedSlot = ShelfSlot(id = "saved-1", rackId = dummyRackId, position = SlotPosition(0f, 0f, 0.3f, 0.4f))
            fakeSaveSlot.invokeResult = savedSlot.ok()
            sut = getDummyRackDetailViewModel()
            val collectedStates = mutableListOf<RackDetailUiState>()
            val stateCollectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
            advanceUntilIdle()
            val collectedEvents = mutableListOf<RackDetailUiEvent?>()
            val eventCollectJob: Job = launch { sut.uiEvent.collect { collectedEvents.add(it) } }
            advanceUntilIdle()

            sut.onImageTap(xRel = 0.3f, yRel = 0.4f)
            advanceUntilIdle()

            val state = collectedStates.last()
            assertEquals(dummyRack, state.rack)
            assertEquals(1, state.slots.size)
            assertEquals(0.3f, state.slots.first().xRel)
            assertEquals(0.4f, state.slots.first().yRel)
            assertEquals("saved-1", state.selectedSlotId)
            stateCollectJob.cancel()
            eventCollectJob.cancel()
        }

    @Test
    fun `GIVEN saveRackEdits would fail WHEN saveRackEdits THEN uiEvent ShowError emitted`() = runTest(testDispatcher) {
        fakeGetRackDataByRackId.invokeResult = RackData(
            id = dummyRackId,
            rack = dummyRack,
            shelfSlots = emptyList(),
            items = emptyList(),
        ).ok()
        fakeSaveRack.invokeResult = DomainError.ValidationError(reason = "Name too long").err()
        sut = getDummyRackDetailViewModel()
        val collectedStates = mutableListOf<RackDetailUiState>()
        val stateCollectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
        advanceUntilIdle()
        sut.onEditSelect()
        advanceUntilIdle()
        val collectedEvents = mutableListOf<RackDetailUiEvent?>()
        val eventCollectJob: Job = launch { sut.uiEvent.collect { collectedEvents.add(it) } }
        advanceUntilIdle()

        sut.onSaveRackEdits()
        advanceUntilIdle()

        val showErrors = collectedEvents.filterNotNull().filterIsInstance<RackDetailUiEvent.ShowError>()
        assertTrue(showErrors.isNotEmpty())
        assertTrue(showErrors.single().message.contains("Name too long"))
        stateCollectJob.cancel()
        eventCollectJob.cancel()
    }

    @Test
    fun `GIVEN confirmDeleteRack fails WHEN confirmDeleteRack THEN uiEvent ShowError`() = runTest(testDispatcher) {
        fakeGetRackDataByRackId.invokeResult = RackData(
            id = dummyRackId,
            rack = dummyRack,
            shelfSlots = emptyList(),
            items = emptyList(),
        ).ok()
        fakeDeleteRack.invokeResult = DomainError.NotFound(resource = "Rack", id = dummyRackId).err()
        sut = getDummyRackDetailViewModel()
        val collectedStates = mutableListOf<RackDetailUiState>()
        val stateCollectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
        advanceUntilIdle()
        sut.onRemoveRackSelect()
        advanceUntilIdle()
        val collectedEvents = mutableListOf<RackDetailUiEvent?>()
        val eventCollectJob: Job = launch { sut.uiEvent.collect { collectedEvents.add(it) } }
        advanceUntilIdle()

        sut.onConfirmDeleteRack()
        advanceUntilIdle()

        val showErrors = collectedEvents.filterNotNull().filterIsInstance<RackDetailUiEvent.ShowError>()
        assertTrue(showErrors.isNotEmpty())
        assertTrue(showErrors.single().message.contains("not found"))
        stateCollectJob.cancel()
        eventCollectJob.cancel()
    }

    @Test
    fun `GIVEN saveSlot would fail WHEN onImageTap THEN uiEvent ShowError emitted`() = runTest(testDispatcher) {
        fakeGetRackDataByRackId.invokeResult = RackData(
            id = dummyRackId,
            rack = dummyRack,
            shelfSlots = emptyList(),
            items = emptyList(),
        ).ok()
        fakeSaveSlot.invokeResult = DomainError.ValidationError(reason = "Invalid position").err()
        sut = getDummyRackDetailViewModel()
        val collectedStates = mutableListOf<RackDetailUiState>()
        val stateCollectJob: Job = launch { sut.uiState.collect { collectedStates.add(it) } }
        advanceUntilIdle()
        val collectedEvents = mutableListOf<RackDetailUiEvent?>()
        val eventCollectJob: Job = launch { sut.uiEvent.collect { collectedEvents.add(it) } }
        advanceUntilIdle()

        sut.onImageTap(xRel = 0.5f, yRel = 0.5f)
        advanceUntilIdle()

        assertTrue(collectedStates.last().slots.isEmpty())
        val showErrors = collectedEvents.filterNotNull().filterIsInstance<RackDetailUiEvent.ShowError>()
        assertTrue(showErrors.isNotEmpty())
        assertTrue(showErrors.single().message.contains("Invalid position"))
        stateCollectJob.cancel()
        eventCollectJob.cancel()
    }

    private fun getDummyRackDetailViewModel(): RackDetailViewModel =
        RackDetailViewModel(
            coroutineScope = testScope,
            rackId = dummyRackId,
            getRackDataByRackIdUseCase = fakeGetRackDataByRackId,
            saveSlotUseCase = fakeSaveSlot,
            saveRackUseCase = fakeSaveRack,
            deleteRackUseCase = fakeDeleteRack,
        )
}
