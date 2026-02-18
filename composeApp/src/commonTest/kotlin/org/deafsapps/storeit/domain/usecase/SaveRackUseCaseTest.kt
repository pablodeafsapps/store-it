package org.deafsapps.storeit.domain.usecase

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.fake.FakeRackRepository

class SaveRackUseCaseTest {
    private lateinit var fakeRackRepository: FakeRackRepository
    private lateinit var sut: SaveRackUseCase

    @BeforeTest
    fun setUp() {
        fakeRackRepository = FakeRackRepository()
        sut = SaveRackUseCase(rackRepository = fakeRackRepository)
    }

    @Test
    fun `GIVEN fake returns saved rack WHEN invoke with valid rack THEN returns saved rack`() = runTest {
        val rack = Rack(id = "1", name = "New Rack", description = "Desc")
        fakeRackRepository.saveRackResult = rack.ok()

        val result = sut(input = rack)

        assertTrue(actual = result.isOk)
        val saved = result.getOrNull()
        assertEquals(expected = rack.id, actual = saved?.id)
        assertEquals(expected = rack.name, actual = saved?.name)
        assertEquals(expected = rack.description, actual = saved?.description)
    }

    @Test
    fun `GIVEN fake returns updated rack WHEN invoke with same id THEN returns updated rack`() = runTest {
        val rack = Rack(id = "1", name = "Original")
        val updated = rack.copy(name = "Updated", description = "New desc")
        fakeRackRepository.saveRackResult = updated.copy(updatedAt = 1L).ok()

        val result = sut(input = updated)

        assertTrue(actual = result.isOk)
        val saved = result.getOrNull()
        assertEquals(expected = "Updated", actual = saved?.name)
        assertEquals(expected = "New desc", actual = saved?.description)
        assertTrue(actual = saved?.updatedAt != null)
    }

    @Test
    fun `GIVEN fake returns ValidationError WHEN invoke with blank id THEN returns ValidationError`() = runTest {
        val rack = Rack(id = "", name = "Invalid")
        fakeRackRepository.saveRackResult = DomainError.ValidationError(field = "id", reason = "ID cannot be blank").err()

        val result = sut(input = rack)

        assertTrue(actual = result.isErr)
        val error = result.failureOrNull()
        assertTrue(actual = error is DomainError.ValidationError)
        assertEquals(expected = "id", actual = error.field)
    }
}
