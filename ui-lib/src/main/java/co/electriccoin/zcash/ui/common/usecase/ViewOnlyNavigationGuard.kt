package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.AccountOperationUnsupportedException
import co.electriccoin.zcash.ui.screen.error.ErrorArgs
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase

/**
 * Shows the view-only unsupported-operation error, through [navigate], when the selected account
 * cannot spend, and returns true; returns false otherwise, also while no account has loaded yet.
 * For the entry points into flows whose only outcome is spending from the selected account.
 */
internal fun NavigateToErrorUseCase.rejectIfSelectedAccountCannotSpend(
    accountDataSource: AccountDataSource,
    navigate: NavigationRouter.(Any) -> Unit = { forward(it) },
): Boolean {
    val selected = accountDataSource.allAccounts.value?.firstOrNull { it.isSelected }
    if (selected?.isSpendable != false) return false
    this(
        ErrorArgs.General(AccountOperationUnsupportedException.viewOnly()),
        navigate
    )
    return true
}
