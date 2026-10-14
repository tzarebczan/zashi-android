package co.electriccoin.zcash.ui.common.provider

import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.preference.EncryptedPreferenceProvider
import co.electriccoin.zcash.preference.api.PreferenceProvider
import co.electriccoin.zcash.preference.model.entry.PreferenceKey
import co.electriccoin.zcash.ui.common.model.toStorageKeyId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A view-only account's name as earlier builds stored it in encrypted preferences, read back and
 * cleared under a key derived from the account's UUID. Clearing is best-effort: a failing store never
 * makes it throw.
 */
class AccountNameProviderTest {
    private val accountUuid = AccountUuid.new(ByteArray(16) { it.toByte() })
    private val otherAccountUuid = AccountUuid.new(ByteArray(16) { (it + 1).toByte() })

    @Test
    fun aStoredNameIsReadUnderTheAccountsStorageKeyId() =
        runTest {
            val store = InMemoryNamePreferenceProvider()
            store.putString(PreferenceKey(nameKey), "Vault")

            assertEquals("Vault", provider(store).get(accountUuid))
        }

    @Test
    fun anAccountWithNoStoredNameReadsNull() =
        runTest {
            val store = InMemoryNamePreferenceProvider()
            store.putString(PreferenceKey(nameKey), "Vault")

            assertNull(provider(store).get(otherAccountUuid))
        }

    @Test
    fun clearingDropsTheName() =
        runTest {
            val store = InMemoryNamePreferenceProvider()
            store.putString(PreferenceKey(nameKey), "Vault")
            val provider = provider(store)

            provider.clear(accountUuid)

            assertNull(provider.get(accountUuid))
            assertNull(store.getString(PreferenceKey(nameKey)))
        }

    @Test
    fun aStoreThatFailsToClearIsOnlyLogged() =
        runTest {
            val provider =
                AccountNameProviderImpl(
                    encryptedPreferenceProvider =
                        mockk<EncryptedPreferenceProvider> {
                            coEvery { this@mockk.invoke() } throws IllegalStateException("preferences unavailable")
                        }
                )

            provider.clear(accountUuid)
        }

    @Test
    fun aBlankStoredNameReadsAsNoName() =
        runTest {
            val store = InMemoryNamePreferenceProvider()
            store.putString(PreferenceKey(nameKey), " ")

            assertNull(provider(store).get(accountUuid))
        }

    private val nameKey get() = "account_display_name_${accountUuid.toStorageKeyId()}"

    private fun provider(store: PreferenceProvider) =
        AccountNameProviderImpl(
            encryptedPreferenceProvider =
                mockk<EncryptedPreferenceProvider> {
                    coEvery { this@mockk.invoke() } returns store
                }
        )
}

/**
 * A hand-written in-memory [PreferenceProvider], as in LedgerAccountBindingProviderTest: MockK's
 * call recorder throws on suspend members that take the [PreferenceKey] value class.
 */
private class InMemoryNamePreferenceProvider : PreferenceProvider {
    private val values = mutableMapOf<String, MutableStateFlow<String?>>()

    private fun flowFor(key: PreferenceKey) = values.getOrPut(key.key) { MutableStateFlow(null) }

    override suspend fun hasKey(key: PreferenceKey) = flowFor(key).value != null

    override suspend fun putString(
        key: PreferenceKey,
        value: String?
    ) {
        flowFor(key).value = value
    }

    override suspend fun putStringSet(
        key: PreferenceKey,
        value: Set<String>?
    ) = Unit

    override suspend fun putLong(
        key: PreferenceKey,
        value: Long?
    ) {
        flowFor(key).value = value?.toString()
    }

    override suspend fun getLong(key: PreferenceKey): Long? = flowFor(key).value?.toLongOrNull()

    override suspend fun getString(key: PreferenceKey): String? = flowFor(key).value

    override suspend fun getStringSet(key: PreferenceKey): Set<String>? = null

    override fun observe(key: PreferenceKey): Flow<String?> = flowFor(key)

    override suspend fun remove(key: PreferenceKey) {
        flowFor(key).value = null
    }

    override suspend fun clearPreferences(): Boolean {
        values.clear()
        return true
    }
}
