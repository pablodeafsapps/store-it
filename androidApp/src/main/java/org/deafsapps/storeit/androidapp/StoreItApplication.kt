package org.deafsapps.storeit.androidapp

import android.app.Application
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.FirebaseApp
import org.deafsapps.storeit.androidapp.di.AndroidModule
import org.deafsapps.storeit.di.AppModule
import org.deafsapps.storeit.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.ksp.generated.module

private const val MISSING_FIREBASE_CONFIGURATION_MESSAGE =
    "Firebase is not configured for Android. Add a valid google-services.json " +
            "for the androidApp module before using account authentication."

@KoinApplication(modules = [AppModule::class])
class StoreItApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installSecurityProvider()
        initializeFirebaseApp()

        initKoin {
            androidLogger()
            modules(AndroidModule().module)
            androidContext(this@StoreItApplication)
        }
    }

    private fun initializeFirebaseApp() {
        val firebaseApp = FirebaseApp.initializeApp(this)
        checkNotNull(firebaseApp) { MISSING_FIREBASE_CONFIGURATION_MESSAGE }
    }

    private fun installSecurityProvider() {
        ProviderInstaller.installIfNeededAsync(this, object : ProviderInstaller.ProviderInstallListener {
            override fun onProviderInstalled() {}
            override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: android.content.Intent?) {}
        })
    }
}
