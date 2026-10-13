package co.electriccoin.zcash.ui.screen.viewingkey.imported

import androidx.compose.runtime.Immutable
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.viewingkey.R

@Immutable
data class ImportedVKState(
    val avatar: ImageResource.ByDrawable,
    val heading: StringResource,
    val message: StringResource,
    val doneButton: ButtonState,
    val onBack: () -> Unit,
) {
    companion object {
        val preview: ImportedVKState = create(onDoneClick = {}, onBack = {})

        fun create(
            onDoneClick: () -> Unit,
            onBack: () -> Unit,
        ) = ImportedVKState(
            avatar = ImageResource.ByDrawable(R.drawable.view_only_ic_avatar),
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
