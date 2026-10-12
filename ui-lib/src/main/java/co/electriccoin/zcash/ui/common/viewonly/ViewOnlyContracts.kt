package co.electriccoin.zcash.ui.common.viewonly

import androidx.annotation.DrawableRes
import androidx.navigation.NavGraphBuilder
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.screen.connecthw.HWWalletBranding

/**
 * The seam between ui-lib and the feature-viewonly module, shaped like LedgerContracts.kt: ui-lib
 * never imports feature-viewonly classes and talks only to these contracts, which the app module
 * wires in via Koin (`featureViewOnlyModule`).
 *
 * What stays in ui-lib is what the rest of the wallet must know about a view-only account: the
 * ViewOnlyAccount model, its import and mapping in the account data source, and the spend guards
 * that reject it. Every text and image the wallet shows for such an account lives in the module
 * and reaches ui-lib through [ViewOnlyContent].
 */
interface ViewOnlyNavigator {
    /** Opens the "Add Account" options sheet from the Wallets & Hardware screen. */
    fun forwardToAddAccount()

    /**
     * Opens the options of the view-only account [accountUuid], the destination of its "..." button
     * on the Wallets & Hardware screen. Implementations put only the account's storage-key id
     * (`AccountUuid.toStorageKeyId()`) in a route, never the viewing key or any other secret.
     */
    fun forwardToEditAccount(accountUuid: AccountUuid)

    /** Starts importing a full viewing key. */
    fun forwardToImport()

    /** Shows the import success screen for the account that was just imported. */
    fun forwardToImported()
}

/** Installs the view-only destinations into the wallet nav graph. */
interface ViewOnlyNavContributor {
    fun contribute(navGraphBuilder: NavGraphBuilder)
}

/**
 * The shared birthday screens' way into a view-only import: whether the key and name the flow
 * collected are still held in memory, and the import itself once the birthday is known.
 */
interface ViewOnlyAccountImporter {
    /**
     * False after process death emptied the in-memory pending import; the screens then return to
     * the root.
     */
    fun hasPendingImport(): Boolean

    /**
     * Imports and selects the pending account at [birthday] and forgets the pending key.
     *
     * @throws co.electriccoin.zcash.ui.common.model.ViewOnlyPendingImportMissingException if the
     *         pending import is gone.
     */
    suspend fun importAccount(birthday: BlockHeight?)
}

/**
 * Every text and image ui-lib shows for a view-only account. The module implements it with its own
 * resources, so none of this copy lives in ui-lib or ui-design-lib.
 */
interface ViewOnlyContent {
    /** The account's avatar, used wherever an account icon is shown. */
    @get:DrawableRes
    val accountIcon: Int

    /** The account name used when the name stored with the account is blank. */
    val defaultAccountName: StringResource

    /** The small label under the account name in the top app bar. */
    val topBarSubtitle: StringResource

    /** The Wallets & Hardware section header above a single view-only account. */
    val accountListSection: StringResource

    /** The Wallets & Hardware section header above several view-only accounts. */
    val accountListSectionPlural: StringResource

    /** The account list's call to action that opens the "Add Account" options. */
    val addAccountButton: StringResource

    /** The leading icon of [addAccountButton]. */
    @get:DrawableRes
    val addAccountButtonIcon: Int

    /** Title of the error shown when an action needs spend authority the account lacks. */
    val unsupportedOperationTitle: StringResource

    /** Message of the error shown when an action needs spend authority the account lacks. */
    val unsupportedOperationMessage: StringResource

    /** Title of the account's shielded receive address. */
    val receiveShieldedTitle: StringResource

    /** Title of the account's transparent receive address. */
    val receiveTransparentTitle: StringResource

    /** Why the integrations are disabled while a view-only account is selected. */
    val integrationsDisabledInfo: StringResource

    /** The prefix of the tax export file name, which is not translated. */
    val taxExportFileNamePrefix: String

    /**
     * The logo, copy and test tags of the shared birthday screens while they import a view-only
     * account; see `brandingOf`.
     */
    val enrollmentBranding: HWWalletBranding

    /** The instructions of the keep-open screen shown while an imported account syncs its history. */
    val keepOpenDescription: StringResource

    /** The subtitle of that keep-open screen. */
    val keepOpenSubtitle: StringResource

    /** The warning of that keep-open screen. */
    val keepOpenDisclaimer: StringResource

    /** The label of that keep-open screen's keep-screen-on checkbox. */
    val keepOpenCheckboxLabel: StringResource
}
