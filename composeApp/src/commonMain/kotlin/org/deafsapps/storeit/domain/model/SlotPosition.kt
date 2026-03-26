package org.deafsapps.storeit.domain.model

interface SlotPosition {
    val x: Float
    val y: Float
    val xRel: Float
    val yRel: Float
}

internal data class SlotPositionModel(
    override val x: Float,
    override val y: Float,
    override val xRel: Float,
    override val yRel: Float,
) : SlotPosition

fun SlotPosition(
    x: Float,
    y: Float,
    xRel: Float,
    yRel: Float,
): SlotPosition = SlotPositionModel(
    x = x,
    y = y,
    xRel = xRel,
    yRel = yRel,
)

internal fun SlotPosition.asModel(): SlotPositionModel = when (this) {
    is SlotPositionModel -> this
    else -> SlotPositionModel(x = x, y = y, xRel = xRel, yRel = yRel)
}
