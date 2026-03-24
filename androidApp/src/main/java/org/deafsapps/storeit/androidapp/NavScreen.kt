package org.deafsapps.storeit.androidapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface NavScreen : NavKey {
    @Serializable
    data object RackList : NavScreen

    @Serializable
    data object Search : NavScreen

    @Serializable
    data object AddRack : NavScreen

    @Serializable
    data class RackDetail(val rackId: String) : NavScreen

    @Serializable
    data class SlotItems(val rackId: String, val slotId: String) : NavScreen

    @Serializable
    data class ItemDetail(
        val itemId: String,
        val rackId: String,
        val slotId: String,
        val fromSearch: Boolean = false,
    ) : NavScreen

    @Serializable
    data class AddItem(val rackId: String? = null, val slotId: String? = null) : NavScreen
}
