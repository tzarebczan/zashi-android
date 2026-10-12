package co.electriccoin.zcash.ui.screen.viewonly.paste

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.common.compose.WithoutPersonalizedLearning
import co.electriccoin.zcash.ui.design.component.BlankBgScaffold
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.component.ZashiButton
import co.electriccoin.zcash.ui.design.component.ZashiConfirmationBottomSheet
import co.electriccoin.zcash.ui.design.component.ZashiTextField
import co.electriccoin.zcash.ui.design.component.ZashiTextFieldDefaults
import co.electriccoin.zcash.ui.design.component.ZashiTextFieldPlaceholder
import co.electriccoin.zcash.ui.design.component.rememberZashiFrostState
import co.electriccoin.zcash.ui.design.component.zashiFrostSource
import co.electriccoin.zcash.ui.design.component.zashiFrostedHeader
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZashiColors
import co.electriccoin.zcash.ui.design.theme.typography.ZashiTypography
import co.electriccoin.zcash.ui.design.util.Compose
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.pressMorph
import co.electriccoin.zcash.ui.design.util.scaffoldPadding
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyHeader
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyTopAppBar

@Composable
fun ViewOnlyPasteView(state: ViewOnlyPasteState) {
    val hazeState = rememberZashiFrostState()
    BlankBgScaffold(
        topBar = {
            ViewOnlyTopAppBar(
                title = state.title,
                subtitle = state.subtitle,
                onBack = state.onBack,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .zashiFrostedHeader(hazeState),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .zashiFrostSource(hazeState)
        ) {
            Content(
                state = state,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .scaffoldPadding(paddingValues)
            )
        }
    }
}

@Composable
private fun Content(
    state: ViewOnlyPasteState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(4.dp))
        ViewOnlyHeader(avatar = state.avatar, heading = state.heading, message = state.message)
        Spacer(Modifier.height(32.dp))
        KeyField(state)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        Disclaimer(state)
        Spacer(Modifier.height(24.dp))
        ZashiButton(
            state = state.continueButton,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(ViewOnlyPasteTag.CONTINUE_BUTTON)
        )
    }
}

@Composable
private fun KeyField(state: ViewOnlyPasteState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = state.fieldLabel.getValue(),
            style = ZashiTypography.textSm,
            fontWeight = FontWeight.Medium,
            color = ZashiColors.Inputs.Default.label
        )
        Spacer(Modifier.height(6.dp))
        WithoutPersonalizedLearning {
            ZashiTextField(
                state = state.field,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(ViewOnlyPasteTag.FIELD),
                innerModifier = ZashiTextFieldDefaults.innerModifier.height(KEY_FIELD_HEIGHT),
                textStyle = ZashiTypography.textSm,
                singleLine = true,
                placeholder = { ZashiTextFieldPlaceholder(state.placeholder) },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = { if (state.continueButton.isEnabled) state.continueButton.onClick() }
                    ),
                trailingIcon = { ScanButton(state.scanButton) },
                contentPadding = KEY_FIELD_CONTENT_PADDING,
            )
        }
    }
}

/**
 * Figma's 32 dp bordered scan button inside the 40 dp key field. It reports 32 dp to the field so the
 * field keeps its height, while the tap target around it is [SCAN_TOUCH_TARGET], centred on it. The
 * press-morph, the ripple and the click share one interaction source, as the design-system buttons do.
 */
@Composable
private fun ScanButton(state: IconButtonState) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val description = state.contentDescription?.getValue()
    Box(
        modifier =
            Modifier
                .padding(end = 4.dp)
                .layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints())
                    val size = SCAN_BUTTON_SIZE.roundToPx()
                    layout(size, size) {
                        placeable.place((size - placeable.width) / 2, (size - placeable.height) / 2)
                    }
                }.size(SCAN_TOUCH_TARGET)
                .testTag(ViewOnlyPasteTag.SCAN_BUTTON)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = state.isEnabled,
                    role = Role.Button,
                    onClick = {
                        state.hapticFeedbackType?.let { runCatching { haptic.performHapticFeedback(it) } }
                        state.onClick()
                    },
                ).semantics { if (description != null) contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .pressMorph(interactionSource)
                    .size(SCAN_BUTTON_SIZE),
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
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private val SCAN_BUTTON_SIZE = 32.dp

private val SCAN_TOUCH_TARGET = 48.dp

@Composable
private fun Disclaimer(state: ViewOnlyPasteState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.disclaimerIcon.Compose(modifier = Modifier.size(16.dp))
        Text(
            text = state.disclaimer.getValue(),
            modifier = Modifier.weight(1f),
            style = ZashiTypography.textXs,
            color = ZashiColors.Text.textTertiary,
            overflow = TextOverflow.Clip,
        )
    }
}

/**
 * Figma draws the key field 40 high. The trailing scan button's slot would otherwise grow it to 48, so
 * the field is fixed at that height and its vertical padding centres one line of small text in it.
 */
private val KEY_FIELD_HEIGHT = 40.dp

private val KEY_FIELD_CONTENT_PADDING = PaddingValues(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)

@PreviewScreens
@Composable
private fun EmptyPreview() =
    ZcashTheme {
        ViewOnlyPasteView(ViewOnlyPasteState.preview)
    }

@PreviewScreens
@Composable
private fun FilledPreview() =
    ZcashTheme {
        ViewOnlyPasteView(ViewOnlyPasteState.previewFilled)
    }

@PreviewScreens
@Composable
private fun InvalidKeyPreview() =
    ZcashTheme {
        ViewOnlyPasteView(ViewOnlyPasteState.previewFilled)
        ZashiConfirmationBottomSheet(ViewOnlyInvalidKeySheet.preview)
    }
