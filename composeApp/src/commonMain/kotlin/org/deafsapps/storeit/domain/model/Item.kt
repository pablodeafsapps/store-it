package org.deafsapps.storeit.domain.model

import kotlin.time.Clock

data class Item(
    val id: String,
    val rackId: String,
    val slotId: String,
    val name: String,
    val description: String = "",
    val photoUri: String? = null,
    val quantity: Int? = null,
    val owner: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long? = null,
)
