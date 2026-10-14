package co.electriccoin.zcash.ui.screen.viewingkey

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.AccountNameLimit
import co.electriccoin.zcash.ui.common.repository.PendingViewOnlyImport
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepositoryImpl
import co.electriccoin.zcash.ui.common.usecase.NavigateToColorPickerUseCase
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.screen.colorpicker.ColorPickerArgs
import co.electriccoin.zcash.ui.screen.connecthw.HWWalletEnrollment
import co.electriccoin.zcash.ui.screen.connecthw.date.HWDateArgs
import co.electriccoin.zcash.ui.screen.connecthw.neworactive.HWNewOrActiveArgs
import co.electriccoin.zcash.ui.screen.viewingkey.name.NameVKState
import co.electriccoin.zcash.ui.screen.viewingkey.name.NameVKVM
import io.mockk.every
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The name is capped at twelve characters as it is typed, the counter follows it, Continue needs a
 * name that is not blank and stores it trimmed before going straight to the birthday date, without
 * the new-or-active question, and back forgets the pending key. The avatar's pencil badge opens the
 * colour picker; the avatar follows the picks live and keeps the last one however the picker closes,
 * and Continue keeps it with the pending import.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NameVKVMTest {
    private val navigationRouter = mockk<NavigationRouter>(relaxed = true)

    private val pickerRequests = mutableListOf<ColorPickerArgs>()

    private val navigateToColorPicker =
        NavigateToColorPickerUseCase(
            mockk(relaxed = true) {
                every { forward(*anyVararg()) } answers {
                    (args[0] as Array<*>).filterIsInstance<ColorPickerArgs>().forEach { pickerRequests.add(it) }
                }
            }
        )

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
                listOf(0, AccountNameLimit.MAX_LENGTH),
                vm.state.value.counter
                    .args()
            )

            type(vm, "Zodl Vault")

            assertEquals(
                listOf(10, AccountNameLimit.MAX_LENGTH),
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
    fun continueStoresTheTrimmedNameAndGoesStraightToTheBirthdayDate() =
        runTest {
            val vm = vm()
            collect(vm)
            type(vm, " Vault ")

            vm.state.value.continueButton
                .onClick()

            assertEquals(PendingViewOnlyImport(ufvk = UFVK, name = "Vault"), repository.get())
            verify(exactly = 1) { navigationRouter.forward(HWDateArgs(HWWalletEnrollment.ViewOnly)) }
            verify(exactly = 0) { navigationRouter.forward(HWNewOrActiveArgs(HWWalletEnrollment.ViewOnly)) }
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

    @Test
    fun aPickedColorShowsOnTheAvatarAndContinueKeepsItWithThePendingImport() =
        runTest {
            val vm = vm()
            collect(vm)
            assertNull(vm.state.value.avatarColor())

            openPicker(vm)
            val request = pickerRequests.single()
            assertNull(request.initialArgb)
            navigateToColorPicker.onPicked(Color(PICKED), request)
            navigateToColorPicker.onClosed(request)
            runCurrent()

            assertEquals(Color(PICKED), vm.state.value.avatarColor())
            assertEquals(PendingViewOnlyImport(ufvk = UFVK, name = ""), repository.get())

            type(vm, "Savings")
            vm.state.value.continueButton
                .onClick()

            assertEquals(PendingViewOnlyImport(ufvk = UFVK, name = "Savings", color = Color(PICKED)), repository.get())
        }

    @Test
    fun theColorOfThePendingImportShowsWhenTheScreenOpensAgain() =
        runTest {
            repository.set(PendingViewOnlyImport(ufvk = UFVK, name = "Savings", color = Color(PICKED)))
            val vm = vm()
            collect(vm)

            assertEquals(Color(PICKED), vm.state.value.avatarColor())
        }

    @Test
    fun aPickerClosedWithoutAPickKeepsTheColorAndReopensWithIt() =
        runTest {
            val vm = vm()
            collect(vm)
            openPicker(vm)
            navigateToColorPicker.onPicked(Color(PICKED), pickerRequests.last())
            navigateToColorPicker.onClosed(pickerRequests.last())
            runCurrent()

            openPicker(vm)
            val second = pickerRequests.last()
            assertEquals(Color(PICKED), Color(assertNotNull(second.initialArgb)))
            navigateToColorPicker.onClosed(second)
            runCurrent()

            assertEquals(Color(PICKED), vm.state.value.avatarColor())
        }

    @Test
    fun theAvatarFollowsLivePicksAndKeepsTheLastOneWhenThePickerCloses() =
        runTest {
            val vm = vm()
            collect(vm)
            openPicker(vm)
            val request = pickerRequests.single()

            navigateToColorPicker.onPicked(Color(OTHER), request)
            runCurrent()
            assertEquals(Color(OTHER), vm.state.value.avatarColor())

            navigateToColorPicker.onPicked(Color(PICKED), request)
            navigateToColorPicker.onClosed(request)
            runCurrent()
            assertEquals(Color(PICKED), vm.state.value.avatarColor())

            openPicker(vm)
            assertEquals(2, pickerRequests.size)
        }

    @Test
    fun aSecondTapWhileThePickerIsOpenOpensNothing() =
        runTest {
            val vm = vm()
            collect(vm)

            openPicker(vm)
            openPicker(vm)

            assertEquals(1, pickerRequests.size)
        }

    private fun NameVKState.avatarColor() =
        (avatar.image as ImageResource.Layered).background.takeIf { it.isSpecified }

    private fun TestScope.openPicker(vm: NameVKVM) {
        assertNotNull(vm.state.value.avatar.editButton).onClick()
        runCurrent()
    }

    private fun TestScope.type(
        vm: NameVKVM,
        text: String,
    ) {
        vm.state.value.field
            .onValueChange(text)
        runCurrent()
    }

    private fun vm() =
        NameVKVM(
            pendingImportRepository = repository,
            navigationRouter = navigationRouter,
            navigateToColorPicker = navigateToColorPicker
        )

    private fun TestScope.collect(vm: NameVKVM) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        runCurrent()
    }

    private fun StringResource.text() = (this as StringResource.ByString).value

    private fun StringResource.args() = (this as StringResource.ByResource).args.toList()

    private companion object {
        const val UFVK = "uview1namefixture"
        const val PICKED = 0x80BE38F3
        const val OTHER = 0xFF0088FF
    }
}

private const val LOCK = "\uD83D\uDD10"
