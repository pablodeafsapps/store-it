package org.deafsapps.storeit.domain.model

import kotlin.time.Clock

internal data class Rack(
    val id: String,
    val name: String,
    val description: String = "",
    val location: String = "",
    val photoUri: String? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long? = null,
)
