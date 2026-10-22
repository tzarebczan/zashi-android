package co.electriccoin.zcash.ui.common.datasource

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountAlreadyExistsException
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountImportException
import co.electriccoin.zcash.ui.common.model.ViewOnlyImportStep
import co.electriccoin.zcash.ui.common.provider.LedgerAccountBindingProvider
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The view-only import is retried while the engine refuses a wallet mutation because it is still
 * finishing a previous pass, for example the initial sync of an account imported just before, or
 * while the restore anchor cannot be fetched over a slow network or Tor; neither writes anything,
 * so a later attempt can succeed. Retries wait 2 s, 4 s, then 8 s each, 54 s in all, in virtual
 * time here. A key collision and a cancellation are never retried, and a failure that outlasts the
 * retries is reported with the step it failed at.
 *
 * Imports driven through the data source run on `Dispatchers.IO`, out of reach of virtual time, so
 * those tests shorten the waits to [TEST_RETRY_DELAYS]; the schedule itself is pinned by
 * [theRetriesWaitTwoFourThenEightSecondsForAtMostAMinute] and driven through [retryViewOnlyImport].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountDataSourceImplViewOnlyImportRetryTest {
    private val account = Account.new(AccountUuid.new(ByteArray(16) { it.toByte() }))

    @Test
    fun anImportRefusedOnceIsRetriedAndReturnsTheAccount() =
        runBlocking {
            val imported = Account.new(AccountUuid.new(ByteArray(16) { (it + 1).toByte() }))
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getAccounts() } returns listOf(account)
                    coEvery { findAccountSharingKeyComponents(any()) } returns null
                    coEvery { importAccountByUfvk(any()) } throws refusal() andThen imported
                }

            val created =
                dataSource(synchronizer).importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)

            assertEquals(imported, created)
            coVerify(exactly = 2) { synchronizer.importAccountByUfvk(any()) }
            coVerify(exactly = 1) { synchronizer.findAccountSharingKeyComponents(any()) }
            coVerify(exactly = 1) { synchronizer.getAccounts() }
        }

    @Test
    fun theRetriesWaitTwoFourThenEightSecondsForAtMostAMinute() {
        assertEquals(
            listOf(2, 4, 8, 8, 8, 8, 8, 8).map { it.seconds },
            VIEW_ONLY_IMPORT_RETRY_DELAYS
        )
        assertTrue(VIEW_ONLY_IMPORT_RETRY_DELAYS.fold(Duration.ZERO, Duration::plus) <= 60.seconds)
    }

    @Test
    fun aRefusedAttemptIsRetriedAfterTheFirstBackoff() =
        runTest {
            var calls = 0

            val result =
                retryViewOnlyImport {
                    calls++
                    if (calls == 1) throw refusal()
                    "imported"
                }

            assertEquals("imported", result)
            assertEquals(2, calls)
            assertEquals(2_000L, testScheduler.currentTime)
        }

    @Test
    fun anAnchorFailureIsRetriedUntilTheImportSucceedsInsideTheWindow() =
        runTest {
            var calls = 0

            val result =
                retryViewOnlyImport {
                    calls++
                    if (calls <= 4) throw anchorUnavailable()
                    "imported"
                }

            assertEquals("imported", result)
            assertEquals(5, calls)
            assertEquals(22_000L, testScheduler.currentTime)
        }

    @Test
    fun anEngineBusyRefusalIsRetriedUntilTheLastAttemptOfTheWindow() =
        runTest {
            var calls = 0

            val result =
                retryViewOnlyImport {
                    calls++
                    if (calls <= 8) throw engineBusy()
                    "imported"
                }

            assertEquals("imported", result)
            assertEquals(9, calls)
            assertEquals(54_000L, testScheduler.currentTime)
        }

    @Test
    fun aFailureThatOutlastsTheWindowGivesUpAfterNineAttemptsAndFiftyFourSeconds() =
        runTest {
            var calls = 0

            assertFailsWith<InitializeException.ImportAccountAnchorUnavailableException> {
                retryViewOnlyImport<Unit> {
                    calls++
                    throw anchorUnavailable()
                }
            }

            assertEquals(9, calls)
            assertEquals(54_000L, testScheduler.currentTime)
        }

    @Test
    fun aKeyCollisionIsNotRetried() =
        runTest {
            var calls = 0

            assertFailsWith<InitializeException.ImportAccountCollisionException> {
                retryViewOnlyImport<Unit> {
                    calls++
                    throw InitializeException.ImportAccountCollisionException(null)
                }
            }

            assertEquals(1, calls)
            assertEquals(0L, testScheduler.currentTime)
        }

    @Test
    fun aCancellationIsNotRetried() =
        runTest {
            var calls = 0

            assertFailsWith<CancellationException> {
                retryViewOnlyImport<Unit> {
                    calls++
                    throw CancellationException("cancelled")
                }
            }

            assertEquals(1, calls)
            assertEquals(0L, testScheduler.currentTime)
        }

    @Test
    fun anAnchorFailureThatOutlastsTheRetriesIsReportedAtTheAnchorStep() =
        runBlocking {
            val synchronizer = failingSynchronizer(anchorUnavailable())

            val thrown =
                assertFailsWith<ViewOnlyAccountImportException> {
                    dataSource(synchronizer).importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)
                }

            assertEquals(ViewOnlyImportStep.ANCHOR, thrown.step)
            assertEquals("The viewing key could not be imported (anchor).", thrown.message)
            assertTrue(generateSequence<Throwable>(thrown) { it.cause }.all { it is ViewOnlyAccountImportException })
            assertFalse(thrown.stackTraceToString().contains("uview"))
            coVerify(exactly = TEST_RETRY_DELAYS.size + 1) { synchronizer.importAccountByUfvk(any()) }
        }

    @Test
    fun anEngineBusyRefusalThatOutlastsTheRetriesIsReportedAtTheEngineBusyStep() =
        runBlocking {
            val synchronizer = failingSynchronizer(engineBusy())

            val thrown =
                assertFailsWith<ViewOnlyAccountImportException> {
                    dataSource(synchronizer).importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)
                }

            assertEquals(ViewOnlyImportStep.ENGINE_BUSY, thrown.step)
            assertEquals("The viewing key could not be imported (engine-busy).", thrown.message)
            coVerify(exactly = TEST_RETRY_DELAYS.size + 1) { synchronizer.importAccountByUfvk(any()) }
        }

    @Test
    fun anyOtherImportFailureIsReportedAtTheImportStep() =
        runBlocking {
            val synchronizer = failingSynchronizer(refusal())

            val thrown =
                assertFailsWith<ViewOnlyAccountImportException> {
                    dataSource(synchronizer).importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)
                }

            assertEquals(ViewOnlyImportStep.IMPORT, thrown.step)
            assertEquals("The viewing key could not be imported (import).", thrown.message)
            coVerify(exactly = TEST_RETRY_DELAYS.size + 1) { synchronizer.importAccountByUfvk(any()) }
        }

    @Test
    fun aFailedOverlapLookupIsReportedAtTheOverlapStepWithoutImporting() =
        runBlocking {
            val synchronizer =
                mockk<Synchronizer>(relaxed = true) {
                    every { accountsFlow } returns MutableStateFlow(listOf(account))
                    every { walletBalances } returns MutableStateFlow(null)
                    coEvery { getAccounts() } returns listOf(account)
                    coEvery { findAccountSharingKeyComponents(any()) } throws
                        InitializeException.FindAccountSharingKeyComponentsException(RuntimeException("lookup failed"))
                }

            val thrown =
                assertFailsWith<ViewOnlyAccountImportException> {
                    dataSource(synchronizer).importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)
                }

            assertEquals(ViewOnlyImportStep.OVERLAP, thrown.step)
            assertEquals("The viewing key could not be imported (overlap).", thrown.message)
            coVerify(exactly = 0) { synchronizer.importAccountByUfvk(any()) }
        }

    @Test
    fun aCollisionDuringTheImportIsReportedAsAlreadyImportedWithoutRetrying() =
        runBlocking {
            val synchronizer = failingSynchronizer(InitializeException.ImportAccountCollisionException(null))

            assertFailsWith<ViewOnlyAccountAlreadyExistsException> {
                dataSource(synchronizer).importViewOnlyAccount(ufvk = UFVK, name = "Savings", birthday = null)
            }

            coVerify(exactly = 1) { synchronizer.importAccountByUfvk(any()) }
        }

    @Test
    fun theStepOfAFailureIsDecidedByItsTypeAlone() {
        assertEquals(ViewOnlyImportStep.ANCHOR, viewOnlyImportFailureStep(anchorUnavailable()))
        assertEquals(ViewOnlyImportStep.ENGINE_BUSY, viewOnlyImportFailureStep(engineBusy()))
        assertEquals(ViewOnlyImportStep.IMPORT, viewOnlyImportFailureStep(refusal()))
        assertEquals(ViewOnlyImportStep.IMPORT, viewOnlyImportFailureStep(RuntimeException("anchor")))
    }

    private fun refusal() = InitializeException.ImportAccountException(RuntimeException("mutation refused"))

    private fun anchorUnavailable() =
        InitializeException.ImportAccountAnchorUnavailableException(RuntimeException("tor circuit build timed out"))

    private fun engineBusy() =
        InitializeException.ImportAccountCheckpointsNotReadyException(IllegalStateException("engine did not quiesce"))

    /** A synchronizer that finds no overlap and fails every import with [failure]. */
    private fun failingSynchronizer(failure: Exception) =
        mockk<Synchronizer>(relaxed = true) {
            every { accountsFlow } returns MutableStateFlow(listOf(account))
            every { walletBalances } returns MutableStateFlow(null)
            coEvery { getAccounts() } returns listOf(account)
            coEvery { findAccountSharingKeyComponents(any()) } returns null
            coEvery { importAccountByUfvk(any()) } throws failure
        }

    private fun dataSource(synchronizer: Synchronizer): AccountDataSourceImpl {
        val synchronizerProvider =
            mockk<SynchronizerProvider> {
                every { this@mockk.synchronizer } returns MutableStateFlow(synchronizer)
                coEvery { getSynchronizer() } returns synchronizer
            }
        return AccountDataSourceImpl(
            synchronizerProvider = synchronizerProvider,
            selectedAccountUUIDProvider =
                mockk(relaxed = true) {
                    every { uuid } returns MutableStateFlow(null)
                    coEvery { getUUID() } returns null
                },
            persistableWalletProvider =
                mockk {
                    every { persistableWallet } returns MutableStateFlow(mockk(relaxed = true))
                },
            ledgerAccountBindingProvider =
                mockk<LedgerAccountBindingProvider>(relaxed = true) {
                    every { observe(any()) } returns MutableStateFlow(null)
                },
            viewOnlyContent = mockk(relaxed = true),
            context = mockk(relaxed = true),
        ).apply { viewOnlyImportRetryDelays = TEST_RETRY_DELAYS }
    }

    private companion object {
        const val UFVK = "uview1testkeythatmustneverbeprinted"

        /** Short waits for imports driven through the data source, which runs them on `Dispatchers.IO`. */
        val TEST_RETRY_DELAYS = List(3) { 1.milliseconds }
    }
}
