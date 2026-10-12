package co.electriccoin.zcash.ui.screen.accountlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.KeystoneAccount
import co.electriccoin.zcash.ui.common.model.LedgerAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.model.toStorageKeyId
import co.electriccoin.zcash.ui.common.usecase.GetWalletAccountsUseCase
import co.electriccoin.zcash.ui.common.usecase.SelectWalletAccountUseCase
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavigator
import co.electriccoin.zcash.ui.screen.ExternalUrl
import co.electriccoin.zcash.ui.screen.accountlist.options.WalletOptionsArgs
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AccountListVM(
    getWalletAccounts: GetWalletAccountsUseCase,
    private val selectWalletAccount: SelectWalletAccountUseCase,
    private val navigationRouter: NavigationRouter,
    private val viewOnlyNavigator: ViewOnlyNavigator,
    private val viewOnlyContent: ViewOnlyContent,
) : ViewModel() {
    val state: StateFlow<AccountListState> =
        getWalletAccounts
            .observe()
            .map(::createState)
            .stateIn(viewModel = this, initialValue = createState(null))

    private fun createState(accounts: List<WalletAccount>?) =
        AccountListState.create(
            entries = accounts?.map(::toEntry),
            viewOnlySection = viewOnlyContent.accountListSection,
            viewOnlySectionPlural = viewOnlyContent.accountListSectionPlural,
            viewOnlyLabel = viewOnlyContent.topBarSubtitle,
            addAccountText = viewOnlyContent.addAccountButton,
            addAccountIcon = viewOnlyContent.addAccountButtonIcon,
            onKeystonePromoClick = ::onKeystonePromoClick,
            onAddAccountClick = ::onAddAccountClick,
            onBack = ::onBack,
        )

    private fun toEntry(account: WalletAccount) =
        AccountListEntry(
            kind =
                when (account) {
                    is ZashiAccount -> AccountListKind.ZODL
                    is KeystoneAccount -> AccountListKind.KEYSTONE
                    is LedgerAccount -> AccountListKind.LEDGER
                    is ViewOnlyAccount -> AccountListKind.VIEW_ONLY
                },
            icon = account.icon,
            name = account.name,
            balance = account.totalBalance,
            isSelected = account.isSelected,
            onClick = { onAccountClick(account) },
            onOptionsClick =
                when (account) {
                    is ZashiAccount -> {
                        null
                    }

                    is KeystoneAccount, is LedgerAccount -> {
                        { onHardwareWalletOptionsClick(account) }
                    }

                    is ViewOnlyAccount -> {
                        { onViewOnlyOptionsClick(account) }
                    }
                },
        )

    /** Selects the account and closes the screen. */
    private fun onAccountClick(account: WalletAccount) =
        viewModelScope.launch {
            selectWalletAccount(account)
        }

    private fun onHardwareWalletOptionsClick(account: WalletAccount) =
        navigationRouter.forward(WalletOptionsArgs(accountKeyId = account.sdkAccount.accountUuid.toStorageKeyId()))

    private fun onViewOnlyOptionsClick(account: ViewOnlyAccount) =
        viewOnlyNavigator.forwardToEditAccount(account.sdkAccount.accountUuid)

    /**
     * Opens the Keystone shop in the in-app browser, replacing this screen as the account picker did
     * before it lost its promo row.
     */
    private fun onKeystonePromoClick() = navigationRouter.replace(ExternalUrl(KEYSTONE_PROMO_URL))

    /**
     * Opens the "Add Account" options, which offer a hardware wallet only while a vendor is still
     * unconnected and always offer importing a viewing key.
     */
    private fun onAddAccountClick() = viewOnlyNavigator.forwardToAddAccount()

    private fun onBack() = navigationRouter.back()

    companion object {
        const val KEYSTONE_PROMO_URL = "https://keyst.one/shop/products/keystone-3-pro?discount=Zodl"
    }
}
