package co.electriccoin.zcash.ui.screen.viewonly

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.repository.PendingViewOnlyImport
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepositoryImpl
import co.electriccoin.zcash.ui.common.usecase.ClearClipboardUseCase
import co.electriccoin.zcash.ui.common.usecase.SubmitViewOnlyKeyResult
import co.electriccoin.zcash.ui.common.usecase.SubmitViewOnlyKeyUseCase
import co.electriccoin.zcash.ui.screen.viewonly.paste.ViewOnlyPasteVM
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The paste screen enables Continue for any text, submits it once, and routes by the outcome: an
 * importable key to naming, a key the wallet holds to the "already imported" sheet, anything else
 * to the invalid-key sheet. A pasted key is wiped from the clipboard on Continue and on back, and
 * the key never shows up in the state's text.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewOnlyPasteVMTest {
    private val navigationRouter = mockk<NavigationRouter>(relaxed = true)

    private val clearClipboard = mockk<ClearClipboardUseCase>(relaxed = true)

    private val repository = ViewOnlyPendingImportRepositoryImpl()

    private var result = SubmitViewOnlyKeyResult.Accepted

    private val submit = mockk<SubmitViewOnlyKeyUseCase> { coEvery { this@mockk.invoke(any()) } answers { result } }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun continueIsEnabledOnlyWithText() =
        runTest {
            val vm = vm()
            collect(vm)

            assertFalse(vm.state.value.continueButton.isEnabled)
            vm.state.value.field
                .onValueChange("   ")
            runCurrent()
            assertFalse(vm.state.value.continueButton.isEnabled)
            vm.state.value.field
                .onValueChange(TYPED)
            runCurrent()
            assertTrue(vm.state.value.continueButton.isEnabled)
        }

    @Test
    fun anImportableKeyGoesOnToNaming() =
        runTest {
            val vm = vm()
            collect(vm)
            type(vm, TYPED)

            vm.state.value.continueButton
                .onClick()
            runCurrent()

            coVerify(exactly = 1) { submit.invoke(TYPED) }
            verify(exactly = 1) { navigationRouter.forward(ViewOnlyNameArgs) }
            assertNull(vm.invalidKeySheet.value)
        }

    @Test
    fun aKeyTheWalletHoldsOpensTheAlreadyImportedSheet() =
        runTest {
            result = SubmitViewOnlyKeyResult.AlreadyImported
            val vm = vm()
            collect(vm)
            type(vm, TYPED)

            vm.state.value.continueButton
                .onClick()
            runCurrent()

            verify(exactly = 1) { navigationRouter.forward(ViewOnlyAlreadyImportedArgs) }
        }

    @Test
    fun anInvalidKeyOpensTheInvalidKeySheetAndEditClosesIt() =
        runTest {
            result = SubmitViewOnlyKeyResult.Invalid
            val vm = vm()
            collect(vm)
            type(vm, "garbage")

            vm.state.value.continueButton
                .onClick()
            runCurrent()

            val sheet = assertNotNull(vm.invalidKeySheet.value)
            verify(exactly = 0) { navigationRouter.forward(any()) }
            sheet.primaryAction.onClick()
            runCurrent()
            assertNull(vm.invalidKeySheet.value)
        }

    @Test
    fun scanInsteadHidesTheSheetBeforeOpeningTheScanner() =
        runTest {
            result = SubmitViewOnlyKeyResult.Invalid
            val vm = vm()
            collect(vm)
            type(vm, "garbage")
            vm.state.value.continueButton
                .onClick()
            runCurrent()

            assertNotNull(assertNotNull(vm.invalidKeySheet.value).secondaryAction).onClick()
            runCurrent()
            assertNull(vm.invalidKeySheet.value)
            verify(exactly = 0) { navigationRouter.forward(ViewOnlyScanArgs) }

            advanceTimeBy(HIDE_DELAY_MS)
            runCurrent()
            verify(exactly = 1) { navigationRouter.forward(ViewOnlyScanArgs) }
        }

    @Test
    fun aDoubleTapSubmitsOnce() =
        runTest {
            val gate = CompletableDeferred<SubmitViewOnlyKeyResult>()
            coEvery { submit.invoke(any()) } coAnswers { gate.await() }
            val vm = vm()
            collect(vm)
            type(vm, TYPED)

            vm.state.value.continueButton
                .onClick()
            runCurrent()
            assertTrue(vm.state.value.continueButton.isLoading)
            vm.state.value.continueButton
                .onClick()
            gate.complete(SubmitViewOnlyKeyResult.Accepted)
            runCurrent()

            coVerify(exactly = 1) { submit.invoke(any()) }
        }

    @Test
    fun backDuringTheCheckCancelsItSoNothingNavigatesLater() =
        runTest {
            val gate = CompletableDeferred<SubmitViewOnlyKeyResult>()
            coEvery { submit.invoke(any()) } coAnswers { gate.await() }
            repository.set(PendingViewOnlyImport(ufvk = "uview1pending", name = ""))
            val vm = vm()
            collect(vm)
            type(vm, TYPED)
            vm.state.value.continueButton
                .onClick()
            runCurrent()

            vm.state.value.onBack()
            gate.complete(SubmitViewOnlyKeyResult.Accepted)
            runCurrent()

            verify(exactly = 1) { navigationRouter.back() }
            verify(exactly = 0) { navigationRouter.forward(any()) }
            assertNull(repository.get())
        }

    @Test
    fun aPastedKeyIsWipedFromTheClipboardOnContinue() =
        runTest {
            val vm = vm()
            collect(vm)
            type(vm, TYPED)

            vm.state.value.continueButton
                .onClick()
            runCurrent()

            verify(exactly = 1) { clearClipboard.invoke() }
        }

    @Test
    fun aPastedKeyIsWipedFromTheClipboardOnBack() =
        runTest {
            val vm = vm()
            collect(vm)
            type(vm, TYPED)

            vm.state.value.onBack()

            verify(exactly = 1) { clearClipboard.invoke() }
        }

    @Test
    fun aTypedKeyLeavesTheClipboardAlone() =
        runTest {
            val vm = vm()
            collect(vm)
            TYPED.forEachIndexed { index, _ -> type(vm, TYPED.substring(0, index + 1)) }

            vm.state.value.continueButton
                .onClick()
            runCurrent()
            vm.state.value.onBack()

            verify(exactly = 0) { clearClipboard.invoke() }
        }

    @Test
    fun theStateNeverPrintsTheKey() =
        runTest {
            val vm = vm()
            collect(vm)
            type(vm, TYPED)

            assertFalse(
                vm.state.value
                    .toString()
                    .contains("uview")
            )
        }

    @Test
    fun theQrButtonOpensTheScanner() =
        runTest {
            val vm = vm()
            collect(vm)

            vm.state.value.scanButton
                .onClick()

            verify(exactly = 1) { navigationRouter.forward(ViewOnlyScanArgs) }
        }

    private fun TestScope.type(
        vm: ViewOnlyPasteVM,
        text: String,
    ) {
        vm.state.value.field
            .onValueChange(text)
        runCurrent()
    }

    private fun vm() =
        ViewOnlyPasteVM(
            submitViewOnlyKey = submit,
            clearClipboard = clearClipboard,
            pendingImportRepository = repository,
            navigationRouter = navigationRouter,
        )

    private fun TestScope.collect(vm: ViewOnlyPasteVM) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.invalidKeySheet.collect { } }
        runCurrent()
    }

    private companion object {
        const val TYPED = "uview1thisisafixturethatisnotarealkey"
        const val HIDE_DELAY_MS = 351L
    }
}
