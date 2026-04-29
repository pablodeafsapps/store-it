package org.deafsapps.storeit.androidapp.presentation.rack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.androidapp.fake.FakeDeleteRackUseCase
import org.deafsapps.storeit.androidapp.fake.FakeGetRackDataByRackIdUseCase
import org.deafsapps.storeit.androidapp.fake.FakeSaveRackUseCase
import org.deafsapps.storeit.androidapp.fake.FakeSaveSlotUseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.RackData
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackDetailUiState
import org.deafsapps.storeit.presentation.rack.model.RackSummaryVo
import org.deafsapps.storeit.presentation.rack.viewmodel.RackDetailViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class RackDetailViewModelTest {

    private lateinit var sut: RackDetailViewModel
    private lateinit var fakeGetRackDataByRackId: FakeGetRackDataByRackIdUseCase
    private lateinit var fakeSaveRack: FakeSaveRackUseCase
    private lateinit var fakeDeleteRack: FakeDeleteRackUseCase
    private lateinit var fakeSaveSlot: FakeSaveSlotUseCase
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val dummyRackId = "rack-1"
    private val dummyRack = Rack(id = dummyRackId, name = "Rack 1")
    private val dummyRackSummary = RackSummaryVo(
        id = dummyRackId,
        name = "Rack 1",
        location = "",
        photoUri = null,
    )

    @BeforeEach
    fun setUp() {
        fakeGetRackDataByRackId = FakeGetRackDataByRackIdUseCase()
        fakeSaveRack = FakeSaveRackUseCase()
        fakeDeleteRack = FakeDeleteRackUseCase()
        fakeSaveSlot = FakeSaveSlotUseCase()
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
            assertEquals(dummyRackSummary, state.rack)
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

            sut.onEditSelected()
            advanceUntilIdle()

            val state = collectedStates.last()
            assertEquals(dummyRackSummary, state.rack)
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
        sut.onEditSelected()
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
            sut.onEditSelected()
            advanceUntilIdle()
            sut.onUpdateEditName("New Name")
            val updatedRack = Rack(
                id = dummyRack.id,
                name = "New Name",
                description = dummyRack.description,
                location = dummyRack.location,
                photoUri = dummyRack.photoUri,
                createdAt = dummyRack.createdAt,
                updatedAt = dummyRack.updatedAt,
            )
            fakeSaveRack.invokeResult = updatedRack.ok()

            sut.onSaveRackEdits()
            advanceUntilIdle()

            val state = collectedStates.last()
            assertEquals(
                RackSummaryVo(
                    id = updatedRack.id,
                    name = updatedRack.name,
                    location = updatedRack.location,
                    photoUri = updatedRack.photoUri,
                ),
                state.rack,
            )
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

        sut.onRemoveRackSelected()
        advanceUntilIdle()

        val state = collectedStates.last()
        assertEquals(dummyRackSummary, state.rack)
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
            sut.onRemoveRackSelected()

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
            sut.onRemoveRackSelected()
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
    fun `GIVEN rack loaded WHEN onImageTap on empty spot THEN NavigateToAddItemDraft emitted`() =
        runTest(testDispatcher) {
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = emptyList(),
                items = emptyList(),
            ).ok()
            sut = getDummyRackDetailViewModel()
            val collectedEvents = mutableListOf<RackDetailUiEvent?>()
            val eventCollectJob: Job = launch { sut.uiEvent.collect { collectedEvents.add(it) } }
            advanceUntilIdle()

            sut.onImageTap(xRel = 0.3f, yRel = 0.4f)
            advanceUntilIdle()

            val draftNav = collectedEvents.filterNotNull().filterIsInstance<RackDetailUiEvent.NavigateToAddItemDraft>().single()
            assertEquals(dummyRackId, draftNav.rackId)
            assertTrue(draftNav.slotId.isNotBlank())
            assertEquals(0.3f, draftNav.slotXRel)
            assertEquals(0.4f, draftNav.slotYRel)
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
        sut.onEditSelected()
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
        sut.onRemoveRackSelected()
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
    fun `GIVEN existing slot WHEN onImageTap nearby THEN NavigateToSlotItems emitted`() =
        runTest(testDispatcher) {
            val slot = ShelfSlot(id = "s1", rackId = dummyRackId, position = SlotPosition(0f, 0f, 0.5f, 0.5f))
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = listOf(slot),
                items = listOf(Item(id = "i1", rackId = dummyRackId, slotId = "s1", name = "Hammer")),
            ).ok()
            sut = getDummyRackDetailViewModel()
            advanceUntilIdle()
            val collectedEvents = mutableListOf<RackDetailUiEvent?>()
            val eventCollectJob: Job = launch { sut.uiEvent.collect { collectedEvents.add(it) } }
            advanceUntilIdle()

            sut.onImageTap(xRel = 0.52f, yRel = 0.52f)
            advanceUntilIdle()

            val nav = collectedEvents.filterNotNull().filterIsInstance<RackDetailUiEvent.NavigateToSlotItems>().single()
            assertEquals(dummyRackId, nav.rackId)
            assertEquals("s1", nav.slotId)
            eventCollectJob.cancel()
        }

    @Test
    fun `GIVEN no matching slot WHEN onImageTap THEN draft add-item navigation emitted`() = runTest(testDispatcher) {
        fakeGetRackDataByRackId.invokeResult = RackData(
            id = dummyRackId,
            rack = dummyRack,
            shelfSlots = emptyList(),
            items = emptyList(),
        ).ok()
        sut = getDummyRackDetailViewModel()
        val collectedEvents = mutableListOf<RackDetailUiEvent?>()
        val eventCollectJob: Job = launch { sut.uiEvent.collect { collectedEvents.add(it) } }
        advanceUntilIdle()

        sut.onImageTap(xRel = 0.5f, yRel = 0.5f)
        advanceUntilIdle()

        val draftNav = collectedEvents.filterNotNull().filterIsInstance<RackDetailUiEvent.NavigateToAddItemDraft>().single()
        assertEquals(dummyRackId, draftNav.rackId)
        eventCollectJob.cancel()
    }

    @Test
    fun `GIVEN existing slot WHEN onSaveSlotMarkerPosition THEN slot moves and persists`() =
        runTest(testDispatcher) {
            val slot = ShelfSlot(id = "s1", rackId = dummyRackId, position = SlotPosition(0f, 0f, 0.5f, 0.5f))
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = listOf(slot),
                items = emptyList(),
            ).ok()
            sut = getDummyRackDetailViewModel()
            advanceUntilIdle()

            sut.onSaveSlotMarkerPosition(slotId = "s1", xRel = 0.8f, yRel = 0.3f)
            advanceUntilIdle()

            assertEquals(0.8f, sut.uiState.value.slots.single().xRel)
            assertEquals(0.3f, sut.uiState.value.slots.single().yRel)
            assertEquals(1, fakeSaveSlot.invokeCount)
            assertEquals("s1", fakeSaveSlot.lastSlot?.id)
            assertEquals(0.8f, fakeSaveSlot.lastSlot?.position?.xRel)
            assertEquals(0.3f, fakeSaveSlot.lastSlot?.position?.yRel)
        }

    @Test
    fun `GIVEN existing slot WHEN onSlotMarkerDrag THEN slot moves in UI without persisting`() =
        runTest(testDispatcher) {
            val slot = ShelfSlot(id = "s1", rackId = dummyRackId, position = SlotPosition(0f, 0f, 0.5f, 0.5f))
            fakeGetRackDataByRackId.invokeResult = RackData(
                id = dummyRackId,
                rack = dummyRack,
                shelfSlots = listOf(slot),
                items = emptyList(),
            ).ok()
            sut = getDummyRackDetailViewModel()
            advanceUntilIdle()

            sut.onSlotMarkerDrag(slotId = "s1", xRel = 0.2f, yRel = 0.4f)
            advanceUntilIdle()

            assertEquals(0.2f, sut.uiState.value.slots.single().xRel)
            assertEquals(0.4f, sut.uiState.value.slots.single().yRel)
            assertEquals(0, fakeSaveSlot.invokeCount)
        }

    private fun getDummyRackDetailViewModel(): RackDetailViewModel {
        val viewModel = RackDetailViewModel(
            coroutineScope = testScope,
            rackId = dummyRackId,
            getRackDataByRackIdUseCase = fakeGetRackDataByRackId,
            saveRackUseCase = fakeSaveRack,
            deleteRackUseCase = fakeDeleteRack,
            saveSlotUseCase = fakeSaveSlot,
        )
        testScope.backgroundScope.launch(UnconfinedTestDispatcher(testScope.testScheduler)) {
            viewModel.uiState.collect {}
        }
        return viewModel
    }
}
