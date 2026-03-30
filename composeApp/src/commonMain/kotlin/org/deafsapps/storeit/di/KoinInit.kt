package org.deafsapps.storeit.di

import org.deafsapps.storeit.data.local.db.StoreItDatabaseProvider
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(AppModule().module)
    }.also { koinApp ->
        // Eagerly create the local DB to validate driver wiring at app startup.
        koinApp.koin.get<StoreItDatabaseProvider>().database
    }

// called by iOS
fun initKoinIos() = initKoin {}
