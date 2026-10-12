package co.electriccoin.zcash.ui.screen.viewonly

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.provider.AccountNameProvider
import co.electriccoin.zcash.ui.common.usecase.GetViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.RenameViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.ViewOnlyAccountFixtures
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.viewonly.edit.ViewOnlyEditAccountVM
import co.electriccoin.zcash.ui.screen.viewonly.name.ViewOnlyNameState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The edit screen starts with the account's name, caps the name at twelve characters as it is
 * typed, enables Save only for a new non-blank name, saves it once however often Save is tapped and
 * returns, opens the removal sheet, and goes straight back for an id that names no view-only account.
 * Back cancels a save that is still running.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewOnlyEditAccountVMTest {
    private val navigationRouter = mockk<NavigationRouter>(relaxed = true)

    private val accountNameProvider = mockk<AccountNameProvider>(relaxed = true)

    private val accounts =
        MutableStateFlow<List<WalletAccount>?>(
            listOf(ViewOnlyAccountFixtures.zashi(isSelected = true), ViewOnlyAccountFixtures.viewOnly())
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theFieldStartsWithTheAccountsNameAndSaveIsDisabled() =
        runTest {
            val vm = vm()
            collect(vm)

            val state = assertNotNull(vm.state.value)
            assertEquals("Zodl Vault", state.field.value.text())
            assertEquals(listOf(10, ViewOnlyNameState.MAX_NAME_LENGTH), state.counter.args())
            assertFalse(state.saveButton.isEnabled)
            assertTrue(state.removeButton.isEnabled)
        }

    @Test
    fun anAccountShowingTheDefaultNameStartsWithAnEmptyField() =
        runTest {
            accounts.value = listOf(ViewOnlyAccountFixtures.viewOnly(name = stringRes(DEFAULT_NAME_RES)))
            val vm = vm()
            collect(vm)

            assertEquals("", assertNotNull(vm.state.value).field.value.text())
        }

    @Test
    fun aLongerNameIsCutToTwelveCharacters() =
        runTest {
            val vm = vm()
            collect(vm)

            type(vm, "Zodl Savings 2026")

            val state = assertNotNull(vm.state.value)
            assertEquals("Zodl Savings", state.field.value.text())
            assertEquals(listOf(12, 12), state.counter.args())
        }

    @Test
    fun emojiCountOnceAndAreNeverCutInHalf() =
        runTest {
            val vm = vm()
            collect(vm)

            type(vm, LOCK.repeat(13))

            val state = assertNotNull(vm.state.value)
            assertEquals(LOCK.repeat(12), state.field.value.text())
            assertEquals(listOf(12, 12), state.counter.args())
        }

    @Test
    fun saveNeedsANewNameThatIsNotBlank() =
        runTest {
            val vm = vm()
            collect(vm)

            type(vm, "   ")
            assertFalse(assertNotNull(vm.state.value).saveButton.isEnabled)

            type(vm, " Zodl Vault ")
            assertFalse(assertNotNull(vm.state.value).saveButton.isEnabled)

            type(vm, "Savings")
            assertTrue(assertNotNull(vm.state.value).saveButton.isEnabled)
        }

    @Test
    fun saveStoresTheTrimmedNameAndReturns() =
        runTest {
            val vm = vm()
            collect(vm)
            type(vm, " Savings ")

            assertNotNull(vm.state.value).saveButton.onClick()
            runCurrent()

            coVerify(exactly = 1) { accountNameProvider.save(ViewOnlyAccountFixtures.viewOnlyUuid, "Savings") }
            verify(exactly = 1) { navigationRouter.back() }
        }

    @Test
    fun theSavedNameReachesTheScreenThroughTheAccounts() =
        runTest {
            val vm = vm()
            collect(vm)
            type(vm, "Savings")

            accounts.value = listOf(ViewOnlyAccountFixtures.viewOnly(name = stringRes("Savings")))
            runCurrent()

            assertFalse(assertNotNull(vm.state.value).saveButton.isEnabled)
        }

    @Test
    fun aDoubleTapSavesOnce() =
        runTest {
            val saved = CompletableDeferred<Unit>()
            coEvery { accountNameProvider.save(any(), any()) } coAnswers { saved.await() }
            val vm = vm()
            collect(vm)
            type(vm, "Savings")

            val save = assertNotNull(vm.state.value).saveButton
            save.onClick()
            runCurrent()
            save.onClick()
            runCurrent()
            assertFalse(assertNotNull(vm.state.value).saveButton.isEnabled)
            saved.complete(Unit)
            runCurrent()

            coVerify(exactly = 1) { accountNameProvider.save(any(), any()) }
            verify(exactly = 1) { navigationRouter.back() }
        }

    @Test
    fun backCancelsARunningSaveSoItNeverNavigatesAgain() =
        runTest {
            coEvery { accountNameProvider.save(any(), any()) } coAnswers { awaitCancellation() }
            val vm = vm()
            collect(vm)
            type(vm, "Savings")
            assertNotNull(vm.state.value).saveButton.onClick()
            runCurrent()

            assertNotNull(vm.state.value).onBack()
            runCurrent()

            verify(exactly = 1) { navigationRouter.back() }
        }

    @Test
    fun removeOpensTheConfirmationSheetForThisAccount() =
        runTest {
            val vm = vm()
            collect(vm)

            assertNotNull(vm.state.value).removeButton.onClick()

            verify(exactly = 1) {
                navigationRouter.forward(ViewOnlyRemoveAccountArgs(ViewOnlyAccountFixtures.viewOnlyKeyId))
            }
        }

    @Test
    fun aMissingAccountGoesBack() =
        runTest {
            accounts.value = listOf(ViewOnlyAccountFixtures.zashi())

            val vm = vm()
            collect(vm)

            verify(exactly = 1) { navigationRouter.back() }
            assertNull(vm.state.value)
        }

    @Test
    fun theZodlAccountIsNeverEdited() =
        runTest {
            val vm = vm(accountKeyId = ViewOnlyAccountFixtures.zashiKeyId)
            collect(vm)

            verify(exactly = 1) { navigationRouter.back() }
            assertNull(vm.state.value)
        }

    private fun TestScope.type(
        vm: ViewOnlyEditAccountVM,
        text: String,
    ) {
        assertNotNull(vm.state.value).field.onValueChange(text)
        runCurrent()
    }

    private fun vm(accountKeyId: String = ViewOnlyAccountFixtures.viewOnlyKeyId) =
        ViewOnlyEditAccountVM(
            args = ViewOnlyEditAccountArgs(accountKeyId),
            getViewOnlyAccount = GetViewOnlyAccountUseCase(ViewOnlyAccountFixtures.getWalletAccounts(accounts)),
            renameViewOnlyAccount = RenameViewOnlyAccountUseCase(accountNameProvider),
            navigationRouter = navigationRouter,
        )

    private fun TestScope.collect(vm: ViewOnlyEditAccountVM) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        runCurrent()
    }

    private fun StringResource.text() = (this as StringResource.ByString).value

    private fun StringResource.args() = (this as StringResource.ByResource).args.toList()

    private companion object {
        const val DEFAULT_NAME_RES = 1
    }
}

private const val LOCK = "\uD83D\uDD10"
