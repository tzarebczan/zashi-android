package co.electriccoin.zcash.ui.screen.viewonly.name

import androidx.compose.runtime.Immutable
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.viewonly.R

/**
 * Names the account being imported.
 *
 * @param counter how many of the allowed characters [field] uses, shown under it.
 */
@Immutable
data class ViewOnlyNameState(
    val title: StringResource,
    val subtitle: StringResource,
    val avatar: ImageResource.ByDrawable,
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
        const val MAX_NAME_LENGTH = 12

        val preview: ViewOnlyNameState = preview(name = "")

        val previewFilled: ViewOnlyNameState = preview(name = "Zodl Vault")

        val previewLimit: ViewOnlyNameState = preview(name = "Zodl Savings")

        /**
         * The screen for [name]. Continue is enabled while the name is not blank.
         */
        fun create(
            name: String,
            onValueChange: (String) -> Unit,
            onContinueClick: () -> Unit,
            onBack: () -> Unit,
        ) = ViewOnlyNameState(
            title = stringRes(R.string.view_only_import_title),
            subtitle = stringRes(R.string.view_only_import_subtitle),
            avatar = ImageResource.ByDrawable(R.drawable.view_only_ic_avatar),
            heading = stringRes(R.string.view_only_name_heading),
            message = stringRes(R.string.view_only_name_message),
            fieldLabel = stringRes(R.string.view_only_name_label),
            field = TextFieldState(value = stringRes(name), onValueChange = onValueChange),
            placeholder = stringRes(R.string.view_only_name_placeholder),
            counter = stringRes(R.string.view_only_name_counter, name.length, MAX_NAME_LENGTH),
            continueButton =
                ButtonState(
                    text = stringRes(R.string.view_only_continue),
                    style = ButtonStyle.PRIMARY,
                    isEnabled = name.isNotBlank(),
                    onClick = onContinueClick,
                ),
            onBack = onBack,
        )

        private fun preview(name: String) =
            create(
                name = name,
                onValueChange = {},
                onContinueClick = {},
                onBack = {},
            )
    }
}

object ViewOnlyNameTag {
    const val FIELD = "view_only_name_field"
    const val COUNTER = "view_only_name_counter"
    const val CONTINUE_BUTTON = "view_only_name_continue_button"
}
