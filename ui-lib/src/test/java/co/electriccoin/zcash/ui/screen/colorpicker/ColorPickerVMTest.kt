package co.electriccoin.zcash.ui.screen.colorpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.usecase.NavigateToColorPickerUseCase
import co.electriccoin.zcash.ui.design.component.colorpicker.ColorPickerPalette
import co.electriccoin.zcash.ui.design.component.colorpicker.spectrumColor
import co.electriccoin.zcash.ui.design.util.StringResource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * [ColorPickerVM] starts on the colour in its args, switches tabs, picks from the grid, the
 * spectrum, the sliders, the fields and the swatches while keeping the opacity apart, and answers
 * once: Done with the picked colour, any dismissal with nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ColorPickerVMTest {
    private val forwarded = mutableListOf<ColorPickerArgs>()

    private val router =
        mockk<NavigationRouter>(relaxed = true) {
            every { forward(*anyVararg()) } answers {
                (args[0] as Array<*>).filterIsInstance<ColorPickerArgs>().forEach { forwarded.add(it) }
            }
        }

    private val navigateToColorPicker = NavigateToColorPickerUseCase(router)

    /** Collects every view model's state, which is shared only while something subscribes. */
    private lateinit var collectors: CoroutineScope

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        collectors = CoroutineScope(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        collectors.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun startsOnTheInitialColorAndItsOpacityInTheGrid() {
        val vm = vm(initial = Color(HALF_PURPLE))

        val state = vm.state.value
        assertEquals(Color(HALF_PURPLE), state.picked)
        assertEquals(PURPLE_OPAQUE, assertIs<ColorPickerContentState.Grid>(state.content).selected.toArgb())
        assertEquals(listOf(true, false, false), state.tabs.map { it.isSelected })
        assertEquals(listOf(HALF_PERCENT), state.opacityText.args())
    }

    @Test
    fun startsOnOpaqueWhiteWithoutAColor() {
        assertEquals(Color.White, vm(initial = null).state.value.picked)
    }

    @Test
    fun tabsSwitchTheContent() {
        val vm = vm()

        vm.state.value.tabs[1]
            .onClick()
        assertIs<ColorPickerContentState.Spectrum>(vm.state.value.content)

        vm.state.value.tabs[2]
            .onClick()
        assertIs<ColorPickerContentState.Sliders>(vm.state.value.content)
        assertEquals(
            listOf(false, false, true),
            vm.state.value.tabs
                .map { it.isSelected }
        )
    }

    @Test
    fun aGridColorKeepsTheOpacity() {
        val vm = vm(initial = Color(HALF_PURPLE))
        val green = ColorPickerPalette.grid.last()

        assertIs<ColorPickerContentState.Grid>(vm.state.value.content).onSelect(green)

        assertEquals(green.copy(alpha = Color(HALF_PURPLE).alpha), vm.state.value.picked)
    }

    @Test
    fun theSpectrumPicksTheColorUnderTheTouchAndRingsIt() {
        val vm = vm()
        vm.state.value.tabs[1]
            .onClick()
        val touch = Offset(0.25f, 0.75f)

        assertIs<ColorPickerContentState.Spectrum>(vm.state.value.content).onPick(touch)

        val spectrum = assertIs<ColorPickerContentState.Spectrum>(vm.state.value.content)
        assertEquals(touch, spectrum.position)
        assertEquals(spectrumColor(touch), vm.state.value.picked)
    }

    @Test
    fun aSliderSetsItsChannel() {
        val vm = vm(initial = Color.Black)
        vm.state.value.tabs[2]
            .onClick()

        sliders(vm).channels[0].onValueChange(1f)

        assertEquals(Color.Red, vm.state.value.picked)
        assertEquals("255", sliders(vm).channels[0].text)
        assertEquals("FF0000", sliders(vm).hex)
    }

    @Test
    fun aTypedChannelAppliesOnceItIsAValueAndKeepsTheDraft() {
        val vm = vm(initial = Color.Black)
        vm.state.value.tabs[2]
            .onClick()

        sliders(vm).channels[1].onTextChange("3x00")
        assertEquals("300", sliders(vm).channels[1].text)
        assertEquals(Color.Black, vm.state.value.picked)

        sliders(vm).channels[1].onTextChange("128")
        assertEquals("128", sliders(vm).channels[1].text)
        assertEquals(
            PARTIAL_GREEN,
            vm.state.value.picked
                .toArgb()
        )
        assertEquals("008000", sliders(vm).hex)
    }

    @Test
    fun aTypedHexAppliesAtSixDigits() {
        val vm = vm(initial = Color.Black)
        vm.state.value.tabs[2]
            .onClick()

        sliders(vm).onHexChange("#be3")
        assertEquals("BE3", sliders(vm).hex)
        assertEquals(Color.Black, vm.state.value.picked)

        sliders(vm).onHexChange("be38f3")
        assertEquals(
            PURPLE_OPAQUE,
            vm.state.value.picked
                .toArgb()
        )
        assertEquals("190", sliders(vm).channels[0].text)
    }

    @Test
    fun opacityChangesOnlyTheAlpha() {
        val vm = vm(initial = Color(PURPLE_OPAQUE))

        vm.state.value.onOpacityChange(0.5f)

        assertEquals(Color(PURPLE_OPAQUE).copy(alpha = 0.5f), vm.state.value.picked)
        assertEquals(
            listOf(HALF_PERCENT),
            vm.state.value.opacityText
                .args()
        )
    }

    @Test
    fun aSwatchIsSelectedWhenItIsThePickedColor() {
        val vm = vm()
        val swatch = vm.state.value.swatches[6]

        swatch.onClick()

        assertEquals(swatch.color, vm.state.value.picked)
        assertEquals(
            listOf(6),
            vm.state.value.swatches.indices
                .filter {
                    vm.state.value.swatches[it]
                        .isSelected
                }
        )
    }

    @Test
    fun doneAnswersWithThePickedColorOnce() =
        runTest(UnconfinedTestDispatcher()) {
            val result = async { navigateToColorPicker(Color(HALF_PURPLE)) }
            val vm = ColorPickerVM(args = forwarded.single(), navigateToColorPicker = navigateToColorPicker)

            vm.state.value.doneButton
                .onClick()
            vm.state.value.onBack()

            assertEquals(Color(HALF_PURPLE), result.await())
            verify(exactly = 1) { router.back() }
        }

    @Test
    fun backAnswersWithNothing() =
        runTest(UnconfinedTestDispatcher()) {
            val result = async { navigateToColorPicker(Color(HALF_PURPLE)) }
            val vm = ColorPickerVM(args = forwarded.single(), navigateToColorPicker = navigateToColorPicker)

            vm.state.value.onBack()
            vm.state.value.doneButton
                .onClick()

            assertNull(result.await())
            verify(exactly = 1) { router.back() }
        }

    private fun vm(initial: Color? = null) =
        ColorPickerVM(
            args = ColorPickerArgs(initialArgb = initial?.toArgb()),
            navigateToColorPicker = navigateToColorPicker
        ).also { vm -> collectors.launch { vm.state.collect { } } }

    private fun sliders(vm: ColorPickerVM) = assertIs<ColorPickerContentState.Sliders>(vm.state.value.content)

    private fun StringResource.args() = (this as StringResource.ByResource).args.toList()

    private companion object {
        const val HALF_PURPLE = 0x80BE38F3
        const val PURPLE_OPAQUE = 0xFFBE38F3.toInt()
        const val PARTIAL_GREEN = 0xFF008000.toInt()
        const val HALF_PERCENT = 50
    }
}
