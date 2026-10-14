package co.electriccoin.zcash.ui.screen.viewingkey

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountNameLimit
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.provider.AccountNameProvider
import co.electriccoin.zcash.ui.common.repository.MetadataRepository
import co.electriccoin.zcash.ui.common.usecase.GetViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToColorPickerUseCase
import co.electriccoin.zcash.ui.common.usecase.RenameViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.ViewOnlyAccountFixtures
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.screen.colorpicker.ColorPickerArgs
import co.electriccoin.zcash.ui.screen.error.ErrorArgs
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase
import co.electriccoin.zcash.ui.screen.viewingkey.edit.EditVKAccountState
import co.electriccoin.zcash.ui.screen.viewingkey.edit.EditVKAccountVM
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
 * Back cancels a save that is still running. The avatar shows the stored colour and its pencil badge
 * opens the colour picker on it; a newly picked colour enables Save, which stores whichever of the
 * name and the colour changed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditVKAccountVMTest {
    private val navigationRouter = mockk<NavigationRouter>(relaxed = true)

    private val accountNameProvider = mockk<AccountNameProvider>(relaxed = true)

    private val navigateToError = mockk<NavigateToErrorUseCase>(relaxed = true)

    private val pickerRequests = mutableListOf<ColorPickerArgs>()

    private val navigateToColorPicker =
        NavigateToColorPickerUseCase(
            mockk(relaxed = true) {
                every { forward(*anyVararg()) } answers {
                    (args[0] as Array<*>).filterIsInstance<ColorPickerArgs>().forEach { pickerRequests.add(it) }
                }
            }
        )

    private val storedColors = MutableStateFlow<Map<AccountUuid, Color>>(emptyMap())

    private val metadataRepository =
        mockk<MetadataRepository>(relaxed = true) {
            every { observeColor(any()) } answers { storedColors.map { it[firstArg()] } }
        }

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
            assertEquals(listOf(10, ViewOnlyAccountNameLimit.MAX_LENGTH), state.counter.args())
            assertFalse(state.saveButton.isEnabled)
            assertTrue(state.removeButton.isEnabled)
        }

    @Test
    fun anAccountShowingTheDefaultNameStartsWithAnEmptyField() =
        runTest {
            accounts.value = listOf(ViewOnlyAccountFixtures.viewOnly(customName = null))
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

            accounts.value = listOf(ViewOnlyAccountFixtures.viewOnly(customName = "Savings"))
            runCurrent()

            assertFalse(assertNotNull(vm.state.value).saveButton.isEnabled)
        }

    @Test
    fun aFailedSaveShowsTheErrorAndStaysOnTheScreen() =
        runTest {
            val failure = IllegalStateException("prefs")
            coEvery { accountNameProvider.save(any(), any()) } throws failure
            val vm = vm()
            collect(vm)
            type(vm, "Savings")

            assertNotNull(vm.state.value).saveButton.onClick()
            runCurrent()

            verify(exactly = 1) { navigateToError(ErrorArgs.General(failure), any()) }
            verify(exactly = 0) { navigationRouter.back() }
            val state = assertNotNull(vm.state.value)
            assertEquals("Savings", state.field.value.text())
            assertTrue(state.saveButton.isEnabled)
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
                navigationRouter.forward(RemoveVKAccountArgs(ViewOnlyAccountFixtures.viewOnlyKeyId))
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

    @Test
    fun aPickedColorShowsOnTheAvatarEnablesSaveAndIsSavedAlone() =
        runTest {
            val vm = vm()
            collect(vm)
            assertNull(assertNotNull(vm.state.value).avatarColor())

            pick(vm, Color(PICKED))

            val state = assertNotNull(vm.state.value)
            assertEquals(Color(PICKED), state.avatarColor())
            assertTrue(state.saveButton.isEnabled)

            state.saveButton.onClick()
            runCurrent()

            val saved = slot<WalletAccount>()
            coVerify(exactly = 1) { metadataRepository.setAccountColor(capture(saved), Color(PICKED)) }
            assertEquals(ViewOnlyAccountFixtures.viewOnlyUuid, saved.captured.sdkAccount.accountUuid)
            coVerify(exactly = 0) { accountNameProvider.save(any(), any()) }
            verify(exactly = 1) { navigationRouter.back() }
        }

    @Test
    fun saveStoresBothANewNameAndANewColor() =
        runTest {
            val vm = vm()
            collect(vm)

            type(vm, "Savings")
            pick(vm, Color(PICKED))
            assertNotNull(vm.state.value).saveButton.onClick()
            runCurrent()

            coVerify(exactly = 1) { accountNameProvider.save(ViewOnlyAccountFixtures.viewOnlyUuid, "Savings") }
            coVerify(exactly = 1) { metadataRepository.setAccountColor(any(), Color(PICKED)) }
        }

    @Test
    fun theStoredColorShowsAndOpensThePicker() =
        runTest {
            storedColors.value = mapOf(ViewOnlyAccountFixtures.viewOnlyUuid to Color(STORED))
            val vm = vm()
            collect(vm)

            assertEquals(Color(STORED), assertNotNull(vm.state.value).avatarColor())
            assertNotNull(assertNotNull(vm.state.value).avatar.editButton).onClick()
            runCurrent()

            assertEquals(Color(STORED), Color(assertNotNull(pickerRequests.single().initialArgb)))
        }

    @Test
    fun pickingTheStoredColorAgainLeavesSaveDisabled() =
        runTest {
            storedColors.value = mapOf(ViewOnlyAccountFixtures.viewOnlyUuid to Color(STORED))
            val vm = vm()
            collect(vm)

            pick(vm, Color(STORED))

            assertFalse(assertNotNull(vm.state.value).saveButton.isEnabled)
        }

    @Test
    fun aDismissedPickerLeavesTheStoredAvatar() =
        runTest {
            val vm = vm()
            collect(vm)

            assertNotNull(assertNotNull(vm.state.value).avatar.editButton).onClick()
            runCurrent()
            navigateToColorPicker.onCancelled(pickerRequests.single())
            runCurrent()

            assertNull(assertNotNull(vm.state.value).avatarColor())
            assertFalse(assertNotNull(vm.state.value).saveButton.isEnabled)
        }

    private suspend fun TestScope.pick(
        vm: EditVKAccountVM,
        color: Color,
    ) {
        assertNotNull(assertNotNull(vm.state.value).avatar.editButton).onClick()
        runCurrent()
        navigateToColorPicker.onSelected(color, pickerRequests.last())
        runCurrent()
    }

    private fun EditVKAccountState.avatarColor() =
        (avatar.image as ImageResource.Layered).background.takeIf { it.isSpecified }

    private fun TestScope.type(
        vm: EditVKAccountVM,
        text: String,
    ) {
        assertNotNull(vm.state.value).field.onValueChange(text)
        runCurrent()
    }

    private fun vm(accountKeyId: String = ViewOnlyAccountFixtures.viewOnlyKeyId) =
        EditVKAccountVM(
            args = EditVKAccountArgs(accountKeyId),
            getViewOnlyAccount = GetViewOnlyAccountUseCase(ViewOnlyAccountFixtures.getWalletAccounts(accounts)),
            renameViewOnlyAccount = RenameViewOnlyAccountUseCase(accountNameProvider),
            navigateToError = navigateToError,
            navigationRouter = navigationRouter,
            navigateToColorPicker = navigateToColorPicker,
            metadataRepository = metadataRepository,
        )

    private fun TestScope.collect(vm: EditVKAccountVM) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        runCurrent()
    }

    private fun StringResource.text() = (this as StringResource.ByString).value

    private fun StringResource.args() = (this as StringResource.ByResource).args.toList()
}

private const val LOCK = "\uD83D\uDD10"
private const val PICKED = 0xFF0088FF
private const val STORED = 0x80BE38F3
