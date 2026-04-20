package org.deafsapps.storeit.data.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.storage.FirebaseStorageException
import dev.gitlive.firebase.storage.storage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.AccountRemoteDataSource
import org.deafsapps.storeit.data.datasource.RemoteAccountSnapshot
import org.deafsapps.storeit.data.datasource.RemoteDatasetMutation
import org.deafsapps.storeit.data.datasource.RemotePhotoAsset
import org.deafsapps.storeit.data.datasource.RemotePhotoReference
import org.deafsapps.storeit.data.datasource.RemoteSyncCheckpoint
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.Item
import org.deafsapps.storeit.domain.model.Rack
import org.deafsapps.storeit.domain.model.ShelfSlot
import org.deafsapps.storeit.domain.model.SlotPosition
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.toUnknownDomainError
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single(binds = [AccountRemoteDataSource::class])
internal class FirebaseRemoteAccountDataSource : AccountRemoteDataSource {

    override suspend fun fetchSnapshot(accountId: String): Result<DomainError, RemoteAccountSnapshot> {
        if (accountId.isBlank()) {
            return DomainError.ValidationError(
                field = "accountId",
                reason = "Account ID cannot be blank",
            ).err()
        }

        return runFirestoreOperation(
            message = "Failed to fetch remote snapshot for account '$accountId'",
        ) {
            val accountMetadata = accountDocument(accountId = accountId)
                .get().takeIf { snapshot -> snapshot.exists }?.data<RemoteAccountDocument>()

            val rackDocuments = racksCollection(accountId = accountId).get().documents
            val slotDocuments = slotsCollection(accountId = accountId).get().documents
            val itemDocuments = itemsCollection(accountId = accountId).get().documents
            val photoDocuments = photosCollection(accountId = accountId).get().documents

            RemoteAccountSnapshot(
                accountId = accountId,
                syncCheckpoint = RemoteSyncCheckpoint(
                    value = accountMetadata?.checkpoint ?: initialCheckpoint(accountId = accountId),
                    updatedAt = accountMetadata?.updatedAt,
                ),
                racks = rackDocuments.map { doc -> doc.data<RemoteRackDocument>().asRack() },
                slots = slotDocuments.map { doc -> doc.data<RemoteSlotDocument>().asShelfSlot() },
                items = itemDocuments.map { doc -> doc.data<RemoteItemDocument>().asItem() },
                photos = photoDocuments.map { doc -> doc.data<RemotePhotoDocument>().asRemotePhotoReference() },
            ).ok()
        }
    }

    override suspend fun applyMutations(
        accountId: String,
        mutations: List<RemoteDatasetMutation>,
    ): Result<DomainError, RemoteSyncCheckpoint> {
        if (accountId.isBlank()) {
            return DomainError.ValidationError(
                field = "accountId",
                reason = "Account ID cannot be blank",
            ).err()
        }

        return runFirestoreOperation(
            message = "Failed to apply remote dataset mutations for account '$accountId'",
        ) {
            val checkpoint = createCheckpoint()
            val batch = Firebase.firestore.batch()

            mutations.forEach { mutation ->
                when (mutation) {
                    is RemoteDatasetMutation.UpsertRack -> {
                        batch.set(
                            documentRef = racksCollection(accountId = accountId).document(mutation.rack.id),
                            data = mutation.rack.asRemoteDocument(),
                        )
                        mutation.rack.toRemotePhotoDocumentOrNull()?.let { photoDocument ->
                            batch.set(
                                documentRef = photosCollection(accountId = accountId).document(photoDocument.photoId),
                                data = photoDocument,
                            )
                        }
                    }

                    is RemoteDatasetMutation.UpsertSlot -> {
                        batch.set(
                            documentRef = slotsCollection(accountId = accountId).document(mutation.slot.id),
                            data = mutation.slot.asRemoteDocument(),
                        )
                    }

                    is RemoteDatasetMutation.UpsertItem -> {
                        batch.set(
                            documentRef = itemsCollection(accountId = accountId).document(mutation.item.id),
                            data = mutation.item.asRemoteDocument(),
                        )
                        mutation.item.toRemotePhotoDocumentOrNull()?.let { photoDocument ->
                            batch.set(
                                documentRef = photosCollection(accountId = accountId).document(photoDocument.photoId),
                                data = photoDocument,
                            )
                        }
                    }

                    is RemoteDatasetMutation.DeleteRack -> {
                        batch.delete(documentRef = racksCollection(accountId = accountId).document(mutation.entityId))
                    }

                    is RemoteDatasetMutation.DeleteSlot -> {
                        batch.delete(documentRef = slotsCollection(accountId = accountId).document(mutation.entityId))
                    }

                    is RemoteDatasetMutation.DeleteItem -> {
                        batch.delete(documentRef = itemsCollection(accountId = accountId).document(mutation.entityId))
                    }
                }
            }

            batch.set(
                documentRef = accountDocument(accountId = accountId),
                data = RemoteAccountDocument(
                    accountId = accountId,
                    checkpoint = checkpoint.value,
                    updatedAt = checkpoint.updatedAt,
                ),
                merge = true,
            )
            batch.commit()

            checkpoint.ok()
        }
    }

    override suspend fun uploadPhoto(asset: RemotePhotoAsset): Result<DomainError, RemotePhotoReference> {
        if (asset.photoId.isBlank()) {
            return DomainError.ValidationError(
                field = "photoId",
                reason = "Photo ID cannot be blank",
            ).err()
        }
        if (asset.ownerId.isBlank()) {
            return DomainError.ValidationError(
                field = "ownerId",
                reason = "Owner ID cannot be blank",
            ).err()
        }
        if (asset.localUri.isBlank()) {
            return DomainError.ValidationError(
                field = "localUri",
                reason = "Local URI cannot be blank",
            ).err()
        }

        return runFirestoreOperation(
            message = "Failed to upload photo '${asset.photoId}' for owner '${asset.ownerId}'",
        ) {
            if (!asset.localUri.isRemoteUri()) {
                return DomainError.ValidationError(
                    field = "localUri",
                    reason = "Remote photo upload from local device URIs requires a platform bridge outside commonMain",
                ).err()
            }

            val remoteReference = RemotePhotoReference(
                photoId = asset.photoId,
                remoteUrl = asset.localUri,
                checksum = asset.checksum,
            )

            photosCollection(accountId = asset.ownerId)
                .document(asset.photoId)
                .set(
                    data = RemotePhotoDocument(
                        photoId = asset.photoId,
                        ownerType = SyncEntityType.Photo.name,
                        ownerId = asset.ownerId,
                        remoteUrl = asset.localUri,
                        checksum = asset.checksum,
                        storagePath = Firebase.storage.reference(getStoragePath(asset = asset)).path,
                    ),
                    merge = true,
                )

            remoteReference.ok()
        }
    }

    override suspend fun deletePhoto(photoId: String): Result<DomainError, Long> {
        if (photoId.isBlank()) {
            return DomainError.ValidationError(
                field = "photoId",
                reason = "Photo ID cannot be blank",
            ).err()
        }

        return runRemoteOperation(
            message = "Failed to delete remote photo '$photoId'",
        ) {
            val photoDocuments = Firebase.firestore
                .collectionGroup(COLLECTION_PHOTOS)
                .where { PHOTO_ID_FIELD equalTo photoId }
                .get()
                .documents

            photoDocuments.forEach { documentSnapshot ->
                val photoDocument = documentSnapshot.data<RemotePhotoDocument>()
                photoDocument.storagePath
                    ?.takeIf { storagePath -> storagePath.isNotBlank() }
                    ?.let { storagePath ->
                        Firebase.storage.reference(storagePath).delete()
                    }

                documentSnapshot.reference.delete()
            }

            photoDocuments.size.toLong().ok()
        }
    }

    private fun accountDocument(accountId: String): DocumentReference =
        Firebase.firestore.collection(COLLECTION_ACCOUNTS).document(accountId)

    private fun racksCollection(accountId: String) = accountDocument(accountId = accountId).collection(COLLECTION_RACKS)

    private fun slotsCollection(accountId: String) = accountDocument(accountId = accountId).collection(COLLECTION_SLOTS)

    private fun itemsCollection(accountId: String) = accountDocument(accountId = accountId).collection(COLLECTION_ITEMS)

    private fun photosCollection(accountId: String) = accountDocument(accountId = accountId).collection(COLLECTION_PHOTOS)
}

private inline fun <T> runFirestoreOperation(
    message: String,
    block: () -> Result<DomainError, T>,
): Result<DomainError, T> =
    try {
        block()
    } catch (exception: FirebaseFirestoreException) {
        exception.toUnknownDomainError(message = message).err()
    } catch (exception: SerializationException) {
        exception.toUnknownDomainError(message = message).err()
    }

private inline fun <T> runRemoteOperation(
    message: String,
    block: () -> Result<DomainError, T>,
): Result<DomainError, T> =
    try {
        block()
    } catch (exception: FirebaseFirestoreException) {
        exception.toUnknownDomainError(message = message).err()
    } catch (exception: FirebaseStorageException) {
        exception.toUnknownDomainError(message = message).err()
    } catch (exception: SerializationException) {
        exception.toUnknownDomainError(message = message).err()
    }

private fun createCheckpoint(now: Long = Clock.System.now().toEpochMilliseconds()): RemoteSyncCheckpoint =
    RemoteSyncCheckpoint(
        value = "checkpoint-$now",
        updatedAt = now,
    )

private fun initialCheckpoint(accountId: String): String = "initial-$accountId"

private fun getStoragePath(asset: RemotePhotoAsset): String = "accounts/${asset.ownerId}/photos/${asset.photoId}"

private fun String.isRemoteUri(): Boolean =
    startsWith(prefix = "gs://") ||
        startsWith(prefix = "http://") ||
        startsWith(prefix = "https://") ||
        startsWith(prefix = "remote://")

private fun Rack.toRemotePhotoDocumentOrNull(): RemotePhotoDocument? =
    photoUri
        ?.takeIf { uri -> uri.isRemoteUri() }
        ?.let { remoteUrl ->
            RemotePhotoDocument(
                photoId = extractPhotoId(remoteUrl = remoteUrl, fallbackId = id),
                ownerType = SyncEntityType.Rack.name,
                ownerId = id,
                remoteUrl = remoteUrl,
                checksum = null,
                storagePath = remoteUrl.takeUnless { uri -> uri.startsWith(prefix = "http") || uri.startsWith(prefix = "remote://") },
            )
        }

private fun Item.toRemotePhotoDocumentOrNull(): RemotePhotoDocument? =
    photoUri
        ?.takeIf { uri -> uri.isRemoteUri() }
        ?.let { remoteUrl ->
            RemotePhotoDocument(
                photoId = extractPhotoId(remoteUrl = remoteUrl, fallbackId = id),
                ownerType = SyncEntityType.Item.name,
                ownerId = id,
                remoteUrl = remoteUrl,
                checksum = null,
                storagePath = remoteUrl.takeUnless { uri -> uri.startsWith(prefix = "http") || uri.startsWith(prefix = "remote://") },
            )
        }

private fun extractPhotoId(remoteUrl: String, fallbackId: String): String =
    remoteUrl
        .substringBefore(delimiter = "?")
        .substringBefore(delimiter = "#")
        .substringAfterLast(delimiter = "/")
        .substringAfterLast(delimiter = ":")
        .ifBlank { fallbackId }

private fun Rack.asRemoteDocument(): RemoteRackDocument = RemoteRackDocument(
    id = id,
    name = name,
    description = description,
    location = location,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun RemoteRackDocument.asRack(): Rack = Rack(
    id = id,
    name = name,
    description = description,
    location = location,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ShelfSlot.asRemoteDocument(): RemoteSlotDocument = RemoteSlotDocument(
    id = id,
    rackId = rackId,
    position = position.asRemoteDocument(),
)

private fun RemoteSlotDocument.asShelfSlot(): ShelfSlot = ShelfSlot(
    id = id,
    rackId = rackId,
    position = position.asSlotPosition(),
)

private fun SlotPosition.asRemoteDocument(): RemoteSlotPositionDocument = RemoteSlotPositionDocument(
    x = x,
    y = y,
    xRel = xRel,
    yRel = yRel,
)

private fun RemoteSlotPositionDocument.asSlotPosition(): SlotPosition = SlotPosition(
    x = x,
    y = y,
    xRel = xRel,
    yRel = yRel,
)

private fun Item.asRemoteDocument(): RemoteItemDocument = RemoteItemDocument(
    id = id,
    rackId = rackId,
    slotId = slotId,
    name = name,
    description = description,
    photoUri = photoUri,
    quantity = quantity,
    owner = owner,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun RemoteItemDocument.asItem(): Item = Item(
    id = id,
    rackId = rackId,
    slotId = slotId,
    name = name,
    description = description,
    photoUri = photoUri,
    quantity = quantity,
    owner = owner,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun RemotePhotoDocument.asRemotePhotoReference(): RemotePhotoReference = RemotePhotoReference(
    photoId = photoId,
    remoteUrl = remoteUrl,
    checksum = checksum,
)

private const val COLLECTION_ACCOUNTS = "accounts"
private const val COLLECTION_RACKS = "racks"
private const val COLLECTION_SLOTS = "slots"
private const val COLLECTION_ITEMS = "items"
private const val COLLECTION_PHOTOS = "photos"
private const val PHOTO_ID_FIELD = "photoId"

@Serializable
private data class RemoteAccountDocument(
    val accountId: String,
    val checkpoint: String,
    val updatedAt: Long? = null,
)

@Serializable
private data class RemoteRackDocument(
    val id: String,
    val name: String,
    val description: String = "",
    val location: String = "",
    val photoUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long? = null,
)

@Serializable
private data class RemoteSlotDocument(
    val id: String,
    val rackId: String,
    val position: RemoteSlotPositionDocument,
)

@Serializable
private data class RemoteSlotPositionDocument(
    val x: Float,
    val y: Float,
    val xRel: Float,
    val yRel: Float,
)

@Serializable
private data class RemoteItemDocument(
    val id: String,
    val rackId: String,
    val slotId: String,
    val name: String,
    val description: String = "",
    val photoUri: String? = null,
    val quantity: Int? = null,
    val owner: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long? = null,
)

@Serializable
private data class RemotePhotoDocument(
    val photoId: String,
    val ownerType: String,
    val ownerId: String,
    val remoteUrl: String,
    val checksum: String? = null,
    val storagePath: String? = null,
)
