package org.deafsapps.storeit.data.datasource

import kotlinx.serialization.Serializable
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot

internal interface AuthRemoteDataSource {
    suspend fun signUp(credentials: EmailPasswordCredentials): Result<DomainError, AuthenticatedRemoteAccount>

    suspend fun signIn(credentials: EmailPasswordCredentials): Result<DomainError, AuthenticatedRemoteAccount>

    suspend fun restoreSession(session: StoredSessionCredentials): Result<DomainError, AuthenticatedRemoteAccount>

    suspend fun signOut(accountId: String): Result<DomainError, Unit>
}

internal interface SessionCredentialDataSource {
    suspend fun save(session: StoredSessionCredentials): Result<DomainError, Unit>

    suspend fun restore(): Result<DomainError, StoredSessionCredentials?>

    suspend fun clear(): Result<DomainError, Unit>
}

internal interface AccountRemoteDataSource {
    suspend fun fetchSnapshot(accountId: String): Result<DomainError, RemoteAccountSnapshot>

    suspend fun applyMutations(
        accountId: String,
        mutations: List<RemoteDatasetMutation>,
    ): Result<DomainError, RemoteSyncCheckpoint>

    suspend fun uploadPhoto(asset: RemotePhotoAsset): Result<DomainError, RemotePhotoReference>

    suspend fun deletePhoto(photoId: String): Result<DomainError, Unit>
}

internal data class EmailPasswordCredentials(
    val email: String,
    val password: String,
)

internal data class AuthenticatedRemoteAccount(
    val accountId: String,
    val email: String,
    val session: StoredSessionCredentials,
    val createdAt: Long? = null,
    val lastAuthenticatedAt: Long? = null,
)

@Serializable
internal data class StoredSessionCredentials(
    val accountId: String,
    val email: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val lastAuthenticatedAt: Long? = null,
)

internal data class RemoteAccountSnapshot(
    val accountId: String,
    val syncCheckpoint: RemoteSyncCheckpoint,
    val racks: List<Rack>,
    val slots: List<ShelfSlot>,
    val items: List<Item>,
    val photos: List<RemotePhotoReference>,
)

internal data class RemoteSyncCheckpoint(
    val value: String,
    val updatedAt: Long? = null,
)

internal sealed interface RemoteDatasetMutation {
    val entityId: String

    data class UpsertRack(val rack: Rack) : RemoteDatasetMutation {
        override val entityId: String = rack.id
    }

    data class UpsertSlot(val slot: ShelfSlot) : RemoteDatasetMutation {
        override val entityId: String = slot.id
    }

    data class UpsertItem(val item: Item) : RemoteDatasetMutation {
        override val entityId: String = item.id
    }

    data class DeleteRack(override val entityId: String) : RemoteDatasetMutation

    data class DeleteSlot(override val entityId: String) : RemoteDatasetMutation

    data class DeleteItem(override val entityId: String) : RemoteDatasetMutation
}

internal data class RemotePhotoAsset(
    val photoId: String,
    val ownerId: String,
    val localUri: String,
    val checksum: String? = null,
)

internal data class RemotePhotoReference(
    val photoId: String,
    val remoteUrl: String,
    val checksum: String? = null,
)
