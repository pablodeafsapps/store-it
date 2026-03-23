package org.deafsapps.storeit.androidapp.presentation.search.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.deafsapps.storeit.androidapp.StoreItComposeUiTestBase
import org.junit.jupiter.api.Test

internal class SearchScreenUiTest : StoreItComposeUiTestBase() {

    @Test
    fun search_findsItem_navigatesToItemDetail() {
        seedRack(id = "r1", name = "Garage Rack")
        seedSlot(rackId = "r1", slotId = "s1", xRel = 0.5f, yRel = 0.5f)
        seedItem(id = "i1", rackId = "r1", slotId = "s1", name = "UniqueSearchBolt")
        composeUiTest {
            onNodeWithTag("rackListSearchField").performClick()
            onNodeWithTag("searchScreenQueryField").performTextInput("UniqueSearch")
            Thread.sleep(600)
            onNodeWithTag("searchResultRow_i1").performClick()
            onNodeWithTag("itemDetailScreenTitle").assertIsDisplayed()
        }
    }

    @Test
    fun search_noMatches_showsNoResultsState() {
        seedRack(id = "r1", name = "Garage Rack")
        seedSlot(rackId = "r1", slotId = "s1", xRel = 0.5f, yRel = 0.5f)
        seedItem(id = "i1", rackId = "r1", slotId = "s1", name = "Hammer")
        composeUiTest {
            onNodeWithTag("rackListSearchField").performClick()
            onNodeWithTag("searchScreenQueryField").performTextInput("zzznomatchzzz")
            Thread.sleep(600)
            onNodeWithTag("searchScreenNoResults").assertIsDisplayed()
        }
    }
}
