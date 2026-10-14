package co.electriccoin.zcash.ui.screen.viewingkey.imported

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import co.electriccoin.zcash.ui.design.component.AccountAvatarState
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.viewingkey.vkAvatar
import co.electriccoin.zcash.viewingkey.R

@Immutable
data class ImportedVKState(
    val avatar: AccountAvatarState,
    val heading: StringResource,
    val message: StringResource,
    val doneButton: ButtonState,
    val onBack: () -> Unit,
) {
    companion object {
        val preview: ImportedVKState = create(color = null, onDoneClick = {}, onBack = {})

        /** The success screen with the new account's avatar in [color], the default one without. */
        fun create(
            color: Color?,
            onDoneClick: () -> Unit,
            onBack: () -> Unit,
        ) = ImportedVKState(
            avatar = vkAvatar(color = color),
            heading = stringRes(R.string.view_only_imported_heading),
            message = stringRes(R.string.view_only_imported_message),
            doneButton =
                ButtonState(
                    text = stringRes(R.string.view_only_imported_done),
                    style = ButtonStyle.PRIMARY,
                    onClick = onDoneClick,
                ),
            onBack = onBack,
        )
    }
}

object ImportedVKTag {
    const val DONE_BUTTON = "view_only_imported_done_button"
}
