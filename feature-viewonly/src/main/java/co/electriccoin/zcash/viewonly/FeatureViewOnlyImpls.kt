package co.electriccoin.zcash.viewonly

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.toStorageKeyId
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository
import co.electriccoin.zcash.ui.common.usecase.ImportViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyAccountImporter
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavContributor
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavigator
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.dialogComposable
import co.electriccoin.zcash.ui.screen.connecthw.HWWalletBranding
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyAddAccountArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyAlreadyImportedArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyEditAccountArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyImportArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyImportedArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyNameArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyRemoveAccountArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyScanArgs
import co.electriccoin.zcash.ui.screen.viewonly.addaccount.ViewOnlyAddAccountScreen
import co.electriccoin.zcash.ui.screen.viewonly.alreadyimported.ViewOnlyAlreadyImportedScreen
import co.electriccoin.zcash.ui.screen.viewonly.edit.ViewOnlyEditAccountScreen
import co.electriccoin.zcash.ui.screen.viewonly.imported.ViewOnlyImportedScreen
import co.electriccoin.zcash.ui.screen.viewonly.name.ViewOnlyNameScreen
import co.electriccoin.zcash.ui.screen.viewonly.paste.ViewOnlyPasteScreen
import co.electriccoin.zcash.ui.screen.viewonly.remove.ViewOnlyRemoveAccountScreen
import co.electriccoin.zcash.ui.screen.viewonly.scan.ViewOnlyScanScreen

/**
 * The module's own copy and images for everything ui-lib shows about a view-only account.
 */
class ViewOnlyContentImpl : ViewOnlyContent {
    override val accountIcon: Int = R.drawable.view_only_ic_account

    override val defaultAccountName = stringRes(R.string.view_only_account_default_name)

    override val topBarSubtitle = stringRes(R.string.view_only_top_bar_subtitle)

    override val accountListSection = stringRes(R.string.view_only_account_list_section)

    override val accountListSectionPlural = stringRes(R.string.view_only_account_list_section_plural)

    override val addAccountButton = stringRes(R.string.view_only_add_account)

    override val addAccountButtonIcon: Int = R.drawable.view_only_ic_plus

    override val unsupportedOperationTitle = stringRes(R.string.view_only_unsupported_title)

    override val unsupportedOperationMessage = stringRes(R.string.view_only_unsupported_message)

    override val receiveShieldedTitle = stringRes(R.string.view_only_receive_shielded_title)

    override val receiveTransparentTitle = stringRes(R.string.view_only_receive_transparent_title)

    override val integrationsDisabledInfo = stringRes(R.string.view_only_integrations_disabled_info)

    override val taxExportFileNamePrefix = "View_Only"

    override val enrollmentBranding =
        HWWalletBranding(
            logo = R.drawable.view_only_ic_avatar,
            deviceQuestion = stringRes(R.string.view_only_birthday_question),
            deviceDescription = stringRes(R.string.view_only_birthday_description),
            connectNewDevice = stringRes(R.string.view_only_birthday_new),
            connectActiveDevice = stringRes(R.string.view_only_birthday_used),
            enterBlockHeightManually = stringRes(R.string.view_only_birthday_enter_manually),
            connect = stringRes(R.string.view_only_birthday_import),
            newDeviceTestTag = ViewOnlyEnrollmentTag.NEW_ACCOUNT,
            activeDeviceTestTag = ViewOnlyEnrollmentTag.USED_ACCOUNT,
            enterManuallyTestTag = ViewOnlyEnrollmentTag.ENTER_MANUALLY_BTN,
            connectTestTag = ViewOnlyEnrollmentTag.IMPORT_BTN,
            blockHeightFieldTestTag = ViewOnlyEnrollmentTag.BLOCK_HEIGHT_FIELD,
        )

    override val keepOpenDescription = stringRes(R.string.view_only_keep_open_description)

    override val keepOpenSubtitle = stringRes(R.string.view_only_keep_open_subtitle)

    override val keepOpenDisclaimer = stringRes(R.string.view_only_keep_open_warning)

    override val keepOpenCheckboxLabel = stringRes(R.string.view_only_keep_open_checkbox)
}

object ViewOnlyEnrollmentTag {
    const val NEW_ACCOUNT = "VIEW_ONLY_NEW_OR_ACTIVE_NEW_ACCOUNT"
    const val USED_ACCOUNT = "VIEW_ONLY_NEW_OR_ACTIVE_USED_ACCOUNT"
    const val ENTER_MANUALLY_BTN = "VIEW_ONLY_DATE_ENTER_MANUALLY"
    const val IMPORT_BTN = "VIEW_ONLY_HEIGHT_IMPORT"
    const val BLOCK_HEIGHT_FIELD = "VIEW_ONLY_HEIGHT_BLOCK_HEIGHT_FIELD"
}

class ViewOnlyAccountImporterImpl(
    private val pendingImportRepository: ViewOnlyPendingImportRepository,
    private val importViewOnlyAccount: ImportViewOnlyAccountUseCase,
) : ViewOnlyAccountImporter {
    override fun hasPendingImport(): Boolean = pendingImportRepository.get() != null

    override suspend fun importAccount(birthday: BlockHeight?) = importViewOnlyAccount(birthday)
}

class ViewOnlyNavigatorImpl(
    private val navigationRouter: NavigationRouter,
) : ViewOnlyNavigator {
    override fun forwardToAddAccount() = navigationRouter.forward(ViewOnlyAddAccountArgs)

    override fun forwardToImport() = navigationRouter.forward(ViewOnlyImportArgs)

    override fun forwardToImported() = navigationRouter.forward(ViewOnlyImportedArgs)

    override fun forwardToEditAccount(accountUuid: AccountUuid) =
        navigationRouter.forward(ViewOnlyEditAccountArgs(accountKeyId = accountUuid.toStorageKeyId()))
}

class ViewOnlyNavContributorImpl : ViewOnlyNavContributor {
    override fun contribute(navGraphBuilder: NavGraphBuilder) {
        with(navGraphBuilder) {
            dialogComposable<ViewOnlyAddAccountArgs> { ViewOnlyAddAccountScreen() }
            composable<ViewOnlyImportArgs> { ViewOnlyPasteScreen() }
            composable<ViewOnlyScanArgs> { ViewOnlyScanScreen() }
            composable<ViewOnlyNameArgs> { ViewOnlyNameScreen() }
            composable<ViewOnlyImportedArgs> { ViewOnlyImportedScreen() }
            dialogComposable<ViewOnlyAlreadyImportedArgs> { ViewOnlyAlreadyImportedScreen() }
            composable<ViewOnlyEditAccountArgs> { ViewOnlyEditAccountScreen(it.toRoute()) }
            dialogComposable<ViewOnlyRemoveAccountArgs> { ViewOnlyRemoveAccountScreen(it.toRoute()) }
        }
    }
}
