package org.deafsapps.storeit.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.ksp.generated.module

@Module(includes = [AppModule::class])
@ComponentScan("org.deafsapps.storeit")
class AndroidModule

internal actual fun platformModule(): org.koin.core.module.Module = AndroidModule().module
