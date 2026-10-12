package co.electriccoin.zcash.ui.common.provider

import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.preference.EncryptedPreferenceProvider
import co.electriccoin.zcash.preference.api.PreferenceProvider
import co.electriccoin.zcash.preference.model.entry.PreferenceDefault
import co.electriccoin.zcash.preference.model.entry.PreferenceKey
import co.electriccoin.zcash.ui.common.model.toStorageKeyId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * The name the user gave an account after it was created, kept in encrypted preferences and keyed
 * by the account's UUID.
 *
 * The SDK stores an account's name only once, when the account is imported, and offers no way to
 * change it, so a later rename lives here and takes precedence over the SDK's name wherever the
 * account is shown. Like every encrypted preference it is local to this install: it is not part of
 * the cloud backup, a wallet reset clears it, and a restored wallet shows the SDK's name again.
 */
interface AccountNameProvider {
    /** The stored name of [accountUuid], or null when the user never renamed it. */
    fun observe(accountUuid: AccountUuid): Flow<String?>

    suspend fun save(
        accountUuid: AccountUuid,
        name: String
    )

    suspend fun clear(accountUuid: AccountUuid)
}

class AccountNameProviderImpl(
    private val encryptedPreferenceProvider: EncryptedPreferenceProvider
) : AccountNameProvider {
    override fun observe(accountUuid: AccountUuid): Flow<String?> =
        flow {
            emitAll(nameDefault(accountUuid).observe(encryptedPreferenceProvider()))
        }

    override suspend fun save(
        accountUuid: AccountUuid,
        name: String
    ) = nameDefault(accountUuid).putValue(encryptedPreferenceProvider(), name)

    override suspend fun clear(accountUuid: AccountUuid) =
        nameDefault(accountUuid).putValue(encryptedPreferenceProvider(), null)
}

private fun nameDefault(accountUuid: AccountUuid) =
    AccountNamePreferenceDefault(PreferenceKey("$NAME_KEY_PREFIX${accountUuid.toStorageKeyId()}"))

/**
 * A blank stored value reads as no name at all, so the account falls back to its SDK name instead
 * of showing nothing.
 */
private class AccountNamePreferenceDefault(
    override val key: PreferenceKey
) : PreferenceDefault<String?> {
    override suspend fun getValue(preferenceProvider: PreferenceProvider): String? =
        preferenceProvider.getString(key)?.takeIf { it.isNotBlank() }

    override suspend fun putValue(
        preferenceProvider: PreferenceProvider,
        newValue: String?
    ) = preferenceProvider.putString(key, newValue?.takeIf { it.isNotBlank() })
}

private const val NAME_KEY_PREFIX = "account_display_name_"
