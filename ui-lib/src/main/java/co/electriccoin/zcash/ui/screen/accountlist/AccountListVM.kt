package co.electriccoin.zcash.ui.screen.accountlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.usecase.GetWalletAccountsUseCase
import co.electriccoin.zcash.ui.common.usecase.SelectWalletAccountUseCase
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavigator
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.util.stringResByAddress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountListVM(
    getWalletAccounts: GetWalletAccountsUseCase,
    private val selectWalletAccount: SelectWalletAccountUseCase,
    private val navigationRouter: NavigationRouter,
    private val viewOnlyNavigator: ViewOnlyNavigator,
    private val viewOnlyContent: ViewOnlyContent,
) : ViewModel() {
    val state =
        getWalletAccounts
            .observe()
            .map { accounts ->
                AccountListState(
                    items =
                        accounts?.map { account ->
                            ZashiAccountListItemState(
                                title = account.name,
                                subtitle = stringResByAddress(account.unifiedAddress),
                                icon = account.icon,
                                isSelected = account.isSelected,
                                onClick = { onAccountClicked(account) }
                            )
                        },
                    isLoading = accounts == null,
                    onBack = ::onBack,
                    addWalletButton =
                        ButtonState(
                            text = viewOnlyContent.addAccountButton,
                            style = ButtonStyle.PRIMARY,
                            icon = viewOnlyContent.addAccountButtonIcon,
                            onClick = ::onAddWalletButtonClicked
                        ).takeIf { accounts != null }
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Companion.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue = null
            )

    private fun onAccountClicked(account: WalletAccount) =
        viewModelScope.launch {
            selectWalletAccount(account)
        }

    /**
     * Opens the "Add Account" options, which offer a hardware wallet only while a vendor is still
     * unconnected and always offer importing a viewing key.
     */
    private fun onAddWalletButtonClicked() = viewOnlyNavigator.forwardToAddAccount()

    private fun onBack() = navigationRouter.back()
}
