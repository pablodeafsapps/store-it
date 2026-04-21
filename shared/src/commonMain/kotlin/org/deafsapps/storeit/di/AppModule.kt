package org.deafsapps.storeit.di

import org.koin.core.annotation.Module

@Module(
    includes = [
        AccountAuthModule::class,
        AccountPhotoBackupModule::class,
        AccountSyncModule::class,
    ],
)
class AppModule

@Module
internal class AccountAuthModule

@Module
internal class AccountPhotoBackupModule

@Module
internal class AccountSyncModule
