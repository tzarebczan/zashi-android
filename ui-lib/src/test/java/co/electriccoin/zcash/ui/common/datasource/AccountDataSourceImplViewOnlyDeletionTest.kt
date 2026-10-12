package co.electriccoin.zcash.ui.common.datasource

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.Zip32AccountIndex
import co.electriccoin.zcash.ui.common.model.LedgerAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.provider.AccountNameProvider
import co.electriccoin.zcash.ui.common.provider.LedgerAccountBindingProvider
import co.electriccoin.zcash.ui.common.provider.SelectedAccountSpendableStorageProvider
import co.electriccoin.zcash.ui.common.provider.SelectedAccountUUIDProvider
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.design.util.stringRes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Deleting a view-only account: the selection is cleared only after the SDK deleted the account and
 * only when the stored selection still names it, whatever the account snapshot says, and clearing
 * the selection or the stored name is best-effort once the account is gone.
 */
class AccountDataSourceImplViewOnlyDeletionTest {
    private val accountUuid = AccountUuid.new(ByteArray(16) { it.toByte() })
    private val account = Account.new(accountUuid)

    private val viewOnlyContent =
        mockk<ViewOnlyContent> {
            every { defaultAccountName } returns DEFAULT_NAME
            every { accountIcon } returns VIEW_ONLY_ICON
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
    fun deletingTheSelectedViewOnlyAccountClearsTheSelectionAfterwards() =
        runBlocking {
            val selection = selectionProvider(selected = accountUuid)
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { deleteAccount(accountUuid) } returns true
                }

            dataSource(synchronizer, relaxedBindingProvider(), selectedAccountUUIDProvider = selection)
                .deleteAccount(viewOnlyAccount().copy(isSelected = true))

            coVerifyOrder {
                synchronizer.deleteAccount(accountUuid)
                selection.clearUUID()
            }
        }

    @Test
    fun aFailedDeletionKeepsTheSelection() =
        runBlocking {
            val selection = selectionProvider(selected = accountUuid)
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { deleteAccount(accountUuid) } returns false
                }

            assertFailsWith<AccountDeletionException> {
                dataSource(synchronizer, relaxedBindingProvider(), selectedAccountUUIDProvider = selection)
                    .deleteAccount(viewOnlyAccount().copy(isSelected = true))
            }

            coVerify(exactly = 0) { selection.clearUUID() }
        }

    @Test
    fun aStaleSelectedSnapshotDoesNotClearTheSelectionOfAnotherAccount() =
        runBlocking {
            val otherUuid = AccountUuid.new(ByteArray(16) { (it + 7).toByte() })
            val selection = selectionProvider(selected = otherUuid)
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { deleteAccount(accountUuid) } returns true
                }

            dataSource(synchronizer, relaxedBindingProvider(), selectedAccountUUIDProvider = selection)
                .deleteAccount(viewOnlyAccount().copy(isSelected = true))

            coVerify(exactly = 1) { synchronizer.deleteAccount(accountUuid) }
            coVerify(exactly = 0) { selection.clearUUID() }
        }

    @Test
    fun aPreferencesFailureAfterASuccessfulDeletionIsNotReported() =
        runBlocking {
            val selection =
                mockk<SelectedAccountUUIDProvider>(relaxed = true) {
                    every { uuid } returns MutableStateFlow(accountUuid)
                    coEvery { getUUID() } returns accountUuid
                    coEvery { clearUUID() } throws IllegalStateException("preferences unavailable")
                }
            val nameProvider =
                mockk<AccountNameProvider>(relaxed = true) {
                    every { observe(any()) } returns MutableStateFlow(null)
                    coEvery { clear(any()) } throws IllegalStateException("preferences unavailable")
                }
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { deleteAccount(accountUuid) } returns true
                }

            dataSource(
                synchronizer = synchronizer,
                ledgerAccountBindingProvider = relaxedBindingProvider(),
                accountNameProvider = nameProvider,
                selectedAccountUUIDProvider = selection,
            ).deleteAccount(viewOnlyAccount().copy(isSelected = true))

            coVerify(exactly = 1) { selection.clearUUID() }
            coVerify(exactly = 1) { nameProvider.clear(accountUuid) }
        }

    @Test
    fun aFailedSelectionReadAfterASuccessfulDeletionIsNotReported() =
        runBlocking {
            val selection =
                mockk<SelectedAccountUUIDProvider>(relaxed = true) {
                    every { uuid } returns MutableStateFlow(accountUuid)
                    coEvery { getUUID() } throws IllegalStateException("preferences unavailable")
                }
            val nameProvider = nameProvider()
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { deleteAccount(accountUuid) } returns true
                }

            dataSource(
                synchronizer = synchronizer,
                ledgerAccountBindingProvider = relaxedBindingProvider(),
                accountNameProvider = nameProvider,
                selectedAccountUUIDProvider = selection,
            ).deleteAccount(viewOnlyAccount())

            coVerify(exactly = 1) { nameProvider.clear(accountUuid) }
        }

    @Test
    fun deletingAnUnselectedAccountKeepsTheSelection() =
        runBlocking {
            val selection = selectionProvider(selected = null)
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { deleteAccount(accountUuid) } returns true
                }

            dataSource(synchronizer, relaxedBindingProvider(), selectedAccountUUIDProvider = selection)
                .deleteAccount(viewOnlyAccount())

            coVerify(exactly = 0) { selection.clearUUID() }
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

    private fun viewOnlyAccount() =
        ViewOnlyAccount(
            sdkAccount = account,
            unifiedAddress = "u1secretaddress",
            transparentAddress = "t1secretaddress",
            orchardBalance = null,
            saplingBalance = null,
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

    private fun nameProvider(storedName: MutableStateFlow<String?> = MutableStateFlow(null)) =
        mockk<AccountNameProvider>(relaxed = true) {
            every { observe(any()) } returns storedName
        }

    private fun selectionProvider(selected: AccountUuid?) =
        mockk<SelectedAccountUUIDProvider>(relaxed = true) {
            every { uuid } returns MutableStateFlow(selected)
            coEvery { getUUID() } returns selected
        }

    private fun dataSource(
        synchronizer: Synchronizer,
        ledgerAccountBindingProvider: LedgerAccountBindingProvider,
        accountNameProvider: AccountNameProvider = nameProvider(),
        selectedAccountUUIDProvider: SelectedAccountUUIDProvider = selectionProvider(selected = null),
        selectedAccountSpendableStorageProvider: SelectedAccountSpendableStorageProvider = mockk(relaxed = true),
    ): AccountDataSourceImpl {
        val synchronizerProvider =
            mockk<SynchronizerProvider> {
                every { this@mockk.synchronizer } returns MutableStateFlow(synchronizer)
                coEvery { getSynchronizer() } returns synchronizer
            }
        return AccountDataSourceImpl(
            synchronizerProvider = synchronizerProvider,
            selectedAccountUUIDProvider = selectedAccountUUIDProvider,
            persistableWalletProvider =
                mockk {
                    every { persistableWallet } returns MutableStateFlow(mockk(relaxed = true))
                },
            ledgerAccountBindingProvider = ledgerAccountBindingProvider,
            accountNameProvider = accountNameProvider,
            selectedAccountSpendableStorageProvider = selectedAccountSpendableStorageProvider,
            viewOnlyContent = viewOnlyContent,
            context = mockk(relaxed = true),
        )
    }

    private companion object {
        const val VIEW_ONLY_ICON = 42

        val DEFAULT_NAME = stringRes(co.electriccoin.zcash.ui.R.string.accounts_zashi)
    }
}
