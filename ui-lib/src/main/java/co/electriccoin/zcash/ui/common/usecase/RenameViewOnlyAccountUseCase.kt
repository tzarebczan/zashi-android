package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.AccountNameDuplicates
import co.electriccoin.zcash.ui.common.model.AccountNameLimit
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.toStorageKeyId
import co.electriccoin.zcash.ui.common.provider.AccountNameLock
import co.electriccoin.zcash.ui.common.provider.AccountNameProvider

/**
 * Stores a new name for a view-only account in the wallet database, which the account list, the top
 * bar and every other place that shows the account read through the account data source.
 *
 * A saved name also clears the name an earlier build stored in preferences, under [AccountNameLock],
 * so [MigrateStoredAccountNamesUseCase] never moves that older name back over the user's choice.
 */
class RenameViewOnlyAccountUseCase(
    private val accountDataSource: AccountDataSource,
    private val accountNameProvider: AccountNameProvider,
    private val accountNameLock: AccountNameLock,
    private val accountNames: ObserveAccountNamesUseCase,
) {
    /**
     * Saves [name] trimmed and capped at [AccountNameLimit.MAX_LENGTH] code points. A blank
     * name is ignored, so the account never loses its name, and so is a name another account already
     * shows, by [AccountNameDuplicates]; the screens refuse such a name before it gets here. Clearing
     * the stored name afterwards is best-effort: the new name is already saved, so a failure there is
     * not reported.
     */
    suspend operator fun invoke(
        account: ViewOnlyAccount,
        name: String
    ) {
        val normalized = AccountNameLimit.normalize(name)
        if (normalized.isEmpty()) return
        val keyId = account.sdkAccount.accountUuid.toStorageKeyId()
        if (AccountNameDuplicates.isDuplicate(normalized, accountNames.get(), excluding = keyId)) return
        accountNameLock.withLock {
            accountDataSource.renameAccount(account, normalized)
            accountNameProvider.clear(account.sdkAccount.accountUuid)
        }
    }
}
