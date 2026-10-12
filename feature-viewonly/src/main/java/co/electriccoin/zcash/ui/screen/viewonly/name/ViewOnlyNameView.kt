package co.electriccoin.zcash.ui.screen.viewonly.name

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.BlankBgScaffold
import co.electriccoin.zcash.ui.design.component.ZashiButton
import co.electriccoin.zcash.ui.design.component.ZashiTextField
import co.electriccoin.zcash.ui.design.component.ZashiTextFieldPlaceholder
import co.electriccoin.zcash.ui.design.component.rememberZashiFrostState
import co.electriccoin.zcash.ui.design.component.zashiFrostSource
import co.electriccoin.zcash.ui.design.component.zashiFrostedHeader
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZashiColors
import co.electriccoin.zcash.ui.design.theme.typography.ZashiTypography
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.scaffoldPadding
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyHeader
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyTopAppBar

@Composable
fun ViewOnlyNameView(state: ViewOnlyNameState) {
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
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .scaffoldPadding(paddingValues)
            ) {
                Spacer(Modifier.height(16.dp))
                ViewOnlyHeader(avatar = state.avatar, heading = state.heading, message = state.message)
                Spacer(Modifier.height(32.dp))
                NameField(state)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(24.dp))
                ZashiButton(
                    state = state.continueButton,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(ViewOnlyNameTag.CONTINUE_BUTTON)
                )
            }
        }
    }
}

@Composable
private fun NameField(state: ViewOnlyNameState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = state.fieldLabel.getValue(),
            style = ZashiTypography.textSm,
            fontWeight = FontWeight.Medium,
            color = ZashiColors.Inputs.Default.label
        )
        Spacer(Modifier.height(6.dp))
        ZashiTextField(
            state = state.field,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(ViewOnlyNameTag.FIELD),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            placeholder = { ZashiTextFieldPlaceholder(state.placeholder) },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = { if (state.continueButton.isEnabled) state.continueButton.onClick() }
                ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.counter.getValue(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(ViewOnlyNameTag.COUNTER),
            style = ZashiTypography.textSm,
            color = ZashiColors.Text.textTertiary,
            textAlign = TextAlign.End,
        )
    }
}

@PreviewScreens
@Composable
private fun EmptyPreview() =
    ZcashTheme {
        ViewOnlyNameView(ViewOnlyNameState.preview)
    }

@PreviewScreens
@Composable
private fun FilledPreview() =
    ZcashTheme {
        ViewOnlyNameView(ViewOnlyNameState.previewFilled)
    }

@PreviewScreens
@Composable
private fun LimitPreview() =
    ZcashTheme {
        ViewOnlyNameView(ViewOnlyNameState.previewLimit)
    }
