package org.deafsapps.storeit.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.deafsapps.storeit.androidapp.presentation.rack.ui.AddRackScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackDetailScreen
import org.deafsapps.storeit.androidapp.presentation.rack.ui.RackListScreen
import org.deafsapps.storeit.presentation.rack.viewmodel.AndroidAddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.AndroidRackDetailViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.AndroidRackListViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

sealed interface NavScreen {
    data object RackList : NavScreen
    data object AddRack : NavScreen
    data class RackDetail(val rackId: String) : NavScreen
}

class MainActivity : ComponentActivity() {

    private val androidRackListViewModel: AndroidRackListViewModel by viewModel()
    private val androidAddRackViewModel: AndroidAddRackViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<NavScreen>(NavScreen.RackList) }
                    when (val screen = currentScreen) {
                        is NavScreen.RackList -> RackListScreen(
                            viewModel = androidRackListViewModel.rackListViewModel,
                            onNavigateToAddRack = { currentScreen = NavScreen.AddRack },
                            onNavigateToRackDetail = { id -> currentScreen = NavScreen.RackDetail(id) },
                        )
                        is NavScreen.AddRack -> AddRackScreen(
                            viewModel = androidAddRackViewModel.addRackViewModel,
                            onNavigateBack = { currentScreen = NavScreen.RackList },
                        )
                        is NavScreen.RackDetail -> {
                            val androidRackDetailViewModel: AndroidRackDetailViewModel by viewModel(
                                parameters = { parametersOf(screen.rackId) },
                            )
                            RackDetailScreen(
                                viewModel = androidRackDetailViewModel.rackDetailViewModel,
                                onNavigateBack = { currentScreen = NavScreen.RackList },
                            )
                        }
                    }
                }
            }
        }
    }
}
