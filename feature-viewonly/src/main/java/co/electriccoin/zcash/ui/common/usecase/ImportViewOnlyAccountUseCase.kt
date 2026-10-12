package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountAlreadyExistsException
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountImportException
import co.electriccoin.zcash.ui.common.model.ViewOnlyPendingImportMissingException
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository
import co.electriccoin.zcash.ui.screen.keepopen.KeepOpenArgs
import co.electriccoin.zcash.ui.screen.keepopen.KeepOpenFlow
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyAlreadyImportedArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyImportedArgs

/**
 * Imports the pending viewing key under its name at [invoke]'s birthday, selects the new account,
 * forgets the key and restarts the synchronizer, then moves on: straight to the success screen when
 * the account imports from the chain tip, through the keep-open screen when it has history to scan.
 * A key the wallet turns out to hold already ends the flow on the "already imported" sheet.
 */
class ImportViewOnlyAccountUseCase(
    private val pendingImportRepository: ViewOnlyPendingImportRepository,
    private val accountDataSource: AccountDataSource,
    private val synchronizerProvider: SynchronizerProvider,
    private val navigationRouter: NavigationRouter,
) {
    /**
     * @throws ViewOnlyPendingImportMissingException if the pending import is gone, typically after
     *         process death.
     * @throws ViewOnlyAccountImportException if the SDK rejected the key.
     */
    suspend operator fun invoke(birthday: BlockHeight?) {
        val pending = pendingImportRepository.get() ?: throw ViewOnlyPendingImportMissingException()
        val created =
            try {
                accountDataSource.importViewOnlyAccount(
                    ufvk = pending.ufvk,
                    name = pending.name,
                    birthday = birthday,
                )
            } catch (_: ViewOnlyAccountAlreadyExistsException) {
                pendingImportRepository.clear()
                navigationRouter.forward(ViewOnlyAlreadyImportedArgs)
                return
            }
        accountDataSource.selectAccount(created)
        pendingImportRepository.clear()
        synchronizerProvider.resetSynchronizer()
        if (birthday != null) {
            navigationRouter.forward(KeepOpenArgs(KeepOpenFlow.VIEW_ONLY))
        } else {
            navigationRouter.forward(ViewOnlyImportedArgs)
        }
    }
}
