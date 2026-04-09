package org.deafsapps.storeit.domain.model

import androidx.compose.runtime.Stable
import kotlin.time.Clock

@Stable
/**
 * Represents an item stored in a rack slot.
 */
interface Item {
    val id: String
    val rackId: String
    val slotId: String
    val name: String
    val description: String
    val photoUri: String?
    val quantity: Int?
    val owner: String
    val tags: List<String>
    val createdAt: Long
    val updatedAt: Long?
}

@Stable
internal data class ItemModel(
    override val id: String,
    override val rackId: String,
    override val slotId: String,
    override val name: String,
    override val description: String = "",
    override val photoUri: String? = null,
    override val quantity: Int? = null,
    override val owner: String = "",
    override val tags: List<String> = emptyList(),
    override val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    override val updatedAt: Long? = null,
) : Item

/**
 * Creates a public [Item] instance while keeping the concrete implementation internal to the module.
 */
fun Item(
    id: String,
    rackId: String,
    slotId: String,
    name: String,
    description: String = "",
    photoUri: String? = null,
    quantity: Int? = null,
    owner: String = "",
    tags: List<String> = emptyList(),
    createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    updatedAt: Long? = null,
): Item = ItemModel(
    id = id,
    rackId = rackId,
    slotId = slotId,
    name = name,
    description = description,
    photoUri = photoUri,
    quantity = quantity,
    owner = owner,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Item.asModel(): ItemModel = when (this) {
    is ItemModel -> this
    else -> ItemModel(
        id = id,
        rackId = rackId,
        slotId = slotId,
        name = name,
        description = description,
        photoUri = photoUri,
        quantity = quantity,
        owner = owner,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
