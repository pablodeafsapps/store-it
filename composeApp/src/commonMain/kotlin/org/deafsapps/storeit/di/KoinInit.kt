package org.deafsapps.storeit.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(AppModule().module)
    }

// called by iOS
fun initKoinIos() = initKoin {}
