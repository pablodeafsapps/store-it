package org.deafsapps.storeit.androidapp.presentation.rack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.androidapp.fake.FakeGetRacksUseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.presentation.rack.model.RackListUiEvent
import org.deafsapps.storeit.presentation.rack.model.RackListUiState
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RackListViewModelTest {

  private lateinit var sut: RackListViewModel
  private lateinit var fakeGetRacksUseCase: FakeGetRacksUseCase
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @Before
  fun setUp() {
    fakeGetRacksUseCase = FakeGetRacksUseCase()
  }

  @Test
  fun `GIVEN fake returns empty list WHEN ViewModel is created THEN uiState has empty racks`() =
      runTest(testDispatcher) {
          fakeGetRacksUseCase.invokeResult = emptyList<Rack>().ok()
          sut = RackListViewModel(coroutineScope = testScope, getRacksUseCase = fakeGetRacksUseCase)
          val states = mutableListOf<RackListUiState>()
          val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }

          advanceUntilIdle()

          val state = states.firstOrNull { !it.isLoading } ?: states.last()
          assertTrue(state.racks.isEmpty())
          assertNull(state.error)
          collectJob.cancel()
          advanceUntilIdle()
      }

  @Test
  fun `GIVEN fake returns two racks WHEN ViewModel is created THEN uiState has both racks`() =
      runTest(testDispatcher) {
          val rack1 = Rack(id = "1", name = "Rack 1")
          val rack2 = Rack(id = "2", name = "Rack 2")
          fakeGetRacksUseCase.invokeResult = listOf(rack1, rack2).ok()
          sut = RackListViewModel(coroutineScope = testScope, getRacksUseCase = fakeGetRacksUseCase)
          val states = mutableListOf<RackListUiState>()
          val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }

          advanceUntilIdle()

          val state = states.firstOrNull { !it.isLoading } ?: states.last()
          assertEquals(2, state.racks.size)
          assertTrue(state.racks.containsAll(listOf(rack1, rack2)))
          assertNull(state.error)
          collectJob.cancel()
          advanceUntilIdle()
      }

  @Test
  fun `GIVEN fake returns ValidationError WHEN ViewModel is created THEN uiState has error message`() =
      runTest(testDispatcher) {
          fakeGetRacksUseCase.invokeResult =
              DomainError.ValidationError(field = "id", reason = "Invalid id").err()
          sut = RackListViewModel(coroutineScope = testScope, getRacksUseCase = fakeGetRacksUseCase)
          val states = mutableListOf<RackListUiState>()
          val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }

          advanceUntilIdle()

          val state = states.firstOrNull { !it.isLoading } ?: states.last()
          assertEquals("Invalid id", state.error)
          collectJob.cancel()
          advanceUntilIdle()
      }

  @Test
  fun `GIVEN fake returns NotFound WHEN ViewModel is created THEN uiState has Racks not found message`() =
      runTest(testDispatcher) {
          fakeGetRacksUseCase.invokeResult = DomainError.NotFound(resource = "Rack", id = "x").err()
          sut = RackListViewModel(coroutineScope = testScope, getRacksUseCase = fakeGetRacksUseCase)
          val states = mutableListOf<RackListUiState>()
          val collectJob: Job = launch { sut.uiState.collect { states.add(it) } }

          advanceUntilIdle()

          val state = states.firstOrNull { !it.isLoading } ?: states.last()
          assertEquals("Racks not found", state.error)
          collectJob.cancel()
          advanceUntilIdle()
      }

  @Test
  fun `GIVEN any state WHEN onAddRackSelect THEN uiEvent emits NavigateToAddRack`() =
      runTest(testDispatcher) {
          fakeGetRacksUseCase.invokeResult = emptyList<Rack>().ok()
          sut = RackListViewModel(coroutineScope = TestScope(testDispatcher), getRacksUseCase = fakeGetRacksUseCase)
          val events = mutableListOf<RackListUiEvent?>()
          val collectJob: Job = launch { sut.uiEvent.collect { events.add(it) } }
          advanceUntilIdle()

          sut.onAddRackSelect()

          advanceUntilIdle()
          val event = events.filterNotNull().single()
          assertTrue(event is RackListUiEvent.NavigateToAddRack)
          collectJob.cancel()
      }

  @Test
  fun `GIVEN any state WHEN onRackSelect THEN uiEvent emits NavigateToRackDetail with rack id`() =
      runTest(testDispatcher) {
          fakeGetRacksUseCase.invokeResult = emptyList<Rack>().ok()
          sut = RackListViewModel(coroutineScope = testScope, getRacksUseCase = fakeGetRacksUseCase)
          val events = mutableListOf<RackListUiEvent?>()
          val collectJob: Job = launch { sut.uiEvent.collect { events.add(it) } }
          advanceUntilIdle()
          val rack = Rack(id = "r1", name = "My Rack")

          sut.onRackSelect(rack = rack)
          advanceUntilIdle()

          val event = events.filterNotNull().single()
          assertTrue(event is RackListUiEvent.NavigateToRackDetail)
          assertEquals("r1", (event as RackListUiEvent.NavigateToRackDetail).rackId)
          collectJob.cancel()
          advanceUntilIdle()
      }
}
