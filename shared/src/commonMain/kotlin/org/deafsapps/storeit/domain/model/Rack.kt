package org.deafsapps.storeit.domain.model

import kotlin.time.Clock

/**
 * Represents a storage rack and its editable metadata.
 */
interface Rack {
    val id: String
    val name: String
    val description: String
    val location: String
    val photoUri: String?
    val createdAt: Long
    val updatedAt: Long?
}

internal data class RackModel(
    override val id: String,
    override val name: String,
    override val description: String = "",
    override val location: String = "",
    override val photoUri: String? = null,
    override val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    override val updatedAt: Long? = null,
) : Rack

/**
 * Creates a public [Rack] instance while keeping the concrete implementation internal to the module.
 */
fun Rack(
    id: String,
    name: String,
    description: String = "",
    location: String = "",
    photoUri: String? = null,
    createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    updatedAt: Long? = null,
): Rack = RackModel(
    id = id,
    name = name,
    description = description,
    location = location,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Rack.asModel(): RackModel = when (this) {
    is RackModel -> this
    else -> RackModel(
        id = id,
        name = name,
        description = description,
        location = location,
        photoUri = photoUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
