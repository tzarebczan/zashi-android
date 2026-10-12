package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.provider.AccountNameProvider

/**
 * Stores a new name for a view-only account. The SDK cannot rename an account, so the name goes to
 * the app's own encrypted store, which the account list, the top bar and every other place that
 * shows the account read through the account data source.
 */
class RenameViewOnlyAccountUseCase(
    private val accountNameProvider: AccountNameProvider,
) {
    /**
     * Saves [name] trimmed. A blank name is ignored, so the account never loses its name.
     */
    suspend operator fun invoke(
        account: ViewOnlyAccount,
        name: String
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        accountNameProvider.save(account.sdkAccount.accountUuid, trimmed)
    }
}
