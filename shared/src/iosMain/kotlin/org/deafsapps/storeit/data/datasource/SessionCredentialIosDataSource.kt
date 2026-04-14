package org.deafsapps.storeit.data.datasource

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.serialization.json.Json
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.toUnknownDomainError
import org.koin.core.annotation.Single
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDataCreate
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Security.SecCopyErrorMessageString
import platform.Security.SecItemAdd
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
@Single(binds = [SessionCredentialDataSource::class])
internal class SessionCredentialIosDataSource : SessionCredentialDataSource {
    override suspend fun save(session: StoredSessionCredentials): Result<DomainError, Unit> = try {
        when (val clearResult = deleteStoredValueIfPresent()) {
            is KeychainOperationResult.Error -> clearResult.error.err()
            KeychainOperationResult.Success -> {
                val payloadData = SESSION_JSON.encodeToString(value = session).toNSData()

                val status = SecItemAdd(
                    (baseQuery() + mapOf(
                        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                        kSecValueData to payloadData,
                    )) as CFDictionaryRef,
                    null,
                )

                if (status == errSecSuccess) {
                    USER_DEFAULTS.setObject(
                        value = SESSION_JSON.encodeToString(value = session),
                        forKey = USER_DEFAULTS_SESSION_KEY,
                    )
                    Unit.ok()
                } else {
                    keychainFailure(
                        operation = "save session credentials",
                        status = status,
                    ).err()
                }
            }
        }
    } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
    }

    override suspend fun restore(): Result<DomainError, StoredSessionCredentials?> = try {
        val payload = USER_DEFAULTS.stringForKey(defaultName = USER_DEFAULTS_SESSION_KEY)
        if (payload.isNullOrBlank()) {
            null.ok()
        } else {
            SESSION_JSON.decodeFromString<StoredSessionCredentials>(string = payload).ok()
        }
    } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
    }

    override suspend fun clear(): Result<DomainError, Unit> = try {
        when (val result = deleteStoredValueIfPresent()) {
            is KeychainOperationResult.Error -> result.error.err()
            KeychainOperationResult.Success -> {
                USER_DEFAULTS.removeObjectForKey(defaultName = USER_DEFAULTS_SESSION_KEY)
                Unit.ok()
            }
        }
    } catch (throwable: Throwable) {
        throwable.toUnknownDomainError().err()
    }

    private fun deleteStoredValueIfPresent(): KeychainOperationResult {
        val status = SecItemDelete(baseQuery() as CFDictionaryRef)

        return when (status) {
            errSecSuccess,
            errSecItemNotFound,
            -> KeychainOperationResult.Success

            else -> KeychainOperationResult.Error(
                error = keychainFailure(
                    operation = "delete session credentials",
                    status = status,
                ),
            )
        }
    }

    private fun baseQuery(): Map<Any?, Any?> = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to KEYCHAIN_SERVICE,
        kSecAttrAccount to KEYCHAIN_ACCOUNT,
    )

    private companion object {
        val SESSION_JSON = Json {
            ignoreUnknownKeys = true
        }
        val USER_DEFAULTS: NSUserDefaults = NSUserDefaults.standardUserDefaults
        const val USER_DEFAULTS_SESSION_KEY = "store_it_active_session"

        const val KEYCHAIN_SERVICE = "org.deafsapps.storeit.session_credentials"
        const val KEYCHAIN_ACCOUNT = "active_session"
    }
}

private sealed interface KeychainOperationResult {
    data object Success : KeychainOperationResult

    data class Error(val error: DomainError) : KeychainOperationResult
}

@OptIn(ExperimentalForeignApi::class)
private fun keychainFailure(
    operation: String,
    status: Int,
): DomainError.Unknown {
    val statusMessage = SecCopyErrorMessageString(status = status, reserved = null)?.toString()
        ?: "OSStatus=$status"

    return DomainError.Unknown(
        message = "Keychain failed to $operation: $statusMessage",
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun String.toNSData(): NSData {
    val bytes = encodeToByteArray()
    val cfData = bytes.usePinned { pinned ->
        CFDataCreate(
            allocator = null,
            bytes = pinned.addressOf(index = 0).reinterpret(),
            length = bytes.size.toLong(),
        )
    } ?: error("Unable to allocate Keychain payload data.")
    return cfData as NSData
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toUtf8String(): String? {
    val payloadLength = length.toInt()
    if (payloadLength <= 0) {
        return ""
    }

    val rawPointer = bytes ?: return null
    return rawPointer.reinterpret<ByteVar>().readBytes(payloadLength).decodeToString()
}
