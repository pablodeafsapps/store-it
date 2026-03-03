package org.deafsapps.storeit.androidapp

import android.app.Application
import org.deafsapps.storeit.di.AppModule
import org.deafsapps.storeit.di.initKoin
import org.deafsapps.storeit.di.rackDetailModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication

@KoinApplication(modules = [AppModule::class])
class StoreItApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@StoreItApplication)
            modules(rackDetailModule)
        }
    }
}
