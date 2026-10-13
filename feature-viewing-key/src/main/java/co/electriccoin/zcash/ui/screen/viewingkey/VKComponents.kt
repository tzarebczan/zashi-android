package co.electriccoin.zcash.ui.screen.viewingkey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.component.ZashiSmallTopAppBar
import co.electriccoin.zcash.ui.design.component.ZashiTextField
import co.electriccoin.zcash.ui.design.component.ZashiTextFieldPlaceholder
import co.electriccoin.zcash.ui.design.component.ZashiTopAppBarBackNavigation
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZashiColors
import co.electriccoin.zcash.ui.design.theme.typography.ZashiTypography
import co.electriccoin.zcash.ui.design.util.Compose
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.getValue

/**
 * The top app bar every full screen of the feature shares: back, the screen's title and, in the
 * import flow, its subtitle.
 */
@Composable
internal fun VKTopAppBar(
    title: StringResource,
    subtitle: StringResource?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ZashiSmallTopAppBar(
        modifier = modifier,
        title = title.getValue(),
        subtitle = subtitle?.getValue(),
        navigationAction = { ZashiTopAppBarBackNavigation(onBack = onBack) },
        colors = ZcashTheme.colors.topAppBarColors.copyColors(containerColor = Color.Transparent),
        subtitleAllCaps = false,
    )
}

/**
 * The account avatar over a heading and a message, which opens every screen of the import flow.
 * The heading is Text XL unless [headingStyle] says otherwise.
 */
@Composable
internal fun VKHeader(
    avatar: ImageResource.ByDrawable,
    heading: StringResource,
    message: StringResource,
    modifier: Modifier = Modifier,
    headingStyle: TextStyle = ZashiTypography.textXl,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        avatar.Compose(modifier = Modifier.size(AVATAR_SIZE))
        Spacer(Modifier.height(20.dp))
        Text(
            text = heading.getValue(),
            style = headingStyle,
            fontWeight = FontWeight.SemiBold,
            color = ZashiColors.Text.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message.getValue(),
            style = ZashiTypography.textSm,
            color = ZashiColors.Text.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The account name field the import's name screen and the edit screen share: its label, the field
 * capped by the caller, and the live character counter under it. The keyboard's Done key runs
 * [onDone].
 */
@Composable
internal fun NameVKField(
    label: StringResource,
    field: TextFieldState,
    placeholder: StringResource,
    counter: StringResource,
    onDone: () -> Unit,
    fieldTestTag: String,
    counterTestTag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.getValue(),
            style = ZashiTypography.textSm,
            fontWeight = FontWeight.Medium,
            color = ZashiColors.Inputs.Default.label
        )
        Spacer(Modifier.height(6.dp))
        ZashiTextField(
            state = field,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(fieldTestTag),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            placeholder = { ZashiTextFieldPlaceholder(placeholder) },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = counter.getValue(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(counterTestTag),
            style = ZashiTypography.textSm,
            color = ZashiColors.Text.textTertiary,
            textAlign = TextAlign.End,
        )
    }
}

private val AVATAR_SIZE = 90.dp
