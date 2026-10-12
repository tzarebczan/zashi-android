package co.electriccoin.zcash.ui.design.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.R
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZashiColors
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.pressMorph

/**
 * Figma's compact bordered icon button: a 32 dp square with radius 8 and a 1 dp stroke on the
 * screen background, a 16 dp icon in its centre. Made for tight slots such as a 40 dp text field's
 * trailing icon.
 *
 * It takes 32 dp in the layout, so the slot keeps its size, while its touch target is a 48 dp
 * square centred on it. The touch target overflows the reported size, and every point of it
 * reaches the button as long as no ancestor clips it away. The press-morph scales the visible
 * square, and it, the ripple and the click share one interaction source, as in [ZashiIconButton].
 * [IconButtonState.contentDescription] names the button for accessibility services.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZashiBorderedIconButton(
    state: IconButtonState,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints())
                    val size = BORDERED_ICON_BUTTON_SIZE.roundToPx()
                    layout(size, size) {
                        placeable.place((size - placeable.width) / 2, (size - placeable.height) / 2)
                    }
                }.size(BORDERED_ICON_BUTTON_TOUCH_TARGET)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = state.isEnabled,
                    role = Role.Button,
                    onClick = {
                        state.hapticFeedbackType?.let { runCatching { haptic.performHapticFeedback(it) } }
                        state.onClick()
                    },
                    onDoubleClick =
                        state.onDoubleClick?.let { onDoubleClick ->
                            {
                                state.hapticFeedbackType?.let { runCatching { haptic.performHapticFeedback(it) } }
                                onDoubleClick()
                            }
                        },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .pressMorph(interactionSource)
                    .size(BORDERED_ICON_BUTTON_SIZE),
            shape = RoundedCornerShape(8.dp),
            color = ZashiColors.Surfaces.bgPrimary,
            border = BorderStroke(1.dp, ZashiColors.Surfaces.strokeSecondary),
        ) {
            Box(
                modifier = Modifier.indication(interactionSource, ripple()),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(state.icon),
                    contentDescription = state.contentDescription?.getValue(),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private val BORDERED_ICON_BUTTON_SIZE = 32.dp

private val BORDERED_ICON_BUTTON_TOUCH_TARGET = 48.dp

@PreviewScreens
@Composable
private fun ZashiBorderedIconButtonPreview() =
    ZcashTheme {
        BlankSurface {
            ZashiBorderedIconButton(
                state = IconButtonState(icon = R.drawable.ic_item_keystone, onClick = {}),
            )
        }
    }
