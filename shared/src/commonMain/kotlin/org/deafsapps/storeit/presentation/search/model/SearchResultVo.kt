package org.deafsapps.storeit.presentation.search.model

data class SearchResultVo(
    val itemId: String,
    val rackId: String,
    val slotId: String,
    val itemName: String,
    val rackName: String,
    val slotSummary: String,
)
