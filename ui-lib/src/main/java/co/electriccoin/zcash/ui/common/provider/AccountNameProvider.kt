package co.electriccoin.zcash.ui.common.provider

import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.preference.EncryptedPreferenceProvider
import co.electriccoin.zcash.preference.api.PreferenceProvider
import co.electriccoin.zcash.preference.model.entry.PreferenceDefault
import co.electriccoin.zcash.preference.model.entry.PreferenceKey
import co.electriccoin.zcash.ui.common.model.toStorageKeyId
import co.electriccoin.zcash.ui.util.loggableNot
import kotlinx.coroutines.CancellationException

/**
 * The name earlier builds stored for a renamed view-only account in encrypted preferences, keyed by
 * the account's UUID, before the SDK could store an account's name itself. Only read so that
 * [co.electriccoin.zcash.ui.common.usecase.MigrateStoredAccountNamesUseCase] can move it into the
 * wallet database, and cleared once it has, or once the user renames or removes the account, so a stale
 * stored name never overrides a newer one.
 */
interface AccountNameProvider {
    /** The stored name of [accountUuid], or null when none is stored. */
    suspend fun get(accountUuid: AccountUuid): String?

    /**
     * Clears the stored name of [accountUuid], best-effort: a failure is logged without the name and
     * never thrown, so it cannot fail the rename, removal or name move that asked for the clear.
     */
    suspend fun clear(accountUuid: AccountUuid)
}

class AccountNameProviderImpl(
    private val encryptedPreferenceProvider: EncryptedPreferenceProvider
) : AccountNameProvider {
    private val log = loggableNot("AccountNameProvider")

    override suspend fun get(accountUuid: AccountUuid): String? =
        nameDefault(accountUuid).getValue(encryptedPreferenceProvider())

    @Suppress("TooGenericExceptionCaught")
    override suspend fun clear(accountUuid: AccountUuid) {
        try {
            nameDefault(accountUuid).putValue(encryptedPreferenceProvider(), null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log("failed to clear a stored account name", e)
        }
    }
}

private fun nameDefault(accountUuid: AccountUuid) =
    AccountNamePreferenceDefault(PreferenceKey("$NAME_KEY_PREFIX${accountUuid.toStorageKeyId()}"))

/**
 * A blank stored value reads as no name at all, so it is never moved into the wallet database.
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
