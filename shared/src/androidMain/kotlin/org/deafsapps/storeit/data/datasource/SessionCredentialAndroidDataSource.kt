package org.deafsapps.storeit.data.datasource

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.json.JSONException
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.toUnknownDomainError
import org.koin.core.annotation.Single

@Single(binds = [SessionCredentialDataSource::class])
internal class SessionCredentialAndroidDataSource(
    context: Context,
) : SessionCredentialDataSource {
    private val applicationContext = context.applicationContext

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = {
                applicationContext.preferencesDataStoreFile(name = DATASTORE_NAME)
            },
        )
    }

    override suspend fun save(session: StoredSessionCredentials): Result<DomainError, Unit> = try {
        val encryptedPayload = encrypt(plainText = session.toJson())
        dataStore.edit { preferences ->
            preferences[KEY_ENCRYPTED_PAYLOAD] = encryptedPayload.cipherText
            preferences[KEY_ENCRYPTION_IV] = encryptedPayload.initializationVector
        }
        Unit.ok()
    } catch (exception: IOException) {
        exception.toUnknownDomainError(message = "Unable to persist encrypted session credentials.").err()
    } catch (exception: GeneralSecurityException) {
        exception.toUnknownDomainError(message = "Unable to encrypt session credentials.").err()
    } catch (exception: JSONException) {
        exception.toUnknownDomainError(message = "Unable to serialize session credentials.").err()
    }

    override suspend fun restore(): Result<DomainError, StoredSessionCredentials?> = try {
        val preferences = dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(value = emptyPreferences())
            } else {
                throw exception
            }
        }.first()
        val encryptedPayload = preferences[KEY_ENCRYPTED_PAYLOAD]
        val encryptionIv = preferences[KEY_ENCRYPTION_IV]

        if (encryptedPayload.isNullOrBlank() || encryptionIv.isNullOrBlank()) {
            null.ok()
        } else {
            decrypt(
                encryptedPayload = encryptedPayload,
                initializationVector = encryptionIv,
            ).toStoredSessionCredentials().ok()
        }
    } catch (exception: GeneralSecurityException) {
        exception.toUnknownDomainError(message = "Unable to decrypt session credentials.").err()
    } catch (exception: IllegalArgumentException) {
        exception.toUnknownDomainError(message = "Stored session credentials are malformed.").err()
    } catch (exception: JSONException) {
        exception.toUnknownDomainError(message = "Stored session credentials are malformed.").err()
    }

    override suspend fun clear(): Result<DomainError, Unit> = try {
        dataStore.edit { preferences ->
            preferences.remove(key = KEY_ENCRYPTED_PAYLOAD)
            preferences.remove(key = KEY_ENCRYPTION_IV)
        }
        Unit.ok()
    } catch (exception: IOException) {
        exception.toUnknownDomainError(message = "Unable to clear persisted session credentials.").err()
    }

    private fun encrypt(plainText: String): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        return EncryptedPayload(
            cipherText = cipher.doFinal(plainText.toByteArray()).toBase64(),
            initializationVector = cipher.iv.toBase64(),
        )
    }

    private fun decrypt(
        encryptedPayload: String,
        initializationVector: String,
    ): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, initializationVector.fromBase64())
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), gcmSpec)

        return cipher.doFinal(encryptedPayload.fromBase64()).decodeToString()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    private companion object {
        const val DATASTORE_NAME = "store_it_session_credentials"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "store_it_session_credentials_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128

        val KEY_ENCRYPTED_PAYLOAD = stringPreferencesKey(name = "encrypted_payload")
        val KEY_ENCRYPTION_IV = stringPreferencesKey(name = "encryption_iv")
    }
}

private data class EncryptedPayload(
    val cipherText: String,
    val initializationVector: String,
)

private fun StoredSessionCredentials.toJson(): String = JSONObject()
    .put("accountId", accountId)
    .put("email", email)
    .put("accessToken", accessToken)
    .put("refreshToken", refreshToken)
    .put("lastAuthenticatedAt", lastAuthenticatedAt)
    .toString()

private fun String.toStoredSessionCredentials(): StoredSessionCredentials {
    val jsonObject = JSONObject(this)

    return StoredSessionCredentials(
        accountId = jsonObject.getString("accountId"),
        email = jsonObject.getString("email"),
        accessToken = jsonObject.optString("accessToken").takeIf { it.isNotEmpty() },
        refreshToken = jsonObject.optString("refreshToken").takeIf { it.isNotEmpty() },
        lastAuthenticatedAt = jsonObject.takeIf { !it.isNull("lastAuthenticatedAt") }
            ?.optLong("lastAuthenticatedAt"),
    )
}

private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
