package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.deafsapps.storeit.androidapp.NavScreen
import org.deafsapps.storeit.androidapp.StoreItComposeUiTestBase
import org.junit.jupiter.api.Test

internal class RackListScreenUiTest : StoreItComposeUiTestBase() {

    @Test
    fun rackList_addRackFab_navigatesToAddRack() {
        composeUiTest(initialScreen = NavScreen.RackList) {
            onNodeWithTag("rackListAddRackFab").assertIsDisplayed()
            onNodeWithTag("rackListAddRackFab").performClick()

            onNodeWithTag("addRackCancelButton").assertIsDisplayed()
            onNodeWithText("Add Rack").assertIsDisplayed()
        }
    }
}
