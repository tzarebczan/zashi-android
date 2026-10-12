package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase
import co.electriccoin.zcash.ui.screen.send.Send

class SendTransactionAgainUseCase(
    private val prefillSendUseCase: PrefillSendUseCase,
    private val navigationRouter: NavigationRouter,
    private val accountDataSource: AccountDataSource,
    private val navigateToError: NavigateToErrorUseCase,
) {
    /**
     * An account that cannot spend gets the unsupported-operation error instead of a prefilled send
     * form.
     */
    operator fun invoke(value: DetailedTransactionData) {
        if (navigateToError.rejectIfSelectedAccountCannotSpend(accountDataSource)) return
        prefillSendUseCase.requestFromTransactionDetail(value)
        navigationRouter.forward(
            Send(
                isScanZip321Enabled = false
            )
        )
    }
}
