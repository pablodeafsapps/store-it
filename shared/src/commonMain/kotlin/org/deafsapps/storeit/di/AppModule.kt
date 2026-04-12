package org.deafsapps.storeit.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(
    includes = [
        AccountAuthModule::class,
        AccountPhotoBackupModule::class,
        AccountSyncModule::class,
    ],
)
@ComponentScan("org.deafsapps.storeit")
class AppModule

@Module
internal class AccountAuthModule

@Module
internal class AccountPhotoBackupModule

@Module
internal class AccountSyncModule
