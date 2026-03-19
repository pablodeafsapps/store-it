package org.deafsapps.storeit.presentation.rack.viewmodel

import org.deafsapps.storeit.presentation.rack.model.RackDetailSlotVo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class RackDetailSlotHitTest {

    @Test
    fun `returns closest slot within radius`() {
        val a = RackDetailSlotVo(id = "a", xRel = 0.5f, yRel = 0.5f)
        val b = RackDetailSlotVo(id = "b", xRel = 0.1f, yRel = 0.1f)
        assertEquals(
            a,
            findNearestSlotWithin(xRel = 0.52f, yRel = 0.52f, slots = listOf(a, b), radiusRel = 0.08f),
        )
    }

    @Test
    fun `returns null when no slot close enough`() {
        val a = RackDetailSlotVo(id = "a", xRel = 0.1f, yRel = 0.1f)
        assertNull(
            findNearestSlotWithin(xRel = 0.9f, yRel = 0.9f, slots = listOf(a), radiusRel = 0.08f),
        )
    }

    @Test
    fun `returns nearest when several are within radius`() {
        val closer = RackDetailSlotVo(id = "c", xRel = 0.5f, yRel = 0.5f)
        val farther = RackDetailSlotVo(id = "f", xRel = 0.55f, yRel = 0.5f)
        assertEquals(
            closer,
            findNearestSlotWithin(xRel = 0.51f, yRel = 0.5f, slots = listOf(farther, closer), radiusRel = 0.15f),
        )
    }
}
