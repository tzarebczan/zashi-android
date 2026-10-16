package co.electriccoin.zcash.ui.screen.editaccount

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import co.electriccoin.zcash.ui.common.model.AccountNameLimit
import co.electriccoin.zcash.ui.design.R
import co.electriccoin.zcash.ui.design.component.AccountAvatarState
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.imageRes
import co.electriccoin.zcash.ui.design.util.stringRes

/**
 * Edits the Zodl account's colour, or a view-only account's colour and name and starts removing it.
 *
 * @param avatar the account avatar in its colour, whose pencil badge opens the colour picker.
 * @param counter how many of the allowed characters [field] uses, shown under it; null while the
 *        name cannot be changed.
 * @param remove the removal entry under the field, only for a view-only account.
 */
@Immutable
data class EditAccountState(
    val title: StringResource,
    val avatar: AccountAvatarState,
    val heading: StringResource,
    val message: StringResource,
    val fieldLabel: StringResource,
    val field: TextFieldState,
    val placeholder: StringResource,
    val counter: StringResource?,
    val remove: EditAccountRemoveState?,
    val saveButton: ButtonState,
    val onBack: () -> Unit,
) {
    companion object {
        val preview: EditAccountState = previewViewOnly(name = "Zodl Vault")

        val previewLimit: EditAccountState = previewViewOnly(name = "Zodl Savings", canSave = true)

        val previewColored: EditAccountState =
            previewViewOnly(name = "Zodl Vault", color = Color(PREVIEW_COLOR), canSave = true)

        val previewDuplicate: EditAccountState = previewViewOnly(name = "Zodl", isDuplicate = true)

        val previewZodl: EditAccountState = previewZodl(image = imageRes(R.drawable.ic_item_zashi))

        val previewZodlColored: EditAccountState =
            previewZodl(
                image =
                    ImageResource.Layered(
                        background = Color(PREVIEW_COLOR),
                        foreground = ImageResource.ByDrawable(R.drawable.ic_item_zodl_glyph)
                    )
            )

        /**
         * The screen for an account showing [image] and [name]. [nameLength] is the code-point length
         * of an editable [name]; null for a name that cannot be changed, which disables the field and
         * hides the counter. [isDuplicate] says under the field that another account has the name.
         * Save is enabled while [canSave] and no save is running.
         */
        fun create(
            image: ImageResource,
            name: StringResource,
            nameLength: Int?,
            isDuplicate: Boolean,
            canSave: Boolean,
            isSaving: Boolean,
            remove: EditAccountRemoveState?,
            onAvatarEditClick: () -> Unit,
            onValueChange: (String) -> Unit,
            onSaveClick: () -> Unit,
            onBack: () -> Unit,
        ) = EditAccountState(
            title = stringRes(R.string.editAccount_title),
            avatar = AccountAvatarState.create(image = image, onEditClick = onAvatarEditClick),
            heading = stringRes(R.string.accountName_heading),
            message = stringRes(R.string.accountName_message),
            fieldLabel = stringRes(R.string.accountName_label),
            field =
                TextFieldState(
                    value = name,
                    error = if (isDuplicate) stringRes(R.string.accountName_duplicate) else null,
                    isEnabled = nameLength != null,
                    onValueChange = onValueChange
                ),
            placeholder = stringRes(R.string.accountName_placeholder),
            counter =
                nameLength?.let {
                    stringRes(R.string.accountName_counter, it, AccountNameLimit.MAX_LENGTH)
                },
            remove = remove,
            saveButton =
                ButtonState(
                    text = stringRes(R.string.editAccount_save),
                    style = ButtonStyle.PRIMARY,
                    isEnabled = canSave && !isSaving,
                    isLoading = isSaving,
                    onClick = onSaveClick,
                ),
            onBack = onBack,
        )

        private fun previewViewOnly(
            name: String,
            color: Color? = null,
            canSave: Boolean = false,
            isDuplicate: Boolean = false,
        ) = create(
            image =
                ImageResource.Layered(
                    background = color ?: Color.Unspecified,
                    foreground = ImageResource.ByDrawable(R.drawable.ic_item_zodl_glyph)
                ),
            name = stringRes(name),
            nameLength = AccountNameLimit.lengthOf(name),
            isDuplicate = isDuplicate,
            canSave = canSave,
            isSaving = false,
            remove =
                EditAccountRemoveState.create(
                    icon = R.drawable.ic_info,
                    info = stringRes("Removal info"),
                    buttonText = stringRes("Remove"),
                    isSaving = false,
                    onClick = {}
                ),
            onAvatarEditClick = {},
            onValueChange = {},
            onSaveClick = {},
            onBack = {},
        )

        private fun previewZodl(image: ImageResource) =
            create(
                image = image,
                name = stringRes("Zodl"),
                nameLength = null,
                isDuplicate = false,
                canSave = false,
                isSaving = false,
                remove = null,
                onAvatarEditClick = {},
                onValueChange = {},
                onSaveClick = {},
                onBack = {},
            )

        private const val PREVIEW_COLOR = 0xFFBE38F3
    }
}

/** The line that says removing asks for a confirmation, with its [icon], and the Remove Account button. */
@Immutable
data class EditAccountRemoveState(
    val icon: ImageResource.ByDrawable,
    val info: StringResource,
    val button: ButtonState,
) {
    companion object {
        /** The removal entry, disabled while a save is running. */
        fun create(
            @DrawableRes icon: Int,
            info: StringResource,
            buttonText: StringResource,
            isSaving: Boolean,
            onClick: () -> Unit,
        ) = EditAccountRemoveState(
            icon = ImageResource.ByDrawable(icon),
            info = info,
            button =
                ButtonState(
                    text = buttonText,
                    style = ButtonStyle.DESTRUCTIVE1,
                    isEnabled = !isSaving,
                    onClick = onClick,
                ),
        )
    }
}

object EditAccountTag {
    const val FIELD = "edit_account_field"
    const val COUNTER = "edit_account_counter"
    const val REMOVE_BUTTON = "edit_account_remove_button"
    const val SAVE_BUTTON = "edit_account_save_button"
}
