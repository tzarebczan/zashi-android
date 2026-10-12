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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * An account's user-given name, round-tripped through the encrypted store under a key derived from
 * the account's UUID.
 */
class AccountNameProviderTest {
    private val accountUuid = AccountUuid.new(ByteArray(16) { it.toByte() })
    private val otherAccountUuid = AccountUuid.new(ByteArray(16) { (it + 1).toByte() })

    @Test
    fun aSavedNameComesBackOutOfTheStore() =
        runTest {
            val provider = provider()

            provider.save(accountUuid, "Vault")

            assertEquals("Vault", provider.observe(accountUuid).first())
        }

    @Test
    fun anAccountThatWasNeverRenamedObservesNull() =
        runTest {
            val provider = provider()

            provider.save(accountUuid, "Vault")

            assertNull(provider.observe(otherAccountUuid).first())
        }

    @Test
    fun clearingDropsTheName() =
        runTest {
            val provider = provider()
            provider.save(accountUuid, "Vault")

            provider.clear(accountUuid)

            assertNull(provider.observe(accountUuid).first())
        }

    @Test
    fun aBlankNameIsNotStoredAndReadsAsNoName() =
        runTest {
            val store = InMemoryNamePreferenceProvider()
            val provider = provider(store)
            provider.save(accountUuid, "Vault")

            provider.save(accountUuid, "   ")

            assertNull(store.getString(PreferenceKey(nameKey)))
            store.putString(PreferenceKey(nameKey), " ")
            assertNull(provider.observe(accountUuid).first())
        }

    @Test
    fun theNameIsKeyedByTheAccountsStorageKeyId() =
        runTest {
            val store = InMemoryNamePreferenceProvider()

            provider(store).save(accountUuid, "Vault")

            assertEquals("Vault", store.getString(PreferenceKey(nameKey)))
        }

    private val nameKey get() = "account_display_name_${accountUuid.toStorageKeyId()}"

    private fun provider(store: PreferenceProvider = InMemoryNamePreferenceProvider()) =
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
