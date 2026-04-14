package org.deafsapps.storeit.domain.usecase

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.fake.FakeRackRepository

class GetRacksUseCaseTest {
    private lateinit var sut: GetRacksFlowUseCase
    private lateinit var fakeRackRepository: FakeRackRepository
    private lateinit var result: Result<DomainError, List<Rack>>

    @BeforeTest
    fun setUp() {
        fakeRackRepository = FakeRackRepository()
        sut = GetRacksFlowUseCase(rackRepository = fakeRackRepository)
    }

    @Test
    fun `GIVEN fake returns empty list WHEN invoke THEN returns empty list`() = runTest {
        fakeRackRepository.getAllRacksResult = emptyList<Rack>().ok()

        sut(input = Unit).collect { allRacks -> result = allRacks }

        assertTrue(actual = result.isOk)
        val racks = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = racks.size)
    }

    @Test
    fun `GIVEN fake returns two racks WHEN invoke THEN returns both`() = runTest {
        val rack1 = Rack(id = "1", name = "Rack 1")
        val rack2 = Rack(id = "2", name = "Rack 2")
        fakeRackRepository.getAllRacksResult = listOf(rack1, rack2).ok()

        sut(input = Unit).collect { allRacks -> result = allRacks }

        assertTrue(actual = result.isOk)
        val racks = result.getOrNull() ?: emptyList()
        assertEquals(expected = 2, actual = racks.size)
        assertTrue(actual = racks.containsAll(listOf(rack1, rack2)))
    }

    @Test
    fun `GIVEN fake returns error WHEN invoke THEN returns same error`() = runTest {
        fakeRackRepository.getAllRacksResult = DomainError.Unknown().err()

        sut(input = Unit).collect { allRacks -> result = allRacks }

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.Unknown)
    }
}
