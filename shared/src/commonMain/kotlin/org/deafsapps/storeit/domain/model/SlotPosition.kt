package org.deafsapps.storeit.domain.model

/**
 * Describes a slot position using absolute and relative coordinates inside a rack image.
 */
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

/**
 * Creates a public [SlotPosition] value for rack-slot mapping.
 */
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
