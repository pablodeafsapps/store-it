package org.deafsapps.storeit.data.local.db

import org.deafsapps.storeit.data.database.decodeTags
import org.deafsapps.storeit.data.database.encodeTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SqlDelightCodecTest {

    @Test
    fun `encode and decode tags keep values`() {
        val tags = listOf("tools", "garage", "heavy")

        val encoded = encodeTags(tags = tags)
        val decoded = decodeTags(encoded = encoded)

        assertEquals(expected = tags, actual = decoded)
    }

    @Test
    fun `decode empty payload returns empty list`() {
        val decoded = decodeTags(encoded = "")

        assertTrue(actual = decoded.isEmpty())
    }
}
