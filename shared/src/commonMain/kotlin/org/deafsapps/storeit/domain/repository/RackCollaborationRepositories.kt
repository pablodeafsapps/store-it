package org.deafsapps.storeit.domain.repository

import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.AccessibleRackSummary
import org.deafsapps.storeit.domain.model.CollaborationActivity
import org.deafsapps.storeit.domain.model.CollaborationTargetEntityType
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.RackMembership
import org.deafsapps.storeit.domain.model.RackPermissionPolicy
import org.deafsapps.storeit.domain.model.RackRole

/**
 * Defines access projections for owned and invited racks.
 */
internal interface RackAccessRepository {
    /**
     * Observes the racks the given account can currently access.
     */
    fun observeAccessibleRacks(accountId: String): Flow<Result<DomainError, List<AccessibleRackSummary>>>

    /**
     * Returns the access projection for one rack and account, if access exists.
     */
    suspend fun getAccessibleRack(
        rackId: String,
        accountId: String,
    ): Result<DomainError, AccessibleRackSummary?>
}

/**
 * Defines collaborator membership lifecycle operations for a shared rack.
 */
internal interface RackMembershipRepository {
    /**
     * Observes collaborator memberships for a single rack.
     */
    fun observeMembershipsByRack(rackId: String): Flow<Result<DomainError, List<RackMembership>>>

    /**
     * Observes collaborator memberships granted to a single account.
     */
    fun observeMembershipsByAccount(accountId: String): Flow<Result<DomainError, List<RackMembership>>>

    /**
     * Returns the membership for one rack and account, if one exists.
     */
    suspend fun getMembership(
        rackId: String,
        accountId: String,
    ): Result<DomainError, RackMembership?>

    /**
     * Creates or updates a rack membership.
     */
    suspend fun saveMembership(membership: RackMembership): Result<DomainError, RackMembership>

    /**
     * Activates an existing rack membership.
     */
    suspend fun activateMembership(
        rackId: String,
        accountId: String,
        updatedAt: Long,
    ): Result<DomainError, RackMembership>

    /**
     * Changes the role of an existing rack membership.
     */
    suspend fun changeMembershipRole(
        rackId: String,
        accountId: String,
        role: RackRole,
        updatedAt: Long,
    ): Result<DomainError, RackMembership>

    /**
     * Revokes an existing rack membership.
     */
    suspend fun revokeMembership(
        rackId: String,
        accountId: String,
        updatedAt: Long,
    ): Result<DomainError, RackMembership?>

    /**
     * Withdraws a pending rack membership.
     */
    suspend fun withdrawMembership(
        rackId: String,
        accountId: String,
        updatedAt: Long,
    ): Result<DomainError, RackMembership?>

    /**
     * Removes all stored rack memberships.
     */
    suspend fun clearMemberships(): Result<DomainError, Long>
}

/**
 * Defines central role-to-permission resolution for shared rack actions.
 */
internal interface RackPermissionPolicyRepository {
    /**
     * Resolves the effective permission policy for one rack and account.
     */
    suspend fun getPermissionPolicy(
        rackId: String,
        accountId: String,
    ): Result<DomainError, RackPermissionPolicy>
}

/**
 * Defines attributed collaboration actions and activity history for shared racks.
 */
internal interface RackCollaborationRepository {
    /**
     * Observes collaboration activity for a rack in presentation order.
     */
    fun observeActivity(rackId: String): Flow<Result<DomainError, List<CollaborationActivity>>>

    /**
     * Returns the current collaboration activity for a rack.
     */
    suspend fun getActivity(rackId: String): Result<DomainError, List<CollaborationActivity>>

    /**
     * Adds a comment activity to a rack after permission checks pass.
     */
    suspend fun addComment(
        rackId: String,
        actorAccountId: String,
        actorRole: RackRole,
        message: String,
        targetEntityType: CollaborationTargetEntityType? = null,
        targetEntityId: String? = null,
        createdAt: Long,
    ): Result<DomainError, CollaborationActivity>

    /**
     * Adds a suggestion activity to a rack after permission checks pass.
     */
    suspend fun addSuggestion(
        rackId: String,
        actorAccountId: String,
        actorRole: RackRole,
        message: String,
        targetEntityType: CollaborationTargetEntityType? = null,
        targetEntityId: String? = null,
        createdAt: Long,
        supersededAt: Long? = null,
    ): Result<DomainError, CollaborationActivity>

    /**
     * Records an attributed direct-edit activity after permission checks pass.
     */
    suspend fun recordDirectEdit(
        rackId: String,
        actorAccountId: String,
        actorRole: RackRole,
        targetEntityType: CollaborationTargetEntityType? = null,
        targetEntityId: String? = null,
        message: String? = null,
        createdAt: Long,
    ): Result<DomainError, CollaborationActivity>
}
