package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountAlreadyExistsException
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountImportException
import co.electriccoin.zcash.ui.common.model.ViewOnlyPendingImportMissingException
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.repository.PendingViewOnlyImport
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepositoryImpl
import co.electriccoin.zcash.ui.screen.keepopen.KeepOpenArgs
import co.electriccoin.zcash.ui.screen.keepopen.KeepOpenFlow
import co.electriccoin.zcash.ui.screen.viewingkey.AlreadyImportedVKArgs
import co.electriccoin.zcash.ui.screen.viewingkey.ImportedVKArgs
import co.electriccoin.zcash.viewingkey.ViewOnlyAccountImporterImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The import imports the pending key under its name, selects the new account, forgets the key and
 * restarts the synchronizer, then shows the success screen, through the keep-open screen when there
 * is history to scan. A key the wallet already holds ends on the "already imported" sheet; nothing
 * pending fails without touching the wallet; an SDK failure keeps the key so the import can be
 * retried. The importer ui-lib calls delegates to it.
 */
class ImportViewOnlyAccountUseCaseTest {
    private val repository = ViewOnlyPendingImportRepositoryImpl()

    private val created = mockk<Account>()

    private val accountDataSource =
        mockk<AccountDataSource>(relaxed = true) {
            coEvery { importViewOnlyAccount(any(), any(), any()) } returns created
        }

    private val synchronizerProvider = mockk<SynchronizerProvider>(relaxed = true)

    private val navigationRouter = mockk<NavigationRouter>(relaxed = true)

    private val useCase =
        ImportViewOnlyAccountUseCase(repository, accountDataSource, synchronizerProvider, navigationRouter)

    @Test
    fun anAccountWithoutHistoryGoesStraightToTheSuccessScreen() =
        runTest {
            repository.set(PendingViewOnlyImport(ufvk = UFVK, name = "Savings"))

            useCase(birthday = null)

            coVerify(exactly = 1) { accountDataSource.importViewOnlyAccount(UFVK, "Savings", null) }
            coVerify(exactly = 1) { accountDataSource.selectAccount(created) }
            verify(exactly = 1) { synchronizerProvider.resetSynchronizer() }
            verify(exactly = 1) { navigationRouter.forward(ImportedVKArgs) }
            assertNull(repository.get())
        }

    @Test
    fun anAccountWithHistoryGoesThroughTheKeepOpenScreen() =
        runTest {
            repository.set(PendingViewOnlyImport(ufvk = UFVK, name = "Savings"))
            val birthday = BlockHeight.new(2_000_000L)

            useCase(birthday)

            coVerify(exactly = 1) { accountDataSource.importViewOnlyAccount(UFVK, "Savings", birthday) }
            verify(exactly = 1) { navigationRouter.forward(KeepOpenArgs(KeepOpenFlow.VIEW_ONLY)) }
            assertNull(repository.get())
        }

    @Test
    fun aKeyTheWalletAlreadyHoldsEndsOnTheAlreadyImportedSheet() =
        runTest {
            repository.set(PendingViewOnlyImport(ufvk = UFVK, name = "Savings"))
            coEvery { accountDataSource.importViewOnlyAccount(any(), any(), any()) } throws
                ViewOnlyAccountAlreadyExistsException()

            useCase(birthday = null)

            verify(exactly = 1) { navigationRouter.forward(AlreadyImportedVKArgs) }
            coVerify(exactly = 0) { accountDataSource.selectAccount(any<Account>()) }
            verify(exactly = 0) { synchronizerProvider.resetSynchronizer() }
            assertNull(repository.get())
        }

    @Test
    fun anSdkFailureKeepsTheKeySoTheImportCanBeRetried() =
        runTest {
            repository.set(PendingViewOnlyImport(ufvk = UFVK, name = "Savings"))
            coEvery { accountDataSource.importViewOnlyAccount(any(), any(), any()) } throws
                ViewOnlyAccountImportException()

            val error = assertFailsWith<ViewOnlyAccountImportException> { useCase(birthday = null) }

            assertFalse(error.stackTraceToString().contains("uview"))
            assertEquals(UFVK, repository.get()?.ufvk)
            verify(exactly = 0) { navigationRouter.forward(any()) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aSecondCallWhileTheFirstImportsReturnsWithoutImportingAgain() =
        runTest {
            repository.set(PendingViewOnlyImport(ufvk = UFVK, name = "Savings"))
            val imported = CompletableDeferred<Account>()
            coEvery { accountDataSource.importViewOnlyAccount(any(), any(), any()) } coAnswers { imported.await() }

            val first = async { useCase(birthday = null) }
            val second = async { useCase(birthday = null) }
            runCurrent()
            imported.complete(created)
            first.await()
            second.await()

            coVerify(exactly = 1) { accountDataSource.importViewOnlyAccount(any(), any(), any()) }
            coVerify(exactly = 1) { accountDataSource.selectAccount(created) }
            verify(exactly = 1) { synchronizerProvider.resetSynchronizer() }
            verify(exactly = 1) { navigationRouter.forward(ImportedVKArgs) }
            verify(exactly = 0) { navigationRouter.forward(AlreadyImportedVKArgs) }
        }

    @Test
    fun aSecondCallAfterAFailedImportRetriesIt() =
        runTest {
            repository.set(PendingViewOnlyImport(ufvk = UFVK, name = "Savings"))
            coEvery { accountDataSource.importViewOnlyAccount(any(), any(), any()) } throws
                ViewOnlyAccountImportException() andThen created

            assertFailsWith<ViewOnlyAccountImportException> { useCase(birthday = null) }
            useCase(birthday = null)

            coVerify(exactly = 2) { accountDataSource.importViewOnlyAccount(UFVK, "Savings", null) }
            verify(exactly = 1) { navigationRouter.forward(ImportedVKArgs) }
            assertNull(repository.get())
        }

    @Test
    fun nothingPendingFailsWithoutTouchingTheWallet() =
        runTest {
            assertFailsWith<ViewOnlyPendingImportMissingException> { useCase(birthday = null) }

            coVerify(exactly = 0) { accountDataSource.importViewOnlyAccount(any(), any(), any()) }
            verify(exactly = 0) { synchronizerProvider.resetSynchronizer() }
        }

    @Test
    fun theImporterReportsThePendingKeyAndDelegatesTheImport() =
        runTest {
            val importer = ViewOnlyAccountImporterImpl(repository, useCase)
            assertFalse(importer.hasPendingImport())

            repository.set(PendingViewOnlyImport(ufvk = UFVK, name = "Savings"))
            assertTrue(importer.hasPendingImport())

            importer.importAccount(null)

            coVerify(exactly = 1) { accountDataSource.importViewOnlyAccount(UFVK, "Savings", null) }
            assertFalse(importer.hasPendingImport())
        }

    @Test
    fun thePendingImportNeverPrintsItsKey() {
        assertFalse(PendingViewOnlyImport(ufvk = UFVK, name = "Savings").toString().contains("uview"))
    }

    private companion object {
        const val UFVK = "uview1testkeythatmustneverbeprinted"
    }
}
