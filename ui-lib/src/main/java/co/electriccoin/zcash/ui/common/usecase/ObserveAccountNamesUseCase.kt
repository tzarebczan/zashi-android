package co.electriccoin.zcash.ui.common.usecase

import android.content.Context
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.toStorageKeyId
import co.electriccoin.zcash.ui.design.util.getString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The name every account shows in the app, the Zodl account's and the hardware accounts' included,
 * keyed by the account's storage key id. The names feed
 * [co.electriccoin.zcash.ui.common.model.AccountNameDuplicates], which keeps a new name unique.
 */
class ObserveAccountNamesUseCase(
    private val accountDataSource: AccountDataSource,
    private val context: Context,
) {
    /** The shown names, emitted again only when one of them changes. */
    fun observe(): Flow<Map<String, String>> =
        accountDataSource.allAccounts
            .filterNotNull()
            .map { accounts ->
                accounts.associate { it.sdkAccount.accountUuid.toStorageKeyId() to it.name.getString(context) }
            }.distinctUntilChanged()

    /** The shown names right now, once the accounts have loaded. */
    suspend fun get(): Map<String, String> = observe().first()
}
