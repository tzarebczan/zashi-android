package co.electriccoin.zcash.ui.screen.viewingkey.name

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import co.electriccoin.zcash.ui.common.model.AccountNameLimit
import co.electriccoin.zcash.ui.design.component.AccountAvatarState
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.viewingkey.vkAvatar
import co.electriccoin.zcash.viewingkey.R
import co.electriccoin.zcash.ui.design.R as DesignR

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

        val previewDuplicate: NameVKState = preview(name = "Zodl", isDuplicate = true)

        /**
         * The screen for [name] with the avatar in [color], or the default avatar without one.
         * Continue is enabled while the name is not blank and not [isDuplicate], the name of another
         * account, which the field also says under it.
         */
        fun create(
            name: String,
            color: Color?,
            isDuplicate: Boolean,
            onAvatarEditClick: () -> Unit,
            onValueChange: (String) -> Unit,
            onContinueClick: () -> Unit,
            onBack: () -> Unit,
        ) = NameVKState(
            title = stringRes(R.string.view_only_import_title),
            subtitle = stringRes(R.string.view_only_import_subtitle),
            avatar = vkAvatar(color = color, onEditClick = onAvatarEditClick),
            heading = stringRes(DesignR.string.accountName_heading),
            message = stringRes(DesignR.string.accountName_message),
            fieldLabel = stringRes(DesignR.string.accountName_label),
            field =
                TextFieldState(
                    value = stringRes(name),
                    error = if (isDuplicate) stringRes(DesignR.string.accountName_duplicate) else null,
                    onValueChange = onValueChange
                ),
            placeholder = stringRes(DesignR.string.accountName_placeholder),
            counter =
                stringRes(
                    DesignR.string.accountName_counter,
                    AccountNameLimit.lengthOf(name),
                    AccountNameLimit.MAX_LENGTH
                ),
            continueButton =
                ButtonState(
                    text = stringRes(R.string.view_only_continue),
                    style = ButtonStyle.PRIMARY,
                    isEnabled = name.isNotBlank() && !isDuplicate,
                    onClick = onContinueClick,
                ),
            onBack = onBack,
        )

        private fun preview(
            name: String,
            color: Color? = null,
            isDuplicate: Boolean = false,
        ) = create(
            name = name,
            color = color,
            isDuplicate = isDuplicate,
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
