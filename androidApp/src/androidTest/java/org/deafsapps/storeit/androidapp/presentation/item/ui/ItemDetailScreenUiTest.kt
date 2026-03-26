package org.deafsapps.storeit.androidapp.presentation.item.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import org.deafsapps.storeit.androidapp.NavScreen
import org.deafsapps.storeit.androidapp.StoreItComposeUiTestBase
import org.junit.jupiter.api.Test

internal class ItemDetailScreenUiTest : StoreItComposeUiTestBase() {

    @Test
    fun slotItems_tapItem_showsItemDetail_saveReturnsToSlotListWithUpdatedName() {
        seedRack(id = "r1", name = "Garage Rack")
        seedSlot(rackId = "r1", slotId = "s1", xRel = 0.5f, yRel = 0.5f)
        seedItem(id = "i1", rackId = "r1", slotId = "s1", name = "Drill")
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithText("Garage Rack").performClick()
            onNodeWithTag("rackDetailImageOverlay").performTouchInput { click() }

            onNodeWithTag("slotItemRow_i1").performClick()

            onNodeWithTag("itemDetailScreenTitle").assertIsDisplayed()
            onNodeWithTag("itemDetailNameField").performTextReplacement("Drill updated")
            onNodeWithTag("itemDetailSaveButton").performClick()

            onNodeWithTag("slotItemsScreenTitle").assertIsDisplayed()
            onNodeWithText("Drill updated").assertIsDisplayed()
        }
    }
}
