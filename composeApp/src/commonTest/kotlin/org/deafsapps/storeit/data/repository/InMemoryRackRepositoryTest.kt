package org.deafsapps.storeit.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
}
