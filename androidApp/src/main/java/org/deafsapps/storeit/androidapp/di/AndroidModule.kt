package org.deafsapps.storeit.androidapp.di

import org.deafsapps.storeit.di.AppModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [AppModule::class])
@ComponentScan("org.deafsapps.storeit")
class AndroidModule
