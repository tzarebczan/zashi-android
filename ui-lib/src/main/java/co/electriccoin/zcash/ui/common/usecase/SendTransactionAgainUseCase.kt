package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.provider.SelectedAccountSpendableStorageProvider
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase
import co.electriccoin.zcash.ui.screen.send.Send

class SendTransactionAgainUseCase(
    private val prefillSendUseCase: PrefillSendUseCase,
    private val navigationRouter: NavigationRouter,
    private val accountDataSource: AccountDataSource,
    private val navigateToError: NavigateToErrorUseCase,
    private val selectedAccountSpendableStorageProvider: SelectedAccountSpendableStorageProvider,
) {
    /**
     * An account that cannot spend gets the unsupported-operation error instead of a prefilled send
     * form.
     */
    suspend operator fun invoke(value: DetailedTransactionData) {
        val rejected =
            navigateToError.rejectIfSelectedAccountCannotSpend(
                accountDataSource,
                selectedAccountSpendableStorageProvider
            )
        if (rejected) return
        prefillSendUseCase.requestFromTransactionDetail(value)
        navigationRouter.forward(
            Send(
                isScanZip321Enabled = false
            )
        )
    }
}
