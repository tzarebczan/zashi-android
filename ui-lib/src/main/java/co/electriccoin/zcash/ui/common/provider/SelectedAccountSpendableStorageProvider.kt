package co.electriccoin.zcash.ui.common.provider

import co.electriccoin.zcash.preference.StandardPreferenceProvider
import co.electriccoin.zcash.preference.model.entry.PreferenceKey

/**
 * Whether the account selected when the app last ran could spend, kept so a cold start can draw
 * Home's buttons for that account before the accounts load. Only a hint: the selected account's
 * own [co.electriccoin.zcash.ui.common.model.WalletAccount.isSpendable] replaces it as soon as it
 * is known. Defaults to true, the case of every wallet without a view-only account.
 */
interface SelectedAccountSpendableStorageProvider : BooleanStorageProvider

class SelectedAccountSpendableStorageProviderImpl(
    override val preferenceHolder: StandardPreferenceProvider,
) : BaseBooleanStorageProvider(
        key = PreferenceKey("selected_account_spendable"),
        default = true,
    ),
    SelectedAccountSpendableStorageProvider
