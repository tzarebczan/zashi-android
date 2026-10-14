package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.provider.AccountNameLock
import co.electriccoin.zcash.ui.common.provider.AccountNameProvider
import co.electriccoin.zcash.ui.util.loggableNot
import kotlinx.coroutines.CancellationException

/**
 * Moves the view-only account names that earlier builds kept in encrypted preferences into the wallet
 * database, then clears them from preferences. Launched once from the application class at startup:
 * it waits for the first accounts load, which it never holds up, and is best-effort and idempotent,
 * so a name that could not be moved stays stored and is moved at the next start.
 *
 * Each account is moved under [AccountNameLock], from reading its stored name to clearing it. A user's
 * rename takes the same lock and clears the stored name, so a rename saved while the move runs either
 * lands after the move or leaves no stored name for it to move.
 */
class MigrateStoredAccountNamesUseCase(
    private val accountDataSource: AccountDataSource,
    private val accountNameProvider: AccountNameProvider,
    private val accountNameLock: AccountNameLock,
) {
    private val log = loggableNot("MigrateStoredAccountNamesUseCase")

    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke() {
        accountDataSource.getAllAccounts().filterIsInstance<ViewOnlyAccount>().forEach { account ->
            try {
                migrate(account)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("failed to move a stored account name into the wallet", e)
            }
        }
    }

    private suspend fun migrate(account: ViewOnlyAccount) =
        accountNameLock.withLock {
            val accountUuid = account.sdkAccount.accountUuid
            val storedName = accountNameProvider.get(accountUuid) ?: return@withLock
            if (storedName != account.sdkAccount.accountName) {
                accountDataSource.renameAccount(account, storedName)
            }
            accountNameProvider.clear(accountUuid)
        }
}
