package org.deafsapps.storeit.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [AppModule::class])
@ComponentScan("org.deafsapps.storeit")
class IosModule
