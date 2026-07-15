package org.deafsapps.storeit.di

import org.koin.core.annotation.Module

@Module(
    includes = [
        AccountAuthModule::class,
        AccountPhotoBackupModule::class,
        AccountSyncModule::class,
        RackAccessModule::class,
        RackMembershipModule::class,
        RackPermissionPolicyModule::class,
        RackCollaborationActivityModule::class,
    ],
)
class AppModule

@Module
internal class AccountAuthModule

@Module
internal class AccountPhotoBackupModule

@Module
internal class AccountSyncModule

@Module
internal class RackAccessModule

@Module
internal class RackMembershipModule

@Module
internal class RackPermissionPolicyModule

@Module
internal class RackCollaborationActivityModule
