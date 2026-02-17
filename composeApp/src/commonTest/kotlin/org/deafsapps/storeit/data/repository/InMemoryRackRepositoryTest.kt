package org.deafsapps.storeit.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack

class InMemoryRackRepositoryTest {
    private val repository = InMemoryRackRepository()

    @Test
    fun `GIVEN empty repository WHEN getAllRacks THEN returns empty list`() = runTest {

        val result = repository.getAllRacks()

        assertTrue(result.isOk)
        val racks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(0, racks.size)
    }

    @Test
    fun `GIVEN two saved racks WHEN getAllRacks THEN returns both`() = runTest {
        val rack1 = Rack(id = "1", name = "Rack 1")
        val rack2 = Rack(id = "2", name = "Rack 2")
        repository.saveRack(rack1)
        repository.saveRack(rack2)

        val result = repository.getAllRacks()

        assertTrue(result.isOk)
        val racks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(2, racks.size)
        assertTrue(racks.containsAll(listOf(rack1, rack2)))
    }

    @Test
    fun `GIVEN saved rack WHEN getRackById with existing id THEN returns rack`() = runTest {
        val rack = Rack(id = "1", name = "Test Rack")
        repository.saveRack(rack)

        val result = repository.getRackById("1")

        assertTrue(result.isOk)
        val retrievedRack = result.getOrNull()
        assertEquals(rack, retrievedRack)
    }

    @Test
    fun `GIVEN empty repository WHEN getRackById with unknown id THEN returns NotFound`() = runTest {

        val result = repository.getRackById("nonexistent")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.NotFound)
        assertEquals("Rack", error.resource)
        assertEquals("nonexistent", error.id)
    }

    @Test
    fun `GIVEN any state WHEN getRackById with blank id THEN returns ValidationError`() = runTest {

        val result = repository.getRackById("")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("id", error.field)
    }

    @Test
    fun `GIVEN empty repository WHEN saveRack with valid rack THEN creates and returns rack`() = runTest {
        val rack = Rack(id = "1", name = "New Rack", description = "Description")

        val result = repository.saveRack(rack)

        assertTrue(result.isOk)
        val savedRack = result.getOrNull()
        assertEquals(rack.id, savedRack?.id)
        assertEquals(rack.name, savedRack?.name)
        assertEquals(rack.description, savedRack?.description)
    }

    @Test
    fun `GIVEN existing rack WHEN saveRack with same id THEN updates and returns rack`() = runTest {
        val rack = Rack(id = "1", name = "Original Name")
        repository.saveRack(rack)
        val updatedRack = rack.copy(name = "Updated Name", description = "New Description")

        val result = repository.saveRack(updatedRack)

        assertTrue(result.isOk)
        val savedRack = result.getOrNull()
        assertEquals("Updated Name", savedRack?.name)
        assertEquals("New Description", savedRack?.description)
        assertTrue(savedRack?.updatedAt != null)
    }

    @Test
    fun `GIVEN any state WHEN saveRack with blank id THEN returns ValidationError`() = runTest {
        val rack = Rack(id = "", name = "Invalid Rack")

        val result = repository.saveRack(rack)

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("id", error.field)
    }

    @Test
    fun `GIVEN saved rack WHEN deleteRack with existing id THEN removes rack`() = runTest {
        val rack = Rack(id = "1", name = "To Delete")
        repository.saveRack(rack)

        val deleteResult = repository.deleteRack("1")

        assertTrue(deleteResult.isOk)
        val getResult = repository.getRackById("1")
        assertTrue(getResult.isErr)
        val error = getResult.failureOrNull()
        assertTrue(error is DomainError.NotFound)
    }

    @Test
    fun `GIVEN empty repository WHEN deleteRack with unknown id THEN returns NotFound`() = runTest {

        val result = repository.deleteRack("nonexistent")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.NotFound)
        assertEquals("Rack", error.resource)
    }

    @Test
    fun `GIVEN any state WHEN deleteRack with blank id THEN returns ValidationError`() = runTest {

        val result = repository.deleteRack("")

        assertTrue(result.isErr)
        val error = result.failureOrNull()
        assertTrue(error is DomainError.ValidationError)
        assertEquals("id", error.field)
    }

    @Test
    fun `GIVEN two saved racks WHEN clear THEN getAllRacks returns empty list`() = runTest {
        repository.saveRack(Rack(id = "1", name = "Rack 1"))
        repository.saveRack(Rack(id = "2", name = "Rack 2"))

        repository.clear()
        val result = repository.getAllRacks()

        assertTrue(result.isOk)
        val racks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(0, racks.size)
    }

    @Test
    fun `GIVEN empty repository WHEN concurrent saves THEN all racks are saved correctly`() = runTest {
        val racks = (1..100).map { i ->
            Rack(id = "rack$i", name = "Rack $i")
        }

        racks.map { rack ->
            async { repository.saveRack(rack) }
        }.awaitAll()

        val result = repository.getAllRacks()
        assertTrue(result.isOk)
        val savedRacks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(100, savedRacks.size)
        val savedIds = savedRacks.map { it.id }.toSet()
        val expectedIds = racks.map { it.id }.toSet()
        assertEquals(expectedIds, savedIds)
    }

    @Test
    fun `GIVEN saved racks WHEN concurrent reads THEN all reads succeed without race conditions`() = runTest {
        val racks = (1..50).map { i ->
            Rack(id = "rack$i", name = "Rack $i")
        }
        racks.forEach { repository.saveRack(it) }

        val readResults = (1..50).map { i ->
            async { repository.getRackById("rack$i") }
        }.awaitAll()

        assertEquals(50, readResults.size)
        readResults.forEach { result ->
            assertTrue(result.isOk)
            assertTrue(result.getOrNull() != null)
        }
    }

    @Test
    fun `GIVEN saved racks WHEN concurrent writes and reads THEN operations complete without race conditions`() =
        runTest {
            val initialRacks = (1..20).map { i ->
                Rack(id = "rack$i", name = "Rack $i")
            }
            initialRacks.forEach { repository.saveRack(it) }

            val writeJobs = (21..40).map { i ->
                async {
                    repository.saveRack(Rack(id = "rack$i", name = "Rack $i"))
                }
            }
            val readJobs = (1..20).map { i ->
                async { repository.getRackById("rack$i") }
            }

            writeJobs.awaitAll()
            val readResults = readJobs.awaitAll()

            assertEquals(20, readResults.size)
            readResults.forEach { result ->
                assertTrue(result.isOk)
            }
            val allRacksResult = repository.getAllRacks()
            assertTrue(allRacksResult.isOk)
            val allRacks: List<Rack> = allRacksResult.getOrNull() ?: emptyList()
            assertEquals(40, allRacks.size)
        }

    @Test
    fun `GIVEN saved racks WHEN concurrent deletes THEN racks are deleted correctly`() = runTest {
        val racks = (1..50).map { i ->
            Rack(id = "rack$i", name = "Rack $i")
        }
        racks.forEach { repository.saveRack(it) }

        val deleteJobs = (1..50).map { i ->
            async { repository.deleteRack("rack$i") }
        }.awaitAll()

        assertEquals(50, deleteJobs.size)
        deleteJobs.forEach { result ->
            assertTrue(result.isOk)
        }
        val result = repository.getAllRacks()
        assertTrue(result.isOk)
        val remainingRacks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(0, remainingRacks.size)
    }

    @Test
    fun `GIVEN saved racks WHEN concurrent mixed operations THEN no race conditions occur`() = runTest {
        val initialRacks = (1..30).map { i ->
            Rack(id = "rack$i", name = "Rack $i")
        }
        initialRacks.forEach { repository.saveRack(it) }

        val saveJobs = (31..50).map { i ->
            async {
                repository.saveRack(Rack(id = "rack$i", name = "Rack $i"))
            }
        }
        val readJobs = (1..30).map { i ->
            async { repository.getRackById("rack$i") }
        }
        val deleteJobs = (1..10).map { i ->
            async { repository.deleteRack("rack$i") }
        }
        val getAllJobs = (1..5).map {
            async { repository.getAllRacks() }
        }

        saveJobs.awaitAll()
        readJobs.awaitAll()
        deleteJobs.awaitAll()
        getAllJobs.awaitAll()

        val result = repository.getAllRacks()
        assertTrue(result.isOk)
        val allRacks: List<Rack> = result.getOrNull() ?: emptyList()
        assertEquals(40, allRacks.size)
        (1..10).forEach { i ->
            val getResult = repository.getRackById("rack$i")
            assertTrue(getResult.isErr)
        }
    }
}
