package org.deafsapps.storeit.domain.model

/**
 * Describes a collaborator's role and lifecycle state on a shared rack.
 */
interface RackMembership {
    val id: String
    val rackId: String
    val accountId: String
    val role: RackRole
    val status: RackMembershipStatus
    val invitedByAccountId: String
    val invitedAt: Long
    val updatedAt: Long
}

internal data class RackMembershipModel(
    override val id: String,
    override val rackId: String,
    override val accountId: String,
    override val role: RackRole,
    override val status: RackMembershipStatus,
    override val invitedByAccountId: String,
    override val invitedAt: Long,
    override val updatedAt: Long,
) : RackMembership

fun RackMembership(
    id: String,
    rackId: String,
    accountId: String,
    role: RackRole,
    status: RackMembershipStatus,
    invitedByAccountId: String,
    invitedAt: Long,
    updatedAt: Long,
): RackMembership = RackMembershipModel(
    id = id,
    rackId = rackId,
    accountId = accountId,
    role = role,
    status = status,
    invitedByAccountId = invitedByAccountId,
    invitedAt = invitedAt,
    updatedAt = updatedAt,
)

internal fun RackMembership.asModel(): RackMembershipModel = when (this) {
    is RackMembershipModel -> this
    else -> RackMembershipModel(
        id = id,
        rackId = rackId,
        accountId = accountId,
        role = role,
        status = status,
        invitedByAccountId = invitedByAccountId,
        invitedAt = invitedAt,
        updatedAt = updatedAt,
    )
}

enum class RackRole {
    OWNER,
    EDITOR,
    COMMENTER,
    SUGGESTER,
}

enum class RackMembershipStatus {
    INVITATION_PENDING,
    ACTIVE,
    REVOKED,
    WITHDRAWN,
}

/**
 * Captures attributed collaboration history associated with a rack.
 */
interface CollaborationActivity {
    val id: String
    val rackId: String
    val actorAccountId: String
    val actorRole: RackRole
    val activityType: CollaborationActivityType
    val targetEntityType: CollaborationTargetEntityType?
    val targetEntityId: String?
    val message: String?
    val createdAt: Long
    val supersededAt: Long?
}

internal data class CollaborationActivityModel(
    override val id: String,
    override val rackId: String,
    override val actorAccountId: String,
    override val actorRole: RackRole,
    override val activityType: CollaborationActivityType,
    override val targetEntityType: CollaborationTargetEntityType? = null,
    override val targetEntityId: String? = null,
    override val message: String? = null,
    override val createdAt: Long,
    override val supersededAt: Long? = null,
) : CollaborationActivity

fun CollaborationActivity(
    id: String,
    rackId: String,
    actorAccountId: String,
    actorRole: RackRole,
    activityType: CollaborationActivityType,
    targetEntityType: CollaborationTargetEntityType? = null,
    targetEntityId: String? = null,
    message: String? = null,
    createdAt: Long,
    supersededAt: Long? = null,
): CollaborationActivity = CollaborationActivityModel(
    id = id,
    rackId = rackId,
    actorAccountId = actorAccountId,
    actorRole = actorRole,
    activityType = activityType,
    targetEntityType = targetEntityType,
    targetEntityId = targetEntityId,
    message = message,
    createdAt = createdAt,
    supersededAt = supersededAt,
)

internal fun CollaborationActivity.asModel(): CollaborationActivityModel = when (this) {
    is CollaborationActivityModel -> this
    else -> CollaborationActivityModel(
        id = id,
        rackId = rackId,
        actorAccountId = actorAccountId,
        actorRole = actorRole,
        activityType = activityType,
        targetEntityType = targetEntityType,
        targetEntityId = targetEntityId,
        message = message,
        createdAt = createdAt,
        supersededAt = supersededAt,
    )
}

enum class CollaborationActivityType {
    COMMENT,
    SUGGESTION,
    DIRECT_EDIT,
}

enum class CollaborationTargetEntityType {
    RACK,
    SHELF_SLOT,
    ITEM,
    OTHER,
}
