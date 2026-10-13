package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountAlreadyExistsException
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountImportException
import co.electriccoin.zcash.ui.common.model.ViewOnlyPendingImportMissingException
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.repository.PendingViewOnlyImport
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository
import co.electriccoin.zcash.ui.screen.keepopen.KeepOpenArgs
import co.electriccoin.zcash.ui.screen.keepopen.KeepOpenFlow
import co.electriccoin.zcash.ui.screen.viewingkey.AlreadyImportedVKArgs
import co.electriccoin.zcash.ui.screen.viewingkey.ImportedVKArgs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Imports the pending viewing key under its name at [invoke]'s birthday, selects the new account,
 * forgets the key and restarts the synchronizer, then moves on: straight to the success screen when
 * the account imports from the chain tip, through the keep-open screen when it has history to scan.
 * A key the wallet turns out to hold already ends the flow on the "already imported" sheet.
 *
 * Imports run one at a time. A call that finds the pending import it started with already taken by
 * the call before it (a double tap) returns without doing anything, so it neither imports the key a
 * second time nor reports the account the first call just created as already imported. Registered
 * as a single, so every caller shares the one lock.
 */
internal class ImportViewOnlyAccountUseCase(
    private val pendingImportRepository: ViewOnlyPendingImportRepository,
    private val accountDataSource: AccountDataSource,
    private val synchronizerProvider: SynchronizerProvider,
    private val navigationRouter: NavigationRouter,
) {
    private val mutex = Mutex()

    /**
     * @throws ViewOnlyPendingImportMissingException if the pending import is gone, typically after
     *         process death.
     * @throws ViewOnlyAccountImportException if the SDK rejected the key.
     */
    suspend operator fun invoke(birthday: BlockHeight?) {
        val requested = pendingImportRepository.get() ?: throw ViewOnlyPendingImportMissingException()
        mutex.withLock {
            if (pendingImportRepository.get() === requested) import(requested, birthday)
        }
    }

    private suspend fun import(
        pending: PendingViewOnlyImport,
        birthday: BlockHeight?,
    ) {
        val created =
            try {
                accountDataSource.importViewOnlyAccount(
                    ufvk = pending.ufvk,
                    name = pending.name,
                    birthday = birthday,
                )
            } catch (_: ViewOnlyAccountAlreadyExistsException) {
                pendingImportRepository.clear()
                navigationRouter.forward(AlreadyImportedVKArgs)
                return
            }
        accountDataSource.selectAccount(created)
        pendingImportRepository.clear()
        synchronizerProvider.resetSynchronizer()
        if (birthday != null) {
            navigationRouter.forward(KeepOpenArgs(KeepOpenFlow.VIEW_ONLY))
        } else {
            navigationRouter.forward(ImportedVKArgs)
        }
    }
}
