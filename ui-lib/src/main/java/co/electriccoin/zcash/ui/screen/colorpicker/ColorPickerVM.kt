package co.electriccoin.zcash.ui.screen.colorpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.usecase.NavigateToColorPickerUseCase
import co.electriccoin.zcash.ui.design.component.colorpicker.ColorChannel
import co.electriccoin.zcash.ui.design.component.colorpicker.filterHexInput
import co.electriccoin.zcash.ui.design.component.colorpicker.parseChannelInput
import co.electriccoin.zcash.ui.design.component.colorpicker.parseHexColor
import co.electriccoin.zcash.ui.design.component.colorpicker.spectrumColor
import co.electriccoin.zcash.ui.design.component.colorpicker.spectrumPosition
import co.electriccoin.zcash.ui.design.component.colorpicker.withChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Picks a colour for [ColorPickerArgs] and answers through [NavigateToColorPickerUseCase]: the
 * toolbar's trailing button returns the picked colour with its opacity, any dismissal returns
 * nothing. Only the first answer counts, because the sheet also reports its own hiding as a
 * dismissal once it has gone.
 */
@Suppress("TooManyFunctions")
class ColorPickerVM(
    private val args: ColorPickerArgs,
    private val navigateToColorPicker: NavigateToColorPickerUseCase,
) : ViewModel() {
    private val selection = MutableStateFlow(ColorPickerSelection.initial(args.initialArgb?.let { Color(it) }))

    private var answerJob: Job? = null

    val state: StateFlow<ColorPickerState> =
        selection.map(::createState).stateIn(viewModel = this, initialValue = createState(selection.value))

    private fun createState(selection: ColorPickerSelection) =
        ColorPickerState.create(
            selection = selection,
            onTabSelect = ::onTabSelect,
            onColorSelect = ::onColorSelect,
            onSpectrumPick = ::onSpectrumPick,
            onChannelChange = ::onChannelChange,
            onFieldChange = ::onFieldChange,
            onOpacityChange = ::onOpacityChange,
            onSwatchSelect = ::onSwatchSelect,
            onDone = ::onDone,
            onBack = ::onBack,
        )

    private fun onTabSelect(tab: ColorPickerTab) = selection.update { it.copy(tab = tab) }

    private fun onColorSelect(color: Color) = pick(color)

    private fun onSpectrumPick(position: Offset) = pick(spectrumColor(position), spectrumPosition = position)

    private fun onChannelChange(
        channel: ColorChannel,
        value: Float
    ) = pick(selection.value.color.withChannel(channel, (value * MAX_CHANNEL).roundToInt()))

    private fun onFieldChange(
        field: ColorPickerField,
        text: String
    ) = selection.update { current ->
        val channel = field.channel
        val draft = if (channel == null) filterHexInput(text) else text.filter(Char::isDigit).take(CHANNEL_DIGITS)
        val parsed =
            if (channel == null) {
                parseHexColor(draft)
            } else {
                parseChannelInput(draft)?.let { current.color.withChannel(channel, it) }
            }
        current.copy(
            color = parsed ?: current.color,
            spectrumPosition = parsed?.spectrumPosition() ?: current.spectrumPosition,
            drafts = mapOf(field to draft)
        )
    }

    private fun onOpacityChange(alpha: Float) = selection.update { it.copy(alpha = alpha.coerceIn(0f, 1f)) }

    private fun onSwatchSelect(color: Color) = pick(color, alpha = color.alpha)

    private fun onDone() = answer { navigateToColorPicker.onSelected(selection.value.picked, args) }

    private fun onBack() = answer { navigateToColorPicker.onCancelled(args) }

    /** Picks the opaque part of [color], which clears every half-typed field. */
    private fun pick(
        color: Color,
        alpha: Float = selection.value.alpha,
        spectrumPosition: Offset = color.spectrumPosition(),
    ) = selection.update {
        it.copy(
            color = color.copy(alpha = 1f),
            alpha = alpha,
            spectrumPosition = spectrumPosition,
            drafts = emptyMap()
        )
    }

    private fun answer(block: suspend () -> Unit) {
        if (answerJob != null) return
        answerJob = viewModelScope.launch { block() }
    }

    private companion object {
        const val MAX_CHANNEL = 255
        const val CHANNEL_DIGITS = 3
    }
}
