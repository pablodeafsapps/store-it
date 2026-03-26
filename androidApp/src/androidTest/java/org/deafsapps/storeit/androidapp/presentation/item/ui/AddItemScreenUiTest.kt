package org.deafsapps.storeit.androidapp.presentation.item.ui

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

internal class AddItemScreenUiTest : StoreItComposeUiTestBase() {

    @Test
    fun addItem_saveWithoutSelection_showsError() {
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithTag("rackListOverflowMenuButton").performClick()
            onNodeWithTag("rackListAddItemMenuItem").performClick()

            onNodeWithText("Add Item").assertIsDisplayed()
            onNodeWithTag("saveItemButton").performClick()

            onNodeWithText("Select a rack and slot to place the item").assertIsDisplayed()
        }
    }

    @Test
    fun addItem_noRacks_selectRackFlow_navigatesToAddRack() {
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithTag("rackListOverflowMenuButton").performClick()
            onNodeWithTag("rackListAddItemMenuItem").performClick()

            onNodeWithTag("selectRackAndSlotButton").performClick()

            onNodeWithText("You need at least one rack to place an item. Create one first.")
                .assertIsDisplayed()
            onNodeWithTag("addRackEmptyStateButton").performClick()

            onNodeWithText("Add Rack").assertIsDisplayed()
        }
    }

    @Test
    fun addItem_withRacks_selectRack_selectSlotAndUseThisSlot_navigatesBackOnSave() {
        seedRack(id = "r1", name = "Garage Rack")
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithTag("rackListOverflowMenuButton").performClick()
            onNodeWithTag("rackListAddItemMenuItem").performClick()

            onNodeWithTag("selectRackAndSlotButton").performClick()
            onNodeWithText("Select Rack").assertIsDisplayed()

            onNodeWithTag("addItemRackCardButton").performClick()
            onNodeWithText("Garage Rack - select slot").assertIsDisplayed()

            onNodeWithTag("rackDetailImageOverlay").assertIsDisplayed()
            onNodeWithTag("rackDetailImageOverlay").performTouchInput { click() }
            onNodeWithTag("useThisSlotButton").assertIsDisplayed()
            onNodeWithTag("useThisSlotButton").performClick()

            onNodeWithText("Place: Rack & slot selected").assertIsDisplayed()
            onNodeWithTag("saveItemButton").performClick()

            onNodeWithText("Racks").assertIsDisplayed()
        }
    }

    @Test
    fun addItem_photoPickerDialog_cancel_dismissesDialog() {
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithTag("rackListOverflowMenuButton").performClick()
            onNodeWithTag("rackListAddItemMenuItem").performClick()

            onNodeWithTag("addItemPhotoPickerButton").performClick()
            onNodeWithTag("imagePickerDialogCancelButton").assertIsDisplayed()
            onNodeWithTag("imagePickerDialogCancelButton").performClick()

            onNodeWithTag("imagePickerDialogCancelButton").assertIsNotDisplayed()
        }
    }
}
