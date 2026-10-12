package co.electriccoin.zcash.ui.screen.viewonly

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.KeystoneAccount
import co.electriccoin.zcash.ui.common.model.LedgerAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.usecase.GetWalletAccountsUseCase
import co.electriccoin.zcash.ui.screen.choosehwwallet.ChooseHWWalletArgs
import co.electriccoin.zcash.ui.screen.viewonly.addaccount.ViewOnlyAddAccountTag
import co.electriccoin.zcash.ui.screen.viewonly.addaccount.ViewOnlyAddAccountVM
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The "Add Account" sheet offers a hardware wallet only while a vendor is unconnected, always offers
 * the viewing key import, and hides itself before its choice replaces it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewOnlyAddAccountVMTest {
    private val navigationRouter = mockk<NavigationRouter>(relaxed = true)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun bothOptionsShowWhileAVendorIsUnconnected() =
        runTest {
            val vm = vm(listOf(mockk<ZashiAccount>(), mockk<KeystoneAccount>(), mockk<ViewOnlyAccount>()))
            collect(vm)

            assertEquals(
                listOf(ViewOnlyAddAccountTag.CONNECT_HARDWARE_WALLET, ViewOnlyAddAccountTag.IMPORT_VIEWING_KEY),
                assertNotNull(vm.state.value).options.map { it.testTag }
            )
        }

    @Test
    fun onlyTheImportShowsOnceEveryVendorIsConnected() =
        runTest {
            val vm = vm(listOf(mockk<ZashiAccount>(), mockk<KeystoneAccount>(), mockk<LedgerAccount>()))
            collect(vm)

            assertEquals(
                listOf(ViewOnlyAddAccountTag.IMPORT_VIEWING_KEY),
                assertNotNull(vm.state.value).options.map { it.testTag }
            )
        }

    @Test
    fun twoAccountsOfOneVendorStillLeaveTheOtherVendorToConnect() =
        runTest {
            val vm = vm(listOf(mockk<ZashiAccount>(), mockk<KeystoneAccount>(), mockk<KeystoneAccount>()))
            collect(vm)

            assertEquals(
                listOf(ViewOnlyAddAccountTag.CONNECT_HARDWARE_WALLET, ViewOnlyAddAccountTag.IMPORT_VIEWING_KEY),
                assertNotNull(vm.state.value).options.map { it.testTag }
            )
        }

    @Test
    fun theSheetWaitsForTheAccounts() =
        runTest {
            val vm = vm(accounts = null)
            collect(vm)

            assertNull(vm.state.value)
        }

    @Test
    fun theImportHidesTheSheetThenReplacesIt() =
        runTest {
            val vm = vm(listOf(mockk<ZashiAccount>()))
            collect(vm)

            assertNotNull(vm.state.value).options.last().onClick()
            runCurrent()
            assertNull(vm.state.value)
            verify(exactly = 0) { navigationRouter.replace(any()) }

            advanceTimeBy(HIDE_DELAY_MS)
            runCurrent()
            verify(exactly = 1) { navigationRouter.replace(ViewOnlyImportArgs) }
        }

    @Test
    fun theHardwareWalletOptionHidesTheSheetThenReplacesItWithTheVendorPicker() =
        runTest {
            val vm = vm(listOf(mockk<ZashiAccount>()))
            collect(vm)

            val option = assertNotNull(vm.state.value).options.first()
            option.onClick()
            option.onClick()
            advanceTimeBy(HIDE_DELAY_MS)
            runCurrent()

            verify(exactly = 1) { navigationRouter.replace(ChooseHWWalletArgs) }
        }

    @Test
    fun dismissGoesBack() =
        runTest {
            val vm = vm(listOf(mockk<ZashiAccount>()))
            collect(vm)

            assertNotNull(vm.state.value).dismissButton.onClick()

            verify(exactly = 1) { navigationRouter.back() }
        }

    private fun vm(accounts: List<WalletAccount>?) =
        ViewOnlyAddAccountVM(
            getWalletAccounts =
                mockk<GetWalletAccountsUseCase> { every { observe() } returns MutableStateFlow(accounts) },
            navigationRouter = navigationRouter,
        )

    private fun TestScope.collect(vm: ViewOnlyAddAccountVM) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        runCurrent()
    }

    private companion object {
        const val HIDE_DELAY_MS = 351L
    }
}
