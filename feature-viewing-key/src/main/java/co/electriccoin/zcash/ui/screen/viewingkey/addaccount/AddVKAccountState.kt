package co.electriccoin.zcash.ui.screen.viewingkey.addaccount

import androidx.compose.runtime.Immutable
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.component.ModalBottomSheetState
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.viewingkey.R

/**
 * The "Add Account" options sheet: connecting a hardware wallet while a vendor is still
 * unconnected, and importing a full viewing key.
 */
@Immutable
data class AddVKAccountState(
    val title: StringResource,
    val options: List<AddVKAccountOptionState>,
    val dismissButton: ButtonState,
    override val onBack: () -> Unit,
) : ModalBottomSheetState {
    companion object {
        val preview: AddVKAccountState = preview(canConnectHardwareWallet = true)

        val previewHardwareWalletsConnected: AddVKAccountState = preview(canConnectHardwareWallet = false)

        fun create(
            canConnectHardwareWallet: Boolean,
            onConnectHardwareWalletClick: () -> Unit,
            onImportClick: () -> Unit,
            onBack: () -> Unit,
        ) = AddVKAccountState(
            title = stringRes(R.string.view_only_add_account),
            options =
                listOfNotNull(
                    AddVKAccountOptionState(
                        icon = ImageResource.ByDrawable(R.drawable.view_only_ic_option_hardware_wallet),
                        title = stringRes(R.string.view_only_add_account_hardware_title),
                        subtitle = stringRes(R.string.view_only_add_account_hardware_subtitle),
                        testTag = AddVKAccountTag.CONNECT_HARDWARE_WALLET,
                        onClick = onConnectHardwareWalletClick,
                    ).takeIf { canConnectHardwareWallet },
                    AddVKAccountOptionState(
                        icon = ImageResource.ByDrawable(R.drawable.view_only_ic_option_import),
                        title = stringRes(R.string.view_only_add_account_import_title),
                        subtitle = stringRes(R.string.view_only_add_account_import_subtitle),
                        testTag = AddVKAccountTag.IMPORT_VIEWING_KEY,
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
data class AddVKAccountOptionState(
    val icon: ImageResource.ByDrawable,
    val title: StringResource,
    val subtitle: StringResource,
    val testTag: String,
    val onClick: () -> Unit,
)

object AddVKAccountTag {
    const val CONNECT_HARDWARE_WALLET = "view_only_add_account_connect_hardware_wallet"
    const val IMPORT_VIEWING_KEY = "view_only_add_account_import_viewing_key"
}
