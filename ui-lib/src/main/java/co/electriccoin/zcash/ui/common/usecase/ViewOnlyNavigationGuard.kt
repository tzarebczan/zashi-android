package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.AccountOperationUnsupportedException
import co.electriccoin.zcash.ui.common.provider.SelectedAccountSpendableStorageProvider
import co.electriccoin.zcash.ui.screen.error.ErrorArgs
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase

/**
 * Shows the view-only unsupported-operation error, through [navigate], when the selected account
 * cannot spend, and returns true; returns false otherwise. Until the accounts load, the answer
 * stored when the app last ran stands in for the selected account's own, as on Home.
 * For the entry points into flows whose only outcome is spending from the selected account.
 */
internal suspend fun NavigateToErrorUseCase.rejectIfSelectedAccountCannotSpend(
    accountDataSource: AccountDataSource,
    selectedAccountSpendableStorageProvider: SelectedAccountSpendableStorageProvider,
    navigate: NavigationRouter.(Any) -> Unit = { forward(it) },
): Boolean {
    val accounts = accountDataSource.allAccounts.value
    val isSpendable =
        if (accounts == null) {
            selectedAccountSpendableStorageProvider.get()
        } else {
            accounts.firstOrNull { it.isSelected }?.isSpendable != false
        }
    if (isSpendable) return false
    this(
        ErrorArgs.General(AccountOperationUnsupportedException.viewOnly()),
        navigate
    )
    return true
}
