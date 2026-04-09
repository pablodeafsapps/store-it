package org.deafsapps.storeit.data.datasource

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.domain.model.Rack
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FirebaseRackDataSourceTest {
    private lateinit var sut: RackDataSource

    @BeforeTest
    fun setUp() {
        sut = FirebaseRackDataSource()
    }

    @Test
    fun `GIVEN firebase placeholder WHEN getAllRacksFlow THEN returns empty list`() = runTest {
        val result = sut.getAllRacksFlow().first()

        assertTrue(actual = result.isOk)
        assertEquals(expected = emptyList<Rack>(), actual = result.getOrNull())
    }

    @Test
    fun `GIVEN firebase placeholder WHEN getRackById THEN returns null`() = runTest {
        val result = sut.getRackById(id = "rack-1")

        assertTrue(actual = result.isOk)
        assertNull(actual = result.getOrNull())
    }

    @Test
    fun `GIVEN firebase placeholder WHEN saveRack THEN returns rack unchanged`() = runTest {
        val rack = Rack(id = "rack-1", name = "Garage")

        val result = sut.saveRack(rack = rack)

        assertTrue(actual = result.isOk)
        assertEquals(expected = rack, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN firebase placeholder WHEN deleteRack THEN returns false`() = runTest {
        val result = sut.deleteRack(id = "rack-1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = false, actual = result.getOrNull())
    }
}
