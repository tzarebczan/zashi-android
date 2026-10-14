package co.electriccoin.zcash.ui.common.repository

import androidx.compose.ui.graphics.Color
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.datasource.MetadataDataSource
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.model.metadata.AccountAppearanceMetadataV3
import co.electriccoin.zcash.ui.common.model.metadata.AccountMetadataV3
import co.electriccoin.zcash.ui.common.model.metadata.MetadataV3
import co.electriccoin.zcash.ui.common.model.metadata.SwapsMetadataV3
import co.electriccoin.zcash.ui.common.provider.MetadataKeyStorageProvider
import co.electriccoin.zcash.ui.common.serialization.metadata.MetadataKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [MetadataRepositoryImpl] writing an account's colour under that account's own metadata key,
 * whichever account is selected, and sharing every account's metadata in one map, empty at first.
 */
class MetadataRepositoryAccountColorTest {
    private val selectedKey = mockk<MetadataKey>()
    private val viewOnlyKey = mockk<MetadataKey>()
    private val selected = account<ZashiAccount>(SELECTED_UUID)
    private val viewOnly = account<ViewOnlyAccount>(VIEW_ONLY_UUID)
    private val metadataDataSource = mockk<MetadataDataSource>(relaxed = true)
    private val accounts = MutableStateFlow<List<WalletAccount>?>(listOf(selected, viewOnly))

    private val repository =
        MetadataRepositoryImpl(
            accountDataSource =
                mockk<AccountDataSource> {
                    every { selectedAccount } returns flowOf(selected)
                    every { allAccounts } returns accounts
                    coEvery { getSelectedAccount() } returns selected
                },
            metadataDataSource = metadataDataSource,
            metadataKeyStorageProvider =
                mockk<MetadataKeyStorageProvider> {
                    coEvery { get(SELECTED_UUID) } returns selectedKey
                    coEvery { get(VIEW_ONLY_UUID) } returns viewOnlyKey
                },
            persistableWalletProvider = mockk(),
            simpleSwapAssetProvider = mockk(relaxed = true)
        )

    @Test
    fun aColorIsWrittenUnderTheAccountsOwnKey() =
        runTest {
            coEvery { metadataDataSource.setAccountColor(any(), any()) } returns true

            assertTrue(repository.setAccountColor(viewOnly, Color(PICKED)))

            coVerify(exactly = 1) { metadataDataSource.setAccountColor(Color(PICKED), viewOnlyKey) }
            coVerify(exactly = 0) { metadataDataSource.setAccountColor(any(), selectedKey) }
        }

    @Test
    fun aFailedWriteReportsFalse() =
        runTest {
            coEvery { metadataDataSource.setAccountColor(any(), any()) } throws IOException()
            assertFalse(repository.setAccountColor(viewOnly, Color(PICKED)))

            coEvery { metadataDataSource.setAccountColor(any(), any()) } returns false
            assertFalse(repository.setAccountColor(viewOnly, Color(PICKED)))
        }

    @Test
    fun theMapIsEmptyFirst() {
        assertEquals(emptyMap(), repository.metadata.value)
    }

    @Test
    fun theMapHoldsEveryAccountsMetadataReadUnderItsOwnKey() =
        runTest {
            every { metadataDataSource.observe(selectedKey) } returns flowOf(null, metadata(null))
            every { metadataDataSource.observe(viewOnlyKey) } returns flowOf(null, metadata(Color(PICKED)))

            assertEquals(
                mapOf(SELECTED_UUID to metadata(null), VIEW_ONLY_UUID to metadata(Color(PICKED))),
                repository.metadata.first { it.size == 2 }
            )
        }

    @Test
    fun anAccountsColorFollowsAWrite() =
        runTest {
            val stored = MutableStateFlow<MetadataV3?>(metadata(null))
            every { metadataDataSource.observe(selectedKey) } returns flowOf(null)
            every { metadataDataSource.observe(viewOnlyKey) } returns stored
            coEvery { metadataDataSource.setAccountColor(any(), viewOnlyKey) } answers {
                stored.value = metadata(Color(PICKED))
                true
            }

            val color = async { repository.observeColor(VIEW_ONLY_UUID).first { it != null } }
            repository.setAccountColor(viewOnly, Color(PICKED))

            assertEquals(Color(PICKED), color.await())
        }

    private inline fun <reified T : WalletAccount> account(uuid: AccountUuid) =
        mockk<T> { every { sdkAccount } returns Account.new(uuid) }

    private fun metadata(color: Color?) =
        MetadataV3(
            lastUpdated = Instant.ofEpochSecond(1),
            accountMetadata =
                AccountMetadataV3(
                    bookmarked = emptyList(),
                    read = emptyList(),
                    annotations = emptyList(),
                    swaps = SwapsMetadataV3(swapIds = emptyList(), lastUsedAssetHistory = emptySet()),
                    appearance =
                        color?.let { AccountAppearanceMetadataV3(color = it, lastUpdated = Instant.ofEpochSecond(1)) }
                )
        )

    private companion object {
        val SELECTED_UUID: AccountUuid = AccountUuid.new(ByteArray(16) { it.toByte() })
        val VIEW_ONLY_UUID: AccountUuid = AccountUuid.new(ByteArray(16) { (it + 1).toByte() })
        const val PICKED = 0x80BE38F3
    }
}
