package org.deafsapps.storeit.androidapp

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

internal class FirebaseInitializationTest {

    @Test
    fun firebase_isInitializedAtApplicationStartup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

        assertTrue(
            actual = FirebaseApp.getApps(context).isNotEmpty(),
            message = "FirebaseApp should be initialized before account authentication is used.",
        )
    }
}
