package org.deafsapps.storeit.data.datasource

import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.CollaborationActivity
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.RackMembership

internal interface RackMembershipDataSource {
    fun observeMembershipsByRack(rackId: String): Flow<Result<DomainError, List<RackMembership>>>

    fun observeMembershipsByAccount(accountId: String): Flow<Result<DomainError, List<RackMembership>>>

    suspend fun getMembership(
        rackId: String,
        accountId: String,
    ): Result<DomainError, RackMembership?>

    suspend fun saveMembership(
        membership: RackMembership,
    ): Result<DomainError, RackMembership>

    suspend fun deleteMembership(
        rackId: String,
        accountId: String,
    ): Result<DomainError, Long>

    suspend fun clearMemberships(): Result<DomainError, Long>
}

internal interface CollaborationActivityDataSource {
    fun observeActivityByRack(rackId: String): Flow<Result<DomainError, List<CollaborationActivity>>>

    suspend fun getActivityByRack(rackId: String): Result<DomainError, List<CollaborationActivity>>

    suspend fun saveActivity(
        activity: CollaborationActivity,
    ): Result<DomainError, CollaborationActivity>

    suspend fun deleteActivity(activityId: String): Result<DomainError, Long>

    suspend fun clearActivityByRack(rackId: String): Result<DomainError, Long>
}
