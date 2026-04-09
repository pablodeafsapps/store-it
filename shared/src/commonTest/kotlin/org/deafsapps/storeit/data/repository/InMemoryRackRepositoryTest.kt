package org.deafsapps.storeit.data.repository

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.repository.RackRepository

class InMemoryRackRepositoryTest {
    private lateinit var sut: RackRepository
    private lateinit var result: Result<DomainError, List<Rack>>

    @BeforeTest
    fun setUp() {
        sut = InMemoryRackRepository()
    }

    @Test
    fun `GIVEN empty repository WHEN getAllRacks THEN returns empty list`() = runTest {

        result = sut.getAllRacksFlow().first()

        assertTrue(actual = result.isOk)
        val racks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = racks.size)
    }

    @Test
    fun `GIVEN two saved racks WHEN getAllRacks THEN returns both`() = runTest {
        val rack1 = Rack(id = "1", name = "Rack 1")
        val rack2 = Rack(id = "2", name = "Rack 2")
        sut.saveRack(rack = rack1)
        sut.saveRack(rack = rack2)

        result = sut.getAllRacksFlow().first()

        assertTrue(actual = result.isOk)
        val racks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 2, actual = racks.size)
        assertTrue(actual = racks.containsAll(listOf(rack1, rack2)))
    }

    @Test
    fun `GIVEN saved rack WHEN getRackById with existing id THEN returns rack`() = runTest {
        val rack = Rack(id = "1", name = "Test Rack")
        val saveResult = sut.saveRack(rack = rack)
        assertTrue(actual = saveResult.isOk)
        val savedRack = saveResult.getOrNull()!!

        val result = sut.getRackById(id = "1")

        assertTrue(actual = result.isOk)
        val retrievedRack = result.getOrNull()
        assertEquals(expected = savedRack, actual = retrievedRack)
    }

    @Test
    fun `GIVEN empty repository WHEN getRackById with unknown id THEN returns NotFound`() = runTest {

        val result = sut.getRackById(id = "nonexistent")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.NotFound)
        assertEquals(expected = "Rack", actual = error.resource)
        assertEquals(expected = "nonexistent", actual = error.id)
    }

    @Test
    fun `GIVEN any state WHEN getRackById with blank id THEN returns ValidationError`() = runTest {

        val result = sut.getRackById(id = "")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "id", actual = error.field)
    }

    @Test
    fun `GIVEN empty repository WHEN saveRack with valid rack THEN creates and returns rack`() = runTest {
        val rack = Rack(id = "1", name = "New Rack", description = "Description")

        val result = sut.saveRack(rack = rack)

        assertTrue(actual = result.isOk)
        val savedRack = result.getOrNull()
        assertEquals(expected = rack.id, actual = savedRack?.id)
        assertEquals(expected = rack.name, actual = savedRack?.name)
        assertEquals(expected = rack.description, actual = savedRack?.description)
    }

    @Test
    fun `GIVEN existing rack WHEN saveRack with same id THEN updates and returns rack`() = runTest {
        val rack = Rack(id = "1", name = "Original Name")
        sut.saveRack(rack = rack)
        val updatedRack = Rack(
            id = rack.id,
            name = "Updated Name",
            description = "New Description",
            location = rack.location,
            photoUri = rack.photoUri,
            createdAt = rack.createdAt,
            updatedAt = rack.updatedAt,
        )

        val result = sut.saveRack(rack =updatedRack)

        assertTrue(actual = result.isOk)
        val savedRack = result.getOrNull()
        assertEquals(expected = "Updated Name", actual = savedRack?.name)
        assertEquals(expected = "New Description", actual = savedRack?.description)
        assertTrue(actual = savedRack?.updatedAt != null)
    }

    @Test
    fun `GIVEN any state WHEN saveRack with blank id THEN returns ValidationError`() = runTest {
        val rack = Rack(id = "", name = "Invalid Rack")

        val result = sut.saveRack(rack = rack)

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "id", actual = error.field)
    }

    @Test
    fun `GIVEN saved rack WHEN deleteRack with existing id THEN removes rack`() = runTest {
        val rack = Rack(id = "1", name = "To Delete")
        sut.saveRack(rack = rack)

        val deleteResult = sut.deleteRack(id = "1")

        assertTrue(actual = deleteResult.isOk)
        val getResult = sut.getRackById(id = "1")
        assertTrue(actual = getResult.isErr)
        val error = getResult.failureOrNull()
        assertTrue(actual = error is DomainError.NotFound)
    }

    @Test
    fun `GIVEN empty repository WHEN deleteRack with unknown id THEN returns NotFound`() = runTest {

        val result = sut.deleteRack(id = "nonexistent")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.NotFound)
        assertEquals(expected = "Rack", actual = error.resource)
    }

    @Test
    fun `GIVEN any state WHEN deleteRack with blank id THEN returns ValidationError`() = runTest {

        val result = sut.deleteRack(id = "")

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "id", actual = error.field)
    }

    @Test
    fun `GIVEN two saved racks WHEN clear THEN getAllRacks returns empty list`() = runTest {
        sut.saveRack(rack =Rack(id = "1", name = "Rack 1"))
        sut.saveRack(rack =Rack(id = "2", name = "Rack 2"))

        sut.clear()
        result = sut.getAllRacksFlow().first()

        assertTrue(actual = result.isOk)
        val racks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = racks.size)
    }

    @Test
    fun `GIVEN empty repository WHEN concurrent saves THEN all racks are saved correctly`() = runTest {
        val racks = (1..100).map { i ->
            Rack(id = "rack$i", name = "Rack $i")
        }

        racks.map { rack ->
            async { sut.saveRack(rack = rack) }
        }.awaitAll()

        result = sut.getAllRacksFlow().first()
        assertTrue(actual = result.isOk)
        val savedRacks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 100, actual = savedRacks.size)
        val savedIds = savedRacks.map { it.id }.toSet()
        val expectedIds = racks.map { it.id }.toSet()
        assertEquals(expected = expectedIds, actual = savedIds)
    }

    @Test
    fun `GIVEN saved racks WHEN concurrent reads THEN all reads succeed without race conditions`() = runTest {
        val racks = (1..50).map { i ->
            Rack(id = "rack$i", name = "Rack $i")
        }
        racks.forEach { sut.saveRack(rack =it) }

        val readResults = (1..50).map { i ->
            async { sut.getRackById(id = "rack$i") }
        }.awaitAll()

        assertEquals(expected = 50, actual = readResults.size)
        readResults.forEach { result ->
            assertTrue(actual = result.isOk)
            assertTrue(actual = result.getOrNull() != null)
        }
    }

    @Test
    fun `GIVEN saved racks WHEN concurrent writes and reads THEN operations complete without race conditions`() =
        runTest {
            val initialRacks = (1..20).map { i ->
                Rack(id = "rack$i", name = "Rack $i")
            }
            initialRacks.forEach { sut.saveRack(rack =it) }

            val writeJobs = (21..40).map { i ->
                async {
                    sut.saveRack(rack =Rack(id = "rack$i", name = "Rack $i"))
                }
            }
            val readJobs = (1..20).map { i ->
                async { sut.getRackById(id = "rack$i") }
            }

            writeJobs.awaitAll()
            val readResults = readJobs.awaitAll()

            assertEquals(expected = 20, actual = readResults.size)
            readResults.forEach { result ->
                assertTrue(actual = result.isOk)
            }
            result = sut.getAllRacksFlow().first()
            assertTrue(actual = result.isOk)
            val allRacks: List<Rack> = result.getOrNull() ?: emptyList()
            assertEquals(expected = 40, actual = allRacks.size)
        }

    @Test
    fun `GIVEN saved racks WHEN concurrent deletes THEN racks are deleted correctly`() = runTest {
        val racks = (1..50).map { i ->
            Rack(id = "rack$i", name = "Rack $i")
        }
        racks.forEach { sut.saveRack(rack =it) }

        val deleteJobs = (1..50).map { i ->
            async { sut.deleteRack(id = "rack$i") }
        }.awaitAll()

        assertEquals(expected = 50, actual = deleteJobs.size)
        deleteJobs.forEach { result ->
            assertTrue(actual = result.isOk)
        }
        result = sut.getAllRacksFlow().first()
        assertTrue(actual = result.isOk)
        val remainingRacks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 0, actual = remainingRacks.size)
    }

    @Test
    fun `GIVEN saved racks WHEN concurrent mixed operations THEN no race conditions occur`() = runTest {
        val initialRacks = (1..30).map { i ->
            Rack(id = "rack$i", name = "Rack $i")
        }
        initialRacks.forEach { sut.saveRack(rack =it) }

        val saveJobs = (31..50).map { i ->
            async {
                sut.saveRack(rack =Rack(id = "rack$i", name = "Rack $i"))
            }
        }
        val readJobs = (1..30).map { i ->
            async { sut.getRackById(id = "rack$i") }
        }
        val deleteJobs = (1..10).map { i ->
            async { sut.deleteRack(id = "rack$i") }
        }
        val getAllJobs = (1..5).map {
            async { sut.getAllRacksFlow() }
        }

        saveJobs.awaitAll()
        readJobs.awaitAll()
        deleteJobs.awaitAll()
        getAllJobs.awaitAll()

        result = sut.getAllRacksFlow().first()
        assertTrue(actual = result.isOk)
        val allRacks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(expected = 40, actual = allRacks.size)
        (1..10).forEach { i ->
            val getResult = sut.getRackById(id = "rack$i")
            assertTrue(actual = getResult.isErr)
        }
    }
}
