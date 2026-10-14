package co.electriccoin.zcash.ui.screen.viewingkey

import androidx.compose.ui.graphics.Color
import co.electriccoin.zcash.ui.design.component.AccountAvatarState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.viewingkey.R
import co.electriccoin.zcash.ui.design.R as DesignR

/**
 * The view-only account avatar: the eye on the default circle, or on [color] once there is one,
 * with the pencil badge that opens the colour picker when [onEditClick] is given.
 */
internal fun vkAvatar(
    color: Color? = null,
    onEditClick: (() -> Unit)? = null,
) = AccountAvatarState(
    image =
        ImageResource.Layered(
            background = color ?: Color.Unspecified,
            foreground = ImageResource.ByDrawable(R.drawable.view_only_ic_avatar_eye)
        ),
    editButton =
        onEditClick?.let {
            IconButtonState(
                icon = DesignR.drawable.ic_avatar_edit,
                contentDescription = stringRes(DesignR.string.colorPicker_edit),
                onClick = it
            )
        },
)
