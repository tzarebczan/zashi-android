package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.preference.StandardPreferenceProvider
import co.electriccoin.zcash.preference.api.PreferenceProvider
import co.electriccoin.zcash.preference.model.entry.PreferenceKey
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.AccountOperationUnsupportedException
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.provider.SelectedAccountSpendableStorageProviderImpl
import co.electriccoin.zcash.ui.screen.error.ErrorArgs
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Until the accounts load, the guard decides from the answer stored when the app last ran, so a
 * cold start on a view-only account cannot open the send form; once they load, the selected
 * account's own answer decides, whatever was stored.
 */
class ViewOnlyNavigationGuardTest {
    private val navigateToError = mockk<NavigateToErrorUseCase>(relaxed = true)

    @Test
    fun aColdStartOnAnAccountStoredAsNotSpendableShowsTheUnsupportedError() =
        runTest {
            assertTrue(guard(accounts = null, stored = "false"))
            verifyViewOnlyErrorShown(times = 1)
        }

    @Test
    fun aColdStartOnAnAccountStoredAsSpendableLetsTheFlowOpen() =
        runTest {
            assertFalse(guard(accounts = null, stored = "true"))
            verifyViewOnlyErrorShown(times = 0)
        }

    @Test
    fun aColdStartWithNothingStoredLetsTheFlowOpenAsHomeDoes() =
        runTest {
            assertFalse(guard(accounts = null, stored = null))
            verifyViewOnlyErrorShown(times = 0)
        }

    @Test
    fun aLoadedViewOnlyAccountIsRefusedWhateverWasStored() =
        runTest {
            assertTrue(guard(accounts = listOf(selected(isSpendable = false)), stored = "true"))
            verifyViewOnlyErrorShown(times = 1)
        }

    @Test
    fun aLoadedSpendableAccountOpensTheFlowWhateverWasStored() =
        runTest {
            assertFalse(guard(accounts = listOf(selected(isSpendable = true)), stored = "false"))
            verifyViewOnlyErrorShown(times = 0)
        }

    private suspend fun guard(
        accounts: List<WalletAccount>?,
        stored: String?,
    ): Boolean {
        val preferences = mockk<PreferenceProvider> { coEvery { getString(HINT_KEY) } returns stored }
        val holder = mockk<StandardPreferenceProvider> { coEvery { this@mockk() } returns preferences }
        return navigateToError.rejectIfSelectedAccountCannotSpend(
            accountDataSource = mockk<AccountDataSource> { every { allAccounts } returns MutableStateFlow(accounts) },
            selectedAccountSpendableStorageProvider = SelectedAccountSpendableStorageProviderImpl(holder),
        )
    }

    private fun selected(isSpendable: Boolean) =
        mockk<WalletAccount> {
            every { isSelected } returns true
            every { this@mockk.isSpendable } returns isSpendable
        }

    private fun verifyViewOnlyErrorShown(times: Int) {
        verify(exactly = times) { navigateToError(match { it.isViewOnlyError() }, any()) }
    }

    private fun ErrorArgs.isViewOnlyError() =
        ((this as? ErrorArgs.General)?.exception as? AccountOperationUnsupportedException)?.kind ==
            AccountOperationUnsupportedException.Kind.VIEW_ONLY
}

/** The key the hint is stored under; matched exactly because MockK cannot match a value class with any(). */
private val HINT_KEY = PreferenceKey("selected_account_spendable")
