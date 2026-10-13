package co.electriccoin.zcash.ui.screen.viewingkey

import kotlinx.serialization.Serializable

/** The "Add Account" options sheet. */
@Serializable
data object AddVKAccountArgs

/**
 * The screen that takes the full viewing key to import, pasted or typed. Like every route of the
 * flow it carries no key: routes are saved into the instance state, so the key lives only in
 * [co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository] and the view models.
 */
@Serializable
data object ImportVKArgs

/** The camera scanner for a full viewing key QR code. */
@Serializable
data object ScanVKArgs

/** The screen that names the account being imported. */
@Serializable
data object NameVKArgs

/** The import success screen. */
@Serializable
data object ImportedVKArgs

/** The sheet shown when the key belongs to an account the wallet already holds. */
@Serializable
data object AlreadyImportedVKArgs

/**
 * Renames or removes the view-only account [accountKeyId] names (`AccountUuid.toStorageKeyId()`).
 * The route carries only that id, never the account's viewing key.
 */
@Serializable
data class EditVKAccountArgs(
    val accountKeyId: String
)

/** The sheet that confirms removing the view-only account [accountKeyId] names. */
@Serializable
data class RemoveVKAccountArgs(
    val accountKeyId: String
)
