package org.deafsapps.storeit.androidapp

import android.app.Application
import org.deafsapps.storeit.androidapp.di.AndroidModule
import org.deafsapps.storeit.di.AppModule
import org.deafsapps.storeit.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.ksp.generated.module

@KoinApplication(modules = [AppModule::class])
class StoreItApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            modules(AndroidModule().module)
            androidContext(this@StoreItApplication)
        }
    }
}
