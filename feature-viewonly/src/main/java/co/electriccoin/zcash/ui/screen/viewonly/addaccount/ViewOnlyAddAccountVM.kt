package co.electriccoin.zcash.ui.screen.viewonly.addaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.HWWalletAccount
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.usecase.GetWalletAccountsUseCase
import co.electriccoin.zcash.ui.screen.choosehwwallet.ChooseHWWalletArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyImportArgs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Offers the ways to add an account. Picking one first hides the sheet and only then replaces it
 * with the chosen flow, so the sheet animates away instead of vanishing under the next screen and
 * leaves no hidden destination behind on the back stack.
 */
class ViewOnlyAddAccountVM(
    getWalletAccounts: GetWalletAccountsUseCase,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val isHidden = MutableStateFlow(false)

    private var navigateJob: Job? = null

    val state: StateFlow<ViewOnlyAddAccountState?> =
        combine(getWalletAccounts.observe(), isHidden) { accounts, isHidden ->
            if (accounts == null || isHidden) {
                null
            } else {
                ViewOnlyAddAccountState.create(
                    canConnectHardwareWallet = canConnectHardwareWallet(accounts),
                    onConnectHardwareWalletClick = { hideThenReplaceWith(ChooseHWWalletArgs) },
                    onImportClick = { hideThenReplaceWith(ViewOnlyImportArgs) },
                    onBack = ::onBack,
                )
            }
        }.stateIn(this)

    /**
     * The option disappears once every supported vendor is connected. A view-only account is not a
     * hardware wallet and does not count.
     */
    private fun canConnectHardwareWallet(accounts: List<WalletAccount>) =
        accounts.count { it is HWWalletAccount } < SUPPORTED_HW_VENDORS

    private fun hideThenReplaceWith(route: Any) {
        if (navigateJob?.isActive == true) return
        navigateJob =
            viewModelScope.launch {
                isHidden.update { true }
                delay(SHEET_HIDE_DELAY)
                navigationRouter.replace(route)
            }
    }

    private fun onBack() {
        if (navigateJob?.isActive == true) return
        navigationRouter.back()
    }

    private companion object {
        const val SUPPORTED_HW_VENDORS = 2
        val SHEET_HIDE_DELAY = 350.milliseconds
    }
}
