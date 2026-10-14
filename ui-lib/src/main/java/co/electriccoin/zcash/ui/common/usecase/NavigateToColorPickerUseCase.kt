package co.electriccoin.zcash.ui.common.usecase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.screen.colorpicker.ColorPickerArgs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription

/**
 * Opens the colour picker sheet seeded with [invoke]'s initial colour and suspends until the sheet
 * answers: the picked colour, alpha included, or null when it was dismissed. The pipeline is
 * subscribed before the sheet is forwarded to, so an answer can never be emitted with nobody
 * listening, and only the answer carrying this call's request id resolves it.
 */
class NavigateToColorPickerUseCase(
    private val navigationRouter: NavigationRouter
) {
    private val pipeline = MutableSharedFlow<ColorPickerPipelineResult>()

    suspend operator fun invoke(initial: Color?): Color? {
        val args = ColorPickerArgs(initialArgb = initial?.toArgb())
        val result =
            pipeline
                .onSubscription { navigationRouter.forward(args) }
                .first { it.args.requestId == args.requestId }
        return when (result) {
            is ColorPickerPipelineResult.Cancelled -> null
            is ColorPickerPipelineResult.Selected -> result.color
        }
    }

    suspend fun onCancelled(args: ColorPickerArgs) {
        pipeline.emit(ColorPickerPipelineResult.Cancelled(args))
        navigationRouter.back()
    }

    suspend fun onSelected(
        color: Color,
        args: ColorPickerArgs
    ) {
        pipeline.emit(ColorPickerPipelineResult.Selected(color = color, args = args))
        navigationRouter.back()
    }
}

private sealed interface ColorPickerPipelineResult {
    val args: ColorPickerArgs

    data class Cancelled(
        override val args: ColorPickerArgs
    ) : ColorPickerPipelineResult

    data class Selected(
        val color: Color,
        override val args: ColorPickerArgs
    ) : ColorPickerPipelineResult
}
