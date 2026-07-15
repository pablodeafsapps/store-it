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

internal fun RackMembership(
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

interface RackPermissionPolicy {
    val effectiveRole: RackRole
    val canManageAccess: Boolean
    val canEditContent: Boolean
    val canComment: Boolean
    val canSuggest: Boolean
    val canView: Boolean
}

internal data class RackPermissionPolicyModel(
    override val effectiveRole: RackRole,
    override val canManageAccess: Boolean,
    override val canEditContent: Boolean,
    override val canComment: Boolean,
    override val canSuggest: Boolean,
    override val canView: Boolean,
) : RackPermissionPolicy

internal fun RackPermissionPolicy(
    effectiveRole: RackRole,
    canManageAccess: Boolean,
    canEditContent: Boolean,
    canComment: Boolean,
    canSuggest: Boolean,
    canView: Boolean,
): RackPermissionPolicy = RackPermissionPolicyModel(
    effectiveRole = effectiveRole,
    canManageAccess = canManageAccess,
    canEditContent = canEditContent,
    canComment = canComment,
    canSuggest = canSuggest,
    canView = canView,
)

internal fun RackPermissionPolicy.asModel(): RackPermissionPolicyModel = when (this) {
    is RackPermissionPolicyModel -> this
    else -> RackPermissionPolicyModel(
        effectiveRole = effectiveRole,
        canManageAccess = canManageAccess,
        canEditContent = canEditContent,
        canComment = canComment,
        canSuggest = canSuggest,
        canView = canView,
    )
}

interface AccessibleRackSummary {
    val rackId: String
    val name: String
    val location: String
    val photoUri: String?
    val accessKind: RackAccessKind
    val effectiveRole: RackRole
    val ownerAccountId: String
    val ownerDisplayName: String?
    val updatedAt: Long?
}

internal data class AccessibleRackSummaryModel(
    override val rackId: String,
    override val name: String,
    override val location: String = "",
    override val photoUri: String? = null,
    override val accessKind: RackAccessKind,
    override val effectiveRole: RackRole,
    override val ownerAccountId: String,
    override val ownerDisplayName: String? = null,
    override val updatedAt: Long? = null,
) : AccessibleRackSummary

internal fun AccessibleRackSummary(
    rackId: String,
    name: String,
    location: String = "",
    photoUri: String? = null,
    accessKind: RackAccessKind,
    effectiveRole: RackRole,
    ownerAccountId: String,
    ownerDisplayName: String? = null,
    updatedAt: Long? = null,
): AccessibleRackSummary = AccessibleRackSummaryModel(
    rackId = rackId,
    name = name,
    location = location,
    photoUri = photoUri,
    accessKind = accessKind,
    effectiveRole = effectiveRole,
    ownerAccountId = ownerAccountId,
    ownerDisplayName = ownerDisplayName,
    updatedAt = updatedAt,
)

internal fun AccessibleRackSummary.asModel(): AccessibleRackSummaryModel = when (this) {
    is AccessibleRackSummaryModel -> this
    else -> AccessibleRackSummaryModel(
        rackId = rackId,
        name = name,
        location = location,
        photoUri = photoUri,
        accessKind = accessKind,
        effectiveRole = effectiveRole,
        ownerAccountId = ownerAccountId,
        ownerDisplayName = ownerDisplayName,
        updatedAt = updatedAt,
    )
}

enum class RackAccessKind {
    OWNED,
    INVITED,
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

internal fun CollaborationActivity(
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

interface RackAccessRemoval {
    val rackId: String
    val reason: RackAccessRemovalReason
    val occurredAt: Long
    val displayMessage: String
}

internal data class RackAccessRemovalModel(
    override val rackId: String,
    override val reason: RackAccessRemovalReason,
    override val occurredAt: Long,
    override val displayMessage: String,
) : RackAccessRemoval

internal fun RackAccessRemoval(
    rackId: String,
    reason: RackAccessRemovalReason,
    occurredAt: Long,
    displayMessage: String,
): RackAccessRemoval = RackAccessRemovalModel(
    rackId = rackId,
    reason = reason,
    occurredAt = occurredAt,
    displayMessage = displayMessage,
)

internal fun RackAccessRemoval.asModel(): RackAccessRemovalModel = when (this) {
    is RackAccessRemovalModel -> this
    else -> RackAccessRemovalModel(
        rackId = rackId,
        reason = reason,
        occurredAt = occurredAt,
        displayMessage = displayMessage,
    )
}

enum class RackAccessRemovalReason {
    REVOKED,
    WITHDRAWN,
    DELETED,
    UNAVAILABLE,
}
