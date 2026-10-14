package co.electriccoin.zcash.ui.screen.viewingkey.edit

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountNameLimit
import co.electriccoin.zcash.ui.design.component.AccountAvatarState
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.viewingkey.vkAvatar
import co.electriccoin.zcash.viewingkey.R

/**
 * Renames a view-only account or starts removing it.
 *
 * @param avatar the account avatar in its colour, whose pencil badge opens the colour picker.
 * @param counter how many of the allowed characters [field] uses, shown under it.
 * @param removeInfo the line above the buttons that says removing asks for a confirmation.
 */
@Immutable
data class EditVKAccountState(
    val title: StringResource,
    val avatar: AccountAvatarState,
    val heading: StringResource,
    val message: StringResource,
    val fieldLabel: StringResource,
    val field: TextFieldState,
    val placeholder: StringResource,
    val counter: StringResource,
    val removeInfoIcon: ImageResource.ByDrawable,
    val removeInfo: StringResource,
    val removeButton: ButtonState,
    val saveButton: ButtonState,
    val onBack: () -> Unit,
) {
    companion object {
        val preview: EditVKAccountState = preview(name = "Zodl Vault")

        val previewChanged: EditVKAccountState = preview(name = "Savings")

        val previewLimit: EditVKAccountState = preview(name = "Zodl Savings")

        val previewColored: EditVKAccountState =
            preview(name = "Zodl Vault", color = Color(PREVIEW_COLOR), isColorChanged = true)

        /**
         * The screen for [name] typed over the account's [currentName], with the avatar in [color] or
         * the default avatar without one. Save is enabled while no save is running and either the
         * trimmed name is not blank and differs from [currentName], or [isColorChanged].
         */
        fun create(
            name: String,
            currentName: String,
            isSaving: Boolean,
            color: Color?,
            isColorChanged: Boolean,
            onAvatarEditClick: () -> Unit,
            onValueChange: (String) -> Unit,
            onSaveClick: () -> Unit,
            onRemoveClick: () -> Unit,
            onBack: () -> Unit,
        ) = EditVKAccountState(
            title = stringRes(R.string.view_only_edit_title),
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
            removeInfoIcon = ImageResource.ByDrawable(R.drawable.view_only_ic_info),
            removeInfo = stringRes(R.string.view_only_edit_remove_info),
            removeButton =
                ButtonState(
                    text = stringRes(R.string.view_only_remove_account),
                    style = ButtonStyle.DESTRUCTIVE1,
                    isEnabled = !isSaving,
                    onClick = onRemoveClick,
                ),
            saveButton =
                ButtonState(
                    text = stringRes(R.string.view_only_edit_save),
                    style = ButtonStyle.PRIMARY,
                    isEnabled = (canSave(name, currentName) || isColorChanged) && !isSaving,
                    isLoading = isSaving,
                    onClick = onSaveClick,
                ),
            onBack = onBack,
        )

        /** Whether [name] is a new, non-blank name for an account currently named [currentName]. */
        fun canSave(
            name: String,
            currentName: String
        ): Boolean {
            val trimmed = name.trim()
            return trimmed.isNotEmpty() && trimmed != currentName
        }

        private fun preview(
            name: String,
            color: Color? = null,
            isColorChanged: Boolean = false,
        ) = create(
            name = name,
            currentName = "Zodl Vault",
            isSaving = false,
            color = color,
            isColorChanged = isColorChanged,
            onAvatarEditClick = {},
            onValueChange = {},
            onSaveClick = {},
            onRemoveClick = {},
            onBack = {},
        )

        private const val PREVIEW_COLOR = 0xFFBE38F3
    }
}

object EditVKAccountTag {
    const val FIELD = "view_only_edit_account_field"
    const val COUNTER = "view_only_edit_account_counter"
    const val REMOVE_BUTTON = "view_only_edit_account_remove_button"
    const val SAVE_BUTTON = "view_only_edit_account_save_button"
}
