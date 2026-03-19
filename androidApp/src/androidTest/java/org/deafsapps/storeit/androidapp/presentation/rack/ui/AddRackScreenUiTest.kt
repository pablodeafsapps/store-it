package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.deafsapps.storeit.androidapp.NavScreen
import org.deafsapps.storeit.androidapp.StoreItComposeUiTestBase
import org.junit.jupiter.api.Test

internal class AddRackScreenUiTest : StoreItComposeUiTestBase() {

    @Test
    fun addRack_cancel_returnsToRackList() {
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithTag("rackListAddRackFab").performClick()
            onNodeWithTag("addRackCancelButton").performClick()

            onNodeWithText("Racks").assertIsDisplayed()
        }
    }

    @Test
    fun addRack_save_nameRequired_showsError() {
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithTag("rackListAddRackFab").performClick()
            onNodeWithTag("saveRackButton").performClick()

            onNodeWithText("Name is required").assertIsDisplayed()
        }
    }

    @Test
    fun addRack_save_success_createsRack() {
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithTag("rackListAddRackFab").performClick()

            onNodeWithTag("addRackNameField").performTextInput("Test Rack")
            onNodeWithTag("saveRackButton").performClick()

            onNodeWithText("Test Rack").assertIsDisplayed()
        }
    }
}
