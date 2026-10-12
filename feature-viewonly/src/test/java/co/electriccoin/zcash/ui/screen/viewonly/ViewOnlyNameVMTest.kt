package co.electriccoin.zcash.ui.screen.viewonly

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.repository.PendingViewOnlyImport
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepositoryImpl
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.screen.connecthw.HWWalletEnrollment
import co.electriccoin.zcash.ui.screen.connecthw.neworactive.HWNewOrActiveArgs
import co.electriccoin.zcash.ui.screen.viewonly.name.ViewOnlyNameState
import co.electriccoin.zcash.ui.screen.viewonly.name.ViewOnlyNameVM
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The name is capped at twelve characters as it is typed, the counter follows it, Continue needs a
 * name that is not blank and stores it trimmed before the birthday question, and back forgets the
 * pending key.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewOnlyNameVMTest {
    private val navigationRouter = mockk<NavigationRouter>(relaxed = true)

    private val repository =
        ViewOnlyPendingImportRepositoryImpl().apply { set(PendingViewOnlyImport(ufvk = UFVK, name = "")) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aLongerNameIsCutToTwelveCharacters() =
        runTest {
            val vm = vm()
            collect(vm)

            type(vm, "Zodl Savings 2026")

            assertEquals(
                "Zodl Savings",
                vm.state.value.field.value
                    .text()
            )
            assertEquals(
                listOf(12, 12),
                vm.state.value.counter
                    .args()
            )
        }

    @Test
    fun emojiCountOnceAndAreNeverCutInHalf() =
        runTest {
            val vm = vm()
            collect(vm)

            type(vm, "Vault " + LOCK.repeat(8))

            assertEquals(
                "Vault " + LOCK.repeat(6),
                vm.state.value.field.value
                    .text()
            )
            assertEquals(
                listOf(12, 12),
                vm.state.value.counter
                    .args()
            )

            type(vm, LOCK.repeat(3))

            assertEquals(
                listOf(3, 12),
                vm.state.value.counter
                    .args()
            )
        }

    @Test
    fun theCounterFollowsTheName() =
        runTest {
            val vm = vm()
            collect(vm)
            assertEquals(
                listOf(0, ViewOnlyNameState.MAX_NAME_LENGTH),
                vm.state.value.counter
                    .args()
            )

            type(vm, "Zodl Vault")

            assertEquals(
                listOf(10, ViewOnlyNameState.MAX_NAME_LENGTH),
                vm.state.value.counter
                    .args()
            )
        }

    @Test
    fun continueNeedsANameThatIsNotBlank() =
        runTest {
            val vm = vm()
            collect(vm)
            assertFalse(vm.state.value.continueButton.isEnabled)

            type(vm, "   ")
            assertFalse(vm.state.value.continueButton.isEnabled)
            vm.state.value.continueButton
                .onClick()
            verify(exactly = 0) { navigationRouter.forward(any()) }

            type(vm, "Vault")
            assertTrue(vm.state.value.continueButton.isEnabled)
        }

    @Test
    fun continueStoresTheTrimmedNameAndAsksForTheBirthday() =
        runTest {
            val vm = vm()
            collect(vm)
            type(vm, " Vault ")

            vm.state.value.continueButton
                .onClick()

            assertEquals(PendingViewOnlyImport(ufvk = UFVK, name = "Vault"), repository.get())
            verify(exactly = 1) { navigationRouter.forward(HWNewOrActiveArgs(HWWalletEnrollment.ViewOnly)) }
        }

    @Test
    fun backForgetsThePendingKey() =
        runTest {
            val vm = vm()
            collect(vm)

            vm.state.value.onBack()

            assertNull(repository.get())
            verify(exactly = 1) { navigationRouter.back() }
        }

    @Test
    fun withoutAPendingKeyTheFlowReturnsToTheRoot() =
        runTest {
            repository.clear()

            vm()

            verify(exactly = 1) { navigationRouter.backToRoot() }
        }

    private fun TestScope.type(
        vm: ViewOnlyNameVM,
        text: String,
    ) {
        vm.state.value.field
            .onValueChange(text)
        runCurrent()
    }

    private fun vm() = ViewOnlyNameVM(pendingImportRepository = repository, navigationRouter = navigationRouter)

    private fun TestScope.collect(vm: ViewOnlyNameVM) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        runCurrent()
    }

    private fun StringResource.text() = (this as StringResource.ByString).value

    private fun StringResource.args() = (this as StringResource.ByResource).args.toList()

    private companion object {
        const val UFVK = "uview1namefixture"
    }
}

private const val LOCK = "\uD83D\uDD10"
