package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.deafsapps.storeit.androidapp.NavScreen
import org.deafsapps.storeit.androidapp.StoreItComposeUiTestBase
import org.junit.jupiter.api.Test

internal class RackDetailScreenUiTest : StoreItComposeUiTestBase() {

    @Test
    fun rackDetail_back_returnsToRackList() {
        seedRack(id = "r1", name = "Garage Rack")
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithText("Garage Rack").performClick()
            onNodeWithTag("rackDetailBackButton").assertIsDisplayed()

            onNodeWithTag("rackDetailBackButton").performClick()

            onNodeWithText("Racks").assertIsDisplayed()
        }
    }

    @Test
    fun rackDetail_editAndDelete_cancelFlows() {
        seedRack(id = "r1", name = "Garage Rack")
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithText("Garage Rack").performClick()

            onNodeWithTag("rackDetailOverflowMenuButton").performClick()
            onNodeWithTag("editRackMenuItem").assertIsDisplayed()
            onNodeWithTag("editRackMenuItem").performClick()

            onNodeWithText("Edit rack").assertIsDisplayed()
            onNodeWithTag("editRackSaveButton").assertIsDisplayed()
            onNodeWithTag("editRackCancelButton").performClick()

            onNodeWithText("Edit rack").assertIsNotDisplayed()

            onNodeWithTag("rackDetailOverflowMenuButton").performClick()
            onNodeWithTag("removeRackMenuItem").performClick()

            onNodeWithText("Remove rack?").assertIsDisplayed()
            onNodeWithTag("removeRackCancelButton").performClick()

            onNodeWithText("Remove rack?").assertIsNotDisplayed()
        }
    }

    @Test
    fun rackDetail_tapExistingSlot_showsStoredItemsSheet() {
        seedRack(id = "r1", name = "Garage Rack")
        seedSlot(rackId = "r1", slotId = "s1", xRel = 0.5f, yRel = 0.5f)
        seedItem(id = "i1", rackId = "r1", slotId = "s1", name = "Drill")
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithText("Garage Rack").performClick()

            onNodeWithTag("rackDetailImageOverlay").performTouchInput { click() }

            onNodeWithTag("slotItemsSheetTitle").assertIsDisplayed()
            onNodeWithText("Drill").assertIsDisplayed()
            onNodeWithTag("slotItemsSheetCloseButton").performClick()
            onNodeWithTag("slotItemsSheetTitle").assertDoesNotExist()
        }
    }

    @Test
    fun rackDetail_delete_removesRack() {
        seedRack(id = "r1", name = "Garage Rack")
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithText("Garage Rack").performClick()
            onNodeWithTag("rackDetailOverflowMenuButton").performClick()
            onNodeWithTag("removeRackMenuItem").performClick()
            onNodeWithTag("removeRackConfirmButton").performClick()

            onNodeWithText("No racks yet").assertIsDisplayed()
        }
    }
}
