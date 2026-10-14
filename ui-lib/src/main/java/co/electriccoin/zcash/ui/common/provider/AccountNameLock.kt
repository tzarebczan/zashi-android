package co.electriccoin.zcash.ui.common.provider

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes a user's rename of a view-only account against
 * [co.electriccoin.zcash.ui.common.usecase.MigrateStoredAccountNamesUseCase]'s move of the name in
 * [AccountNameProvider]. Each side holds the lock from its read of the stored name to its clear, so the
 * move can never write a stored name it read before a rename back over that rename. One instance is
 * shared app-wide, and it goes away together with the migration.
 */
class AccountNameLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
