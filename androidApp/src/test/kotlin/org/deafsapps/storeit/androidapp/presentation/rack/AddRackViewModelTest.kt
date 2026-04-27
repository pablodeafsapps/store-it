package org.deafsapps.storeit.androidapp.presentation.rack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.androidapp.fake.FakeSaveRackUseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.presentation.rack.model.AddRackUiEvent
import org.deafsapps.storeit.presentation.rack.model.AddRackUiState
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddRackViewModelTest {

    private lateinit var sut: AddRackViewModel
    private lateinit var fakeSaveRackUseCase: FakeSaveRackUseCase
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setUp() {
        fakeSaveRackUseCase = FakeSaveRackUseCase()
    }

    @Test
    fun `GIVEN ViewModel created WHEN initial state THEN has default values`() = runTest(testDispatcher) {
        sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
        val states = mutableListOf<AddRackUiState>()
        val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        val state = states.last()
        assertEquals("", state.name)
        assertEquals("", state.description)
        assertEquals("", state.location)
        assertNull(state.photoUri)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isSuccess)
        collectJob.cancel()
    }

    @Test
    fun `GIVEN ViewModel WHEN updateName THEN uiState name updated and error cleared`() = runTest(testDispatcher) {
        sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
        val states = mutableListOf<AddRackUiState>()
        val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }
        advanceUntilIdle()
        fakeSaveRackUseCase.invokeResult = DomainError.ValidationError(reason = "x").err()
        sut.onSaveRack()
        advanceUntilIdle()

        sut.onUpdateName("My Rack")
        advanceUntilIdle()

        val state = states.last()
        assertEquals("My Rack", state.name)
        assertNull(state.error)
        collectJob.cancel()
    }

    @Test
    fun `GIVEN ViewModel WHEN updateDescription THEN uiState description updated`() = runTest(testDispatcher) {
        sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
        val states = mutableListOf<AddRackUiState>()
        val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        sut.onUpdateDescription("desc")
        advanceUntilIdle()

        assertEquals("desc", states.last().description)
        collectJob.cancel()
    }

    @Test
    fun `GIVEN ViewModel WHEN updateLocation THEN uiState location updated`() = runTest(testDispatcher) {
        sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
        val states = mutableListOf<AddRackUiState>()
        val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        sut.onUpdateLocation("garage")
        advanceUntilIdle()

        assertEquals("garage", states.last().location)
        collectJob.cancel()
    }

    @Test
    fun `GIVEN ViewModel WHEN updatePhotoUri THEN uiState photoUri updated`() = runTest(testDispatcher) {
        sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
        val states = mutableListOf<AddRackUiState>()
        val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        sut.onUpdatePhotoUri("content://photo")
        advanceUntilIdle()

        assertEquals("content://photo", states.last().photoUri)
        collectJob.cancel()
    }

    @Test
    fun `GIVEN name is blank WHEN saveRack THEN uiState has error Name is required`() = runTest(testDispatcher) {
        sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
        val states = mutableListOf<AddRackUiState>()
        val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        sut.onSaveRack()
        advanceUntilIdle()

        val state = states.last()
        assertEquals("Name is required", state.error)
        assertFalse(state.isSuccess)
        collectJob.cancel()
    }

    @Test
    fun `GIVEN valid name and save succeeds WHEN saveRack THEN form reset and uiEvent NavigateBack`() =
        runTest(testDispatcher) {
            sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
            val states = mutableListOf<AddRackUiState>()
            val stateCollectJob: Job = launch { sut.uiState.collect { states.add(it) } }
            advanceUntilIdle()
            sut.onUpdateName("Rack 1")
            advanceUntilIdle()
            val saved = Rack(id = "id1", name = "Rack 1")
            fakeSaveRackUseCase.invokeResult = saved.ok()

            val events = mutableListOf<AddRackUiEvent?>()
            val eventCollectJob: Job = launch { sut.uiEvent.collect { events.add(it) } }
            advanceUntilIdle()

            sut.onSaveRack()
            advanceUntilIdle()

            val state = states.last()
            assertEquals("", state.name)
            assertFalse(state.isSuccess)
            assertNull(state.error)
            val event = events.filterNotNull().single()
            assertTrue(event is AddRackUiEvent.NavigateBack)
            stateCollectJob.cancel()
            eventCollectJob.cancel()
        }

    @Test
    fun `GIVEN save returns ValidationError WHEN saveRack THEN uiState has error message`() =
        runTest(testDispatcher) {
            sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
            val states = mutableListOf<AddRackUiState>()
            val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }
            advanceUntilIdle()
            sut.onUpdateName("Rack 1")
            advanceUntilIdle()
            fakeSaveRackUseCase.invokeResult = DomainError.ValidationError(reason = "Invalid name").err()

            sut.onSaveRack()
            advanceUntilIdle()

            val state = states.last()
            assertEquals("Invalid name", state.error)
            assertFalse(state.isSuccess)
            collectJob.cancel()
        }

    @Test
    fun `GIVEN save returns NotFound WHEN saveRack THEN uiState has Rack not found`() =
        runTest(testDispatcher) {
            sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
            val states = mutableListOf<AddRackUiState>()
            val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }
            advanceUntilIdle()
            sut.onUpdateName("Rack 1")
            advanceUntilIdle()
            fakeSaveRackUseCase.invokeResult = DomainError.NotFound(resource = "Rack", id = "x").err()

            sut.onSaveRack()
            advanceUntilIdle()

            val state = states.last()
            assertEquals("Rack not found", state.error)
            assertFalse(state.isSuccess)
            collectJob.cancel()
        }

    @Test
    fun `GIVEN save returns Unknown WHEN saveRack THEN uiState has unknown error message`() =
        runTest(testDispatcher) {
            sut = AddRackViewModel(coroutineScope = testScope, saveRackUseCase = fakeSaveRackUseCase)
            val states = mutableListOf<AddRackUiState>()
            val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }
            advanceUntilIdle()
            sut.onUpdateName("Rack 1")
            advanceUntilIdle()
            fakeSaveRackUseCase.invokeResult = DomainError.Unknown().err()

            sut.onSaveRack()
            advanceUntilIdle()

            val state = states.last()
            assertEquals("Unknown error", state.error)
            assertFalse(state.isSuccess)
            collectJob.cancel()
        }
}
