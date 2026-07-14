package org.deafsapps.storeit.data.datasource

import kotlinx.coroutines.test.runTest
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.domain.model.Item
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FirebaseItemDataSourceTest {
    private lateinit var sut: ItemDataSource

    @BeforeTest
    fun setUp() {
        sut = FirebaseItemDataSource()
    }

    @Test
    fun `GIVEN firebase placeholder WHEN getItemsByRack THEN returns empty list`() = runTest {
        val result = sut.getItemsByRack(rackId = "rack-1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = emptyList<Item>(), actual = result.getOrNull())
    }

    @Test
    fun `GIVEN firebase placeholder WHEN getItemsBySlot THEN returns empty list`() = runTest {
        val result = sut.getItemsBySlot(rackId = "rack-1", slotId = "slot-1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = emptyList<Item>(), actual = result.getOrNull())
    }

    @Test
    fun `GIVEN firebase placeholder WHEN getItemById THEN returns null`() = runTest {
        val result = sut.getItemById(id = "item-1")

        assertTrue(actual = result.isOk)
        assertNull(actual = result.getOrNull())
    }

    @Test
    fun `GIVEN firebase placeholder WHEN searchItems THEN returns empty list`() = runTest {
        val result = sut.searchItems(query = "hammer")

        assertTrue(actual = result.isOk)
        assertEquals(expected = emptyList<Item>(), actual = result.getOrNull())
    }

    @Test
    fun `GIVEN firebase placeholder WHEN saveItem THEN returns item unchanged`() = runTest {
        val item = Item(
            id = "item-1",
            rackId = "rack-1",
            slotId = "slot-1",
            name = "Hammer",
        )

        val result = sut.saveItem(item = item)

        assertTrue(actual = result.isOk)
        assertEquals(expected = item, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN firebase placeholder WHEN deleteItem THEN returns false`() = runTest {
        val result = sut.deleteItem(id = "item-1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = false, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN firebase placeholder WHEN deleteItemsByRack THEN returns success`() = runTest {
        val result = sut.deleteItemsByRack(rackId = "rack-1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = 0L, actual = result.getOrNull())
    }
}
