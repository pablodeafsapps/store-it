package org.deafsapps.storeit.data.datasource

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.foldMap
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.base.onOk
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.toUnknownDomainError
import org.koin.core.annotation.Single
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSUserDefaults
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSCopyingProtocol
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
        deleteStoredValueIfPresent()
            .foldMap(ifOk = {
                val payloadData = SESSION_JSON.encodeToString(value = session).toNSData()
                val query = baseQuery().apply {
                    setObject(
                        anObject = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                        forKey = cfStringKey(value = kSecAttrAccessible),
                    )
                    setObject(
                        anObject = payloadData,
                        forKey = cfStringKey(value = kSecValueData),
                    )
                }

                val status = query.useAsKeychainQuery { keychainQuery ->
                    SecItemAdd(
                        attributes = keychainQuery,
                        result = null,
                    )
                }

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
            })
    } catch (exception: SerializationException) {
        exception.toUnknownDomainError(message = "Unable to serialize session credentials.").err()
    } catch (exception: IllegalStateException) {
        exception.toUnknownDomainError(message = "Unable to allocate keychain payload data.").err()
    }

    override suspend fun restore(): Result<DomainError, StoredSessionCredentials?> = try {
        val payload = USER_DEFAULTS.stringForKey(defaultName = USER_DEFAULTS_SESSION_KEY)
        if (payload.isNullOrBlank()) {
            null.ok()
        } else {
            SESSION_JSON.decodeFromString<StoredSessionCredentials>(string = payload).ok()
        }
    } catch (exception: SerializationException) {
        exception.toUnknownDomainError(message = "Stored session credentials are malformed.").err()
    }

    override suspend fun clear(): Result<DomainError, Unit> =
        deleteStoredValueIfPresent()
            .onOk { USER_DEFAULTS.removeObjectForKey(defaultName = USER_DEFAULTS_SESSION_KEY) }

    private fun deleteStoredValueIfPresent(): Result<DomainError, Unit> =
        when (val status = baseQuery().useAsKeychainQuery { keychainQuery -> SecItemDelete(query = keychainQuery) }) {
            errSecSuccess,
            errSecItemNotFound, -> Unit.ok()
            else -> keychainFailure(
                operation = "delete session credentials",
                status = status,
            ).err()
        }

    private fun baseQuery(): NSMutableDictionary = NSMutableDictionary().apply {
        setObject(anObject = kSecClassGenericPassword, forKey = cfStringKey(value = kSecClass))
        setObject(anObject = KEYCHAIN_SERVICE, forKey = cfStringKey(value = kSecAttrService))
        setObject(anObject = KEYCHAIN_ACCOUNT, forKey = cfStringKey(value = kSecAttrAccount))
    }

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

    return CFBridgingRelease(cfData) as NSData
}

@OptIn(ExperimentalForeignApi::class)
private fun cfStringKey(value: CFStringRef?): NSCopyingProtocol =
    CFBridgingRelease(CFRetain(value)) as NSCopyingProtocol

@OptIn(ExperimentalForeignApi::class)
private inline fun <T> NSMutableDictionary.useAsKeychainQuery(block: (CFDictionaryRef?) -> T): T {
    val retainedQuery = CFBridgingRetain(this)

    try {
        return block(retainedQuery?.reinterpret())
    } finally {
        if (retainedQuery != null) {
            CFRelease(retainedQuery)
        }
    }
}
