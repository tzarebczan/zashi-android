package co.electriccoin.zcash.ui.screen.viewonly.addaccount

import androidx.compose.runtime.Immutable
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.component.ModalBottomSheetState
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.viewonly.R

/**
 * The "Add Account" options sheet: connecting a hardware wallet while a vendor is still
 * unconnected, and importing a full viewing key.
 */
@Immutable
data class ViewOnlyAddAccountState(
    val title: StringResource,
    val options: List<ViewOnlyAddAccountOptionState>,
    val dismissButton: ButtonState,
    override val onBack: () -> Unit,
) : ModalBottomSheetState {
    companion object {
        val preview: ViewOnlyAddAccountState = preview(canConnectHardwareWallet = true)

        val previewHardwareWalletsConnected: ViewOnlyAddAccountState = preview(canConnectHardwareWallet = false)

        fun create(
            canConnectHardwareWallet: Boolean,
            onConnectHardwareWalletClick: () -> Unit,
            onImportClick: () -> Unit,
            onBack: () -> Unit,
        ) = ViewOnlyAddAccountState(
            title = stringRes(R.string.view_only_add_account),
            options =
                listOfNotNull(
                    ViewOnlyAddAccountOptionState(
                        icon = ImageResource.ByDrawable(R.drawable.view_only_ic_option_hardware_wallet),
                        title = stringRes(R.string.view_only_add_account_hardware_title),
                        subtitle = stringRes(R.string.view_only_add_account_hardware_subtitle),
                        testTag = ViewOnlyAddAccountTag.CONNECT_HARDWARE_WALLET,
                        onClick = onConnectHardwareWalletClick,
                    ).takeIf { canConnectHardwareWallet },
                    ViewOnlyAddAccountOptionState(
                        icon = ImageResource.ByDrawable(R.drawable.view_only_ic_option_import),
                        title = stringRes(R.string.view_only_add_account_import_title),
                        subtitle = stringRes(R.string.view_only_add_account_import_subtitle),
                        testTag = ViewOnlyAddAccountTag.IMPORT_VIEWING_KEY,
                        onClick = onImportClick,
                    ),
                ),
            dismissButton =
                ButtonState(
                    text = stringRes(R.string.view_only_add_account_dismiss),
                    style = ButtonStyle.SECONDARY,
                    onClick = onBack,
                ),
            onBack = onBack,
        )

        private fun preview(canConnectHardwareWallet: Boolean) =
            create(
                canConnectHardwareWallet = canConnectHardwareWallet,
                onConnectHardwareWalletClick = {},
                onImportClick = {},
                onBack = {},
            )
    }
}

@Immutable
data class ViewOnlyAddAccountOptionState(
    val icon: ImageResource.ByDrawable,
    val title: StringResource,
    val subtitle: StringResource,
    val testTag: String,
    val onClick: () -> Unit,
)

object ViewOnlyAddAccountTag {
    const val CONNECT_HARDWARE_WALLET = "view_only_add_account_connect_hardware_wallet"
    const val IMPORT_VIEWING_KEY = "view_only_add_account_import_viewing_key"
}
