package co.electriccoin.zcash.ui.screen.viewingkey.name

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountNameLimit
import co.electriccoin.zcash.ui.design.component.AccountAvatarState
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.viewingkey.vkAvatar
import co.electriccoin.zcash.viewingkey.R

/**
 * Names the account being imported.
 *
 * @param avatar the account avatar, whose pencil badge opens the colour picker. The picked colour is
 *        only shown, not yet stored with the account.
 * @param counter how many of the allowed characters [field] uses, counted in code points, shown
 *        under it.
 */
@Immutable
data class NameVKState(
    val title: StringResource,
    val subtitle: StringResource,
    val avatar: AccountAvatarState,
    val heading: StringResource,
    val message: StringResource,
    val fieldLabel: StringResource,
    val field: TextFieldState,
    val placeholder: StringResource,
    val counter: StringResource,
    val continueButton: ButtonState,
    val onBack: () -> Unit,
) {
    companion object {
        val preview: NameVKState = preview(name = "")

        val previewFilled: NameVKState = preview(name = "Zodl Vault")

        val previewColored: NameVKState = preview(name = "Zodl Vault", color = Color(PREVIEW_COLOR))

        val previewLimit: NameVKState = preview(name = "Zodl Savings")

        /**
         * The screen for [name] with the avatar in [color], or the default avatar without one.
         * Continue is enabled while the name is not blank.
         */
        fun create(
            name: String,
            color: Color?,
            onAvatarEditClick: () -> Unit,
            onValueChange: (String) -> Unit,
            onContinueClick: () -> Unit,
            onBack: () -> Unit,
        ) = NameVKState(
            title = stringRes(R.string.view_only_import_title),
            subtitle = stringRes(R.string.view_only_import_subtitle),
            avatar = vkAvatar(color = color, onEditClick = onAvatarEditClick),
            heading = stringRes(R.string.view_only_name_heading),
            message = stringRes(R.string.view_only_name_message),
            fieldLabel = stringRes(R.string.view_only_name_label),
            field = TextFieldState(value = stringRes(name), onValueChange = onValueChange),
            placeholder = stringRes(R.string.view_only_name_placeholder),
            counter =
                stringRes(
                    R.string.view_only_name_counter,
                    ViewOnlyAccountNameLimit.lengthOf(name),
                    ViewOnlyAccountNameLimit.MAX_LENGTH
                ),
            continueButton =
                ButtonState(
                    text = stringRes(R.string.view_only_continue),
                    style = ButtonStyle.PRIMARY,
                    isEnabled = name.isNotBlank(),
                    onClick = onContinueClick,
                ),
            onBack = onBack,
        )

        private fun preview(
            name: String,
            color: Color? = null
        ) = create(
            name = name,
            color = color,
            onAvatarEditClick = {},
            onValueChange = {},
            onContinueClick = {},
            onBack = {},
        )

        private const val PREVIEW_COLOR = 0xFFBE38F3
    }
}

object NameVKTag {
    const val FIELD = "view_only_name_field"
    const val COUNTER = "view_only_name_counter"
    const val CONTINUE_BUTTON = "view_only_name_continue_button"
}
