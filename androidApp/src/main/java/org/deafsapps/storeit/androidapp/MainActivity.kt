package org.deafsapps.storeit.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import org.deafsapps.storeit.androidapp.design.StoreItTheme
import org.deafsapps.storeit.presentation.rack.viewmodel.RackListViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    private val rackListViewModel: RackListViewModel by viewModel()
    private val preferences by lazy { getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE) }
    private var isDarkModeEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        isDarkModeEnabled = preferences.getBoolean(KEY_DARK_MODE_ENABLED, false)
        setContent {
            val backStack = rememberNavBackStack(NavScreen.RackList)
            StoreItTheme(darkTheme = isDarkModeEnabled) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StoreItNavDisplay(
                        backStack = backStack,
                        onRootBack = { finish() },
                        rackListViewModel = { rackListViewModel },
                        isDarkModeEnabled = isDarkModeEnabled,
                        onThemeModeToggle = ::toggleThemeMode,
                    )
                }
            }
        }
    }

    private fun toggleThemeMode() {
        isDarkModeEnabled = !isDarkModeEnabled
        preferences.edit {
            putBoolean(KEY_DARK_MODE_ENABLED, isDarkModeEnabled)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "store_it_preferences"
        const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"
    }
}
