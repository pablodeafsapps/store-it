package org.deafsapps.storeit.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import org.deafsapps.storeit.presentation.rack.viewmodel.AddRackViewModel
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.deafsapps.storeit.presentation.search.viewmodel.SearchViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val rackListViewModel: RackListViewModel by viewModel()
    private val addRackViewModel: AddRackViewModel by viewModel()
    private val searchViewModel: SearchViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val backStack = rememberNavBackStack(NavScreen.RackList)
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StoreItNavDisplay(
                        backStack = backStack,
                        rackListViewModel = { rackListViewModel },
                        addRackViewModel = { addRackViewModel },
                        searchViewModel = { searchViewModel },
                        onRootBack = { finish() },
                    )
                }
            }
        }
    }
}
