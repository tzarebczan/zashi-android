package co.electriccoin.zcash.ui.common.datasource

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountBalance
import cash.z.ecc.android.sdk.model.AccountImportSetup
import cash.z.ecc.android.sdk.model.AccountPurpose
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.UnifiedAddressRequest
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.Zip32AccountIndex
import co.electriccoin.zcash.ui.common.model.KeystoneAccount
import co.electriccoin.zcash.ui.common.model.LedgerAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountAlreadyExistsException
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountImportException
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.provider.AccountNameProvider
import co.electriccoin.zcash.ui.common.provider.LedgerAccountBindingProvider
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.design.util.stringRes
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A view-only account as the data source maps and imports it: it is recognised by its key source
 * before the Zashi fallback (which would need an HD index and a Sapling address it lacks), asks for
 * an Orchard address only, is never selected by default, sorts after every other account, cannot
 * spend, and its import refuses a key the wallet already holds and never carries the key in any
 * exception it raises.
 */
class AccountDataSourceImplViewOnlyTest {
    private val accountUuid = AccountUuid.new(ByteArray(16) { it.toByte() })
    private val account = Account.new(accountUuid)

    private val viewOnlyContent =
        mockk<ViewOnlyContent> {
            every { defaultAccountName } returns DEFAULT_NAME
            every { accountIcon } returns VIEW_ONLY_ICON
        }

    @Test
    fun viewOnlyKeySourceMapsToViewOnlyAccountWithoutTouchingTheHdIndexOrSapling() =
        runBlocking {
            val requests = mutableListOf<UnifiedAddressRequest>()
            val synchronizer =
                mockk<Synchronizer> {
                    every { accountsFlow } returns
                        MutableStateFlow(listOf(viewOnlySdkAccount(name = "Savings", uuid = accountUuid)))
                    every { walletBalances } returns MutableStateFlow(accountBalance(available = 100L, unshielded = 5L))
                    coEvery { getCustomUnifiedAddress(any(), capture(requests)) } returns "unified-address"
                    coEvery { getTransparentAddress(any()) } returns "transparent-address"
                }

            val accounts =
                withTimeout(5_000) {
                    dataSource(synchronizer, relaxedBindingProvider()).allAccounts.filterNotNull().first()
                }

            val viewOnly = accounts.single() as ViewOnlyAccount
            assertEquals(stringRes("Savings"), viewOnly.name)
            assertEquals(VIEW_ONLY_ICON, viewOnly.icon)
            assertFalse(viewOnly.isSpendable)
            assertNull(viewOnly.hdAccountIndex)
            assertNull(viewOnly.saplingAddress)
            assertNull(viewOnly.saplingBalance)
            assertEquals("unified-address", viewOnly.unifiedAddress)
            assertEquals("transparent-address", viewOnly.transparentAddress)
            assertEquals(Zatoshi(100L), viewOnly.spendableShieldedBalance)
            assertEquals(Zatoshi(105L), viewOnly.totalBalance)
            assertEquals(listOf<UnifiedAddressRequest>(UnifiedAddressRequest.Orchard), requests)
            coVerify(exactly = 0) { synchronizer.getSaplingAddress(any()) }
        }

    @Test
    fun aViewOnlyAccountWithABlankNameFallsBackToTheDefaultName() =
        runBlocking {
            val synchronizer =
                mockk<Synchronizer> {
                    every { accountsFlow } returns MutableStateFlow(listOf(viewOnlySdkAccount(name = " ")))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getCustomUnifiedAddress(any(), any()) } returns "unified-address"
                    coEvery { getTransparentAddress(any()) } returns "transparent-address"
                }

            val accounts =
                withTimeout(5_000) {
                    dataSource(synchronizer, relaxedBindingProvider()).allAccounts.filterNotNull().first()
                }

            assertEquals(DEFAULT_NAME, accounts.single().name)
        }

    @Test
    fun aStoredNameReplacesTheImportedNameAndFollowsEveryRename() =
        runBlocking<Unit> {
            val storedName = MutableStateFlow<String?>(null)
            val synchronizer =
                mockk<Synchronizer> {
                    every { accountsFlow } returns
                        MutableStateFlow(listOf(viewOnlySdkAccount(name = "Savings", uuid = accountUuid)))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getCustomUnifiedAddress(any(), any()) } returns "unified-address"
                    coEvery { getTransparentAddress(any()) } returns "transparent-address"
                }
            val dataSource = dataSource(synchronizer, relaxedBindingProvider(), nameProvider(storedName))

            withTimeout(5_000) {
                assertEquals(
                    stringRes("Savings"),
                    dataSource.allAccounts
                        .filterNotNull()
                        .first()
                        .single()
                        .name
                )
                storedName.value = "Vault"
                dataSource.allAccounts.first { it?.singleOrNull()?.name == stringRes("Vault") }
            }
        }

    @Test
    fun onlyAViewOnlyAccountReadsTheNameStore() =
        runBlocking {
            val nameProvider = nameProvider(MutableStateFlow("Renamed"))
            val synchronizer =
                mockk<Synchronizer> {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getCustomUnifiedAddress(any(), any()) } returns "unified-address"
                    coEvery { getTransparentAddress(any()) } returns "transparent-address"
                    coEvery { getSaplingAddress(any()) } returns "sapling-address"
                }

            val accounts =
                withTimeout(5_000) {
                    dataSource(synchronizer, relaxedBindingProvider(), nameProvider)
                        .allAccounts
                        .filterNotNull()
                        .first()
                }

            assertTrue(accounts.single() is ZashiAccount)
            verify(exactly = 0) { nameProvider.observe(any()) }
        }

    @Test
    fun deletingAViewOnlyAccountForgetsItsStoredName() =
        runBlocking {
            val nameProvider = nameProvider()
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { deleteAccount(accountUuid) } returns true
                }

            dataSource(synchronizer, relaxedBindingProvider(), nameProvider).deleteAccount(viewOnlyAccount())

            coVerify(exactly = 1) { synchronizer.deleteAccount(accountUuid) }
            coVerify(exactly = 1) { nameProvider.clear(accountUuid) }
        }

    @Test
    fun deletingAnotherAccountLeavesTheNameStoreAlone() =
        runBlocking {
            val nameProvider = nameProvider()
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { deleteAccount(accountUuid) } returns true
                }

            dataSource(synchronizer, relaxedBindingProvider(), nameProvider).deleteAccount(ledgerAccount())

            coVerify(exactly = 0) { nameProvider.clear(any()) }
        }

    @Test
    fun aViewOnlyAccountIsNeverSelectedByDefault() =
        runBlocking {
            val synchronizer =
                mockk<Synchronizer> {
                    every { accountsFlow } returns
                        MutableStateFlow(listOf(account, viewOnlySdkAccount(name = "Savings")))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getCustomUnifiedAddress(any(), any()) } returns "unified-address"
                    coEvery { getTransparentAddress(any()) } returns "transparent-address"
                    coEvery { getSaplingAddress(any()) } returns "sapling-address"
                }

            val accounts =
                withTimeout(5_000) {
                    dataSource(synchronizer, relaxedBindingProvider()).allAccounts.filterNotNull().first()
                }

            assertTrue(accounts.filterIsInstance<ZashiAccount>().single().isSelected)
            assertFalse(accounts.filterIsInstance<ViewOnlyAccount>().single().isSelected)
        }

    @Test
    fun accountsSortDescendingWithViewOnlyAccountsLast() {
        val zashi =
            ZashiAccount(
                sdkAccount = account,
                unifiedAddress = "u",
                transparentAddress = "t",
                saplingAddress = "s",
                orchardBalance = null,
                saplingBalance = null,
                ironwoodBalance = null,
                transparentBalance = null,
                isSelected = true,
            )
        val keystone =
            KeystoneAccount(
                sdkAccount = account,
                unifiedAddress = "u",
                transparentAddress = "t",
                orchardBalance = null,
                ironwoodBalance = null,
                transparentBalance = null,
                isSelected = false,
            )
        val ledger = ledgerAccount()
        val viewOnly = viewOnlyAccount()

        val sorted = listOf<WalletAccount>(viewOnly, ledger, zashi, keystone).sortedDescending()

        assertEquals(listOf(zashi, keystone, ledger, viewOnly), sorted)
        assertEquals(0, viewOnly.compareTo(viewOnlyAccount()))
        listOf<WalletAccount>(zashi, keystone, ledger).forEach { other ->
            assertTrue(other > viewOnly, "$other must sort above the view-only account")
            assertTrue(viewOnly < other)
        }
    }

    @Test
    fun onlyTheViewOnlyAccountIsNotSpendable() {
        assertTrue(ledgerAccount().isSpendable)
        assertFalse(viewOnlyAccount().isSpendable)
    }

    @Test
    fun aViewOnlyAccountToStringDoesNotLeakItsAddresses() {
        val printed = viewOnlyAccount().toString()

        assertFalse(printed.contains("u1secretaddress"))
        assertFalse(printed.contains("t1secretaddress"))
    }

    @Test
    fun importingAViewOnlyAccountUsesTheViewOnlyPurposeAndKeySource() =
        runBlocking {
            val setup: CapturingSlot<AccountImportSetup> = slot()
            val importedUuid = AccountUuid.new(ByteArray(16) { (it + 1).toByte() })
            val imported = mockk<Account> { every { accountUuid } returns importedUuid }
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getAccounts() } returns listOf(account)
                    coEvery { importAccountByUfvk(capture(setup)) } returns imported
                }

            val created =
                dataSource(synchronizer, relaxedBindingProvider())
                    .importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)

            assertEquals(imported, created)
            assertEquals(AccountPurpose.ViewOnly, setup.captured.purpose)
            assertEquals(VIEW_ONLY_KEYSOURCE, setup.captured.keySource)
            assertEquals("Savings", setup.captured.accountName)
            assertEquals(UFVK, setup.captured.ufvk.encoding)
        }

    @Test
    fun importingAKeyTheWalletAlreadyHoldsIsRefusedBeforeTheSdkIsAsked() =
        runBlocking {
            val existing =
                mockk<Account> {
                    every { accountUuid } returns this@AccountDataSourceImplViewOnlyTest.accountUuid
                    every { ufvk } returns UFVK
                }
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getAccounts() } returns listOf(existing)
                }

            assertFailsWith<ViewOnlyAccountAlreadyExistsException> {
                dataSource(synchronizer, relaxedBindingProvider())
                    .importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)
            }
            coVerify(exactly = 0) { synchronizer.importAccountByUfvk(any()) }
        }

    @Test
    fun anImportThatReturnsAnExistingAccountIsReportedAsAlreadyImported() =
        runBlocking<Unit> {
            val existing =
                mockk<Account> {
                    every { accountUuid } returns this@AccountDataSourceImplViewOnlyTest.accountUuid
                    every { ufvk } returns "uview1differentencoding"
                }
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getAccounts() } returns listOf(existing)
                    coEvery { importAccountByUfvk(any()) } returns existing
                }

            assertFailsWith<ViewOnlyAccountAlreadyExistsException> {
                dataSource(synchronizer, relaxedBindingProvider())
                    .importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)
            }
        }

    /**
     * The SDK's exception must not survive anywhere in the cause chain; the only cause allowed is
     * the copy of the wrapper itself that coroutines' stack-trace recovery may attach.
     */
    @Test
    fun aRejectedImportIsWrappedWithoutTheKeyAnywhereInItsStackTrace() =
        runBlocking {
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getAccounts() } returns listOf(account)
                    coEvery { importAccountByUfvk(any()) } throws
                        IllegalArgumentException("Invalid UFVK: $UFVK", RuntimeException("parse_ufvk($UFVK)"))
                }

            val thrown =
                assertFailsWith<ViewOnlyAccountImportException> {
                    dataSource(synchronizer, relaxedBindingProvider())
                        .importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)
                }

            assertTrue(generateSequence<Throwable>(thrown) { it.cause }.all { it is ViewOnlyAccountImportException })
            assertFalse(thrown.stackTraceToString().contains("uview"))
        }

    @Test
    fun noWrappedImportExceptionLeaksAKeyInItsStackTrace() {
        listOf(
            ViewOnlyAccountImportException(),
            ViewOnlyAccountAlreadyExistsException(),
        ).forEach { exception ->
            assertFalse(exception.stackTraceToString().contains("uview"), exception.toString())
        }
    }

    private fun viewOnlySdkAccount(
        name: String,
        uuid: AccountUuid = AccountUuid.new(ByteArray(16) { (it + 2).toByte() }),
    ) = mockk<Account> {
        every { accountUuid } returns uuid
        every { keySource } returns VIEW_ONLY_KEYSOURCE
        every { accountName } returns name
        every { ufvk } returns UFVK
        every { hdAccountIndex } returns null
    }

    private fun viewOnlyAccount() =
        ViewOnlyAccount(
            sdkAccount = account,
            unifiedAddress = "u1secretaddress",
            transparentAddress = "t1secretaddress",
            orchardBalance = null,
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = false,
            name = stringRes("Savings"),
            icon = VIEW_ONLY_ICON,
        )

    private fun relaxedBindingProvider() =
        mockk<LedgerAccountBindingProvider>(relaxed = true) {
            every { observe(any()) } returns MutableStateFlow(null)
        }

    private fun ledgerAccount() =
        LedgerAccount(
            sdkAccount = account,
            unifiedAddress = "u",
            transparentAddress = "t",
            orchardBalance = null,
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = false,
            deviceIdentity = null,
            zip32AccountIndex = Zip32AccountIndex.new(0L),
        )

    private fun zeroBalance() = WalletBalance(Zatoshi(0), Zatoshi(0), Zatoshi(0))

    private fun accountBalance(
        available: Long,
        unshielded: Long,
    ): Map<AccountUuid, AccountBalance> =
        mapOf(
            accountUuid to
                AccountBalance(
                    sapling = zeroBalance(),
                    orchard = zeroBalance().copy(available = Zatoshi(available)),
                    ironwood = zeroBalance(),
                    unshielded = Zatoshi(unshielded),
                )
        )

    private fun nameProvider(storedName: MutableStateFlow<String?> = MutableStateFlow(null)) =
        mockk<AccountNameProvider>(relaxed = true) {
            every { observe(any()) } returns storedName
        }

    private fun dataSource(
        synchronizer: Synchronizer,
        ledgerAccountBindingProvider: LedgerAccountBindingProvider,
        accountNameProvider: AccountNameProvider = nameProvider(),
    ): AccountDataSourceImpl {
        val synchronizerProvider =
            mockk<SynchronizerProvider> {
                every { this@mockk.synchronizer } returns MutableStateFlow(synchronizer)
                coEvery { getSynchronizer() } returns synchronizer
            }
        return AccountDataSourceImpl(
            synchronizerProvider = synchronizerProvider,
            selectedAccountUUIDProvider =
                mockk {
                    every { uuid } returns MutableStateFlow(null)
                },
            persistableWalletProvider =
                mockk {
                    every { persistableWallet } returns MutableStateFlow(mockk(relaxed = true))
                },
            ledgerAccountBindingProvider = ledgerAccountBindingProvider,
            accountNameProvider = accountNameProvider,
            viewOnlyContent = viewOnlyContent,
            context = mockk(relaxed = true),
        )
    }

    private companion object {
        const val VIEW_ONLY_ICON = 42

        const val UFVK = "uview1testkeythatmustneverbeprinted"

        val DEFAULT_NAME = stringRes(co.electriccoin.zcash.ui.R.string.accounts_zashi)
    }
}
