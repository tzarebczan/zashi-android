package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.provider.SelectedAccountSpendableStorageProvider
import co.electriccoin.zcash.ui.common.repository.SwapRepository
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase
import co.electriccoin.zcash.ui.screen.pay.PayArgs

class NavigateToNearPayUseCase(
    private val swapRepository: SwapRepository,
    private val navigationRouter: NavigationRouter,
    private val accountDataSource: AccountDataSource,
    private val navigateToError: NavigateToErrorUseCase,
    private val selectedAccountSpendableStorageProvider: SelectedAccountSpendableStorageProvider,
    // private val ephemeralAddressRepository: EphemeralAddressRepository,
    // private val isEphemeralAddressLocked: IsEphemeralAddressLockedUseCase,
    // private val createIncreaseEphemeralGapLimitProposal: CreateIncreaseEphemeralGapLimitProposalUseCase,
    // private val navigateToError: NavigateToErrorUseCase
) {
    /**
     * A selected account that cannot spend gets the unsupported-operation error instead of the
     * flow, whose only outcome is spending from it.
     */
    suspend operator fun invoke() {
        val rejected =
            navigateToError.rejectIfSelectedAccountCannotSpend(
                accountDataSource,
                selectedAccountSpendableStorageProvider
            )
        if (rejected) return
        swapRepository.requestRefreshAssets()
        navigationRouter.forward(PayArgs)
        // try {
        //     if (ephemeralAddressRepository.get() == null) {
        //         ephemeralAddressRepository.create()
        //     }
        //     when (isEphemeralAddressLocked()) {
        //         EphemeralLockState.LOCKED -> {
        //             createIncreaseEphemeralGapLimitProposal()
        //             navigationRouter.forward(PayArgs, EphemeralLockArgs)
        //         }
        //         EphemeralLockState.UNLOCKING,
        //         EphemeralLockState.UNLOCKED -> navigationRouter.forward(PayArgs)
        //     }
        // } catch (e: Exception) {
        //     navigateToError(ErrorArgs.General(e))
        // }
    }
}
