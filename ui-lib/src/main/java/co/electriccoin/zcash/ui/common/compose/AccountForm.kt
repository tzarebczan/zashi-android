package co.electriccoin.zcash.ui.common.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import co.electriccoin.zcash.ui.design.component.AccountAvatarState
import co.electriccoin.zcash.ui.design.component.BlankBgScaffold
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.component.ZashiAccountAvatar
import co.electriccoin.zcash.ui.design.component.ZashiSmallTopAppBar
import co.electriccoin.zcash.ui.design.component.ZashiTextField
import co.electriccoin.zcash.ui.design.component.ZashiTextFieldPlaceholder
import co.electriccoin.zcash.ui.design.component.ZashiTopAppBarBackNavigation
import co.electriccoin.zcash.ui.design.component.rememberZashiFrostState
import co.electriccoin.zcash.ui.design.component.zashiFrostSource
import co.electriccoin.zcash.ui.design.component.zashiFrostedHeader
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZashiColors
import co.electriccoin.zcash.ui.design.theme.typography.ZashiTypography
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.scaffoldPadding

/**
 * The full-screen layout of the account forms (the view-only import and naming screens, the Edit
 * Account screen): a frosted top app bar with back, the [title] and an optional [subtitle], over a
 * scrolling column that opens with [AccountFormHeader] and continues with [content].
 */
@Composable
fun AccountFormScreen(
    title: StringResource,
    subtitle: StringResource?,
    onBack: () -> Unit,
    avatar: AccountAvatarState,
    heading: StringResource,
    message: StringResource,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hazeState = rememberZashiFrostState()
    BlankBgScaffold(
        topBar = {
            ZashiSmallTopAppBar(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .zashiFrostedHeader(hazeState),
                title = title.getValue(),
                subtitle = subtitle?.getValue(),
                navigationAction = { ZashiTopAppBarBackNavigation(onBack = onBack) },
                colors = ZcashTheme.colors.topAppBarColors.copyColors(containerColor = Color.Transparent),
                subtitleAllCaps = false,
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
                Spacer(Modifier.height(4.dp))
                AccountFormHeader(avatar = avatar, heading = heading, message = message)
                Spacer(Modifier.height(32.dp))
                content()
            }
        }
    }
}

/**
 * The account avatar over a heading and a message. The avatar carries the pencil badge when
 * [avatar] has an edit button. The heading is Text XL unless [headingStyle] says otherwise.
 */
@Composable
fun AccountFormHeader(
    avatar: AccountAvatarState,
    heading: StringResource,
    message: StringResource,
    modifier: Modifier = Modifier,
    headingStyle: TextStyle = ZashiTypography.textXl,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ZashiAccountAvatar(state = avatar, size = AVATAR_SIZE)
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
 * The account name field: its label, the field capped by the caller, and the live character
 * [counter] under it when there is one. The keyboard's Done key runs [onDone].
 */
@Composable
fun AccountNameField(
    label: StringResource,
    field: TextFieldState,
    placeholder: StringResource,
    counter: StringResource?,
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
        counter?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                text = it.getValue(),
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
}

private val AVATAR_SIZE = 90.dp
