package org.deafsapps.storeit.presentation.rack.viewmodel.model

import org.deafsapps.storeit.presentation.rack.model.RackSlotMarkerVo
import org.deafsapps.storeit.presentation.rack.viewmodel.findNearestSlotWithinOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class RackSlotMarkerHitTest {

    @Test
    fun `returns closest slot within radius`() {
        val markerA = RackSlotMarkerVo(id = "a", xRel = 0.5f, yRel = 0.5f)
        val markerB = RackSlotMarkerVo(id = "b", xRel = 0.1f, yRel = 0.1f)
        assertEquals(
            markerA,
            listOf(markerA, markerB).findNearestSlotWithinOrNull(
                xRel = 0.52f,
                yRel = 0.52f,
                radiusRel = 0.08f
            ),
        )
    }

    @Test
    fun `returns null when no slot close enough`() {
        val markerA = RackSlotMarkerVo(id = "a", xRel = 0.1f, yRel = 0.1f)
        assertNull(
            listOf(markerA).findNearestSlotWithinOrNull(
                xRel = 0.9f,
                yRel = 0.9f,
                radiusRel = 0.08f
            ),
        )
    }

    @Test
    fun `returns nearest when several are within radius`() {
        val closerMarker = RackSlotMarkerVo(id = "c", xRel = 0.5f, yRel = 0.5f)
        val fartherMarker = RackSlotMarkerVo(id = "f", xRel = 0.55f, yRel = 0.5f)
        assertEquals(
            closerMarker,
            listOf(fartherMarker, closerMarker).findNearestSlotWithinOrNull(
                xRel = 0.51f,
                yRel = 0.5f,
                radiusRel = 0.15f
            ),
        )
    }
}