package org.deafsapps.storeit.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.deafsapps.storeit.App
import org.deafsapps.storeit.androidapp.presentation.rack.ui.AddRackScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackDetailPlaceholderScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackListScreen
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

sealed interface NavScreen {
    data object RackList : NavScreen
    data object AddRack : NavScreen
    data class RackDetail(val rackId: String) : NavScreen
}

class MainActivity : ComponentActivity() {

    private val rackListViewModel: RackListViewModel by viewModel()
    private val addRackViewModel: AddRackViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<NavScreen>(NavScreen.RackList) }
                    when (val screen = currentScreen) {
                        is NavScreen.RackList -> RackListScreen(
                            viewModel = rackListViewModel,
                            onNavigateToAddRack = { currentScreen = NavScreen.AddRack },
                            onNavigateToRackDetail = { id -> currentScreen = NavScreen.RackDetail(id) },
                        )
                        is NavScreen.AddRack -> AddRackScreen(
                            viewModel = addRackViewModel,
                            onNavigateBack = { currentScreen = NavScreen.RackList },
                        )
                        is NavScreen.RackDetail -> RackDetailPlaceholderScreen(
                            rackId = screen.rackId,
                            onNavigateBack = { currentScreen = NavScreen.RackList },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
