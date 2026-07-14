package org.deafsapps.storeit.di

import org.deafsapps.storeit.data.database.StoreItDatabaseProvider
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(platformModule())
    }.also { koinApp ->
        // Eagerly create the local DB to validate driver wiring at app startup.
        koinApp.koin.getOrNull<StoreItDatabaseProvider>()?.database
    }

// called by iOS
fun initKoinIos() = initKoin {}
