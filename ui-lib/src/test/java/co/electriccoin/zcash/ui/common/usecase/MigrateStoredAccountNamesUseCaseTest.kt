package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.provider.AccountNameLock
import co.electriccoin.zcash.ui.common.provider.AccountNameProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A view-only account's name stored by an earlier build moves into the wallet database once and is
 * then cleared from preferences; a name the wallet already has is only cleared, an account without a
 * stored name is left alone, and a name that fails to move stays stored for the next start without
 * holding up the other accounts. A user's rename saved while the move runs is never overwritten by
 * the older stored name.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MigrateStoredAccountNamesUseCaseTest {
    private val accountDataSource = mockk<AccountDataSource>(relaxed = true)

    private val accountNameProvider = mockk<AccountNameProvider>(relaxed = true)

    private val useCase = MigrateStoredAccountNamesUseCase(accountDataSource, accountNameProvider, AccountNameLock())

    @Test
    fun aStoredNameIsMovedIntoTheWalletAndCleared() =
        runTest {
            val account = viewOnly(FIRST_UUID, sdkName = "Imported")
            coEvery { accountDataSource.getAllAccounts() } returns listOf(account)
            coEvery { accountNameProvider.get(FIRST_UUID) } returns "Vault"

            useCase()

            coVerify(exactly = 1) { accountDataSource.renameAccount(account, "Vault") }
            coVerify(exactly = 1) { accountNameProvider.clear(FIRST_UUID) }
        }

    @Test
    fun aStoredNameTheWalletAlreadyHasIsOnlyCleared() =
        runTest {
            coEvery { accountDataSource.getAllAccounts() } returns listOf(viewOnly(FIRST_UUID, sdkName = "Vault"))
            coEvery { accountNameProvider.get(FIRST_UUID) } returns "Vault"

            useCase()

            coVerify(exactly = 0) { accountDataSource.renameAccount(any(), any()) }
            coVerify(exactly = 1) { accountNameProvider.clear(FIRST_UUID) }
        }

    @Test
    fun anAccountWithoutAStoredNameIsLeftAlone() =
        runTest {
            coEvery { accountDataSource.getAllAccounts() } returns listOf(viewOnly(FIRST_UUID, sdkName = "Imported"))
            coEvery { accountNameProvider.get(FIRST_UUID) } returns null

            useCase()

            coVerify(exactly = 0) { accountDataSource.renameAccount(any(), any()) }
            coVerify(exactly = 0) { accountNameProvider.clear(any()) }
        }

    @Test
    fun aNameThatFailsToMoveStaysStoredAndTheOtherAccountsStillMove() =
        runTest {
            val failing = viewOnly(FIRST_UUID, sdkName = "Imported")
            val moving = viewOnly(SECOND_UUID, sdkName = "Imported")
            coEvery { accountDataSource.getAllAccounts() } returns listOf(failing, moving)
            coEvery { accountNameProvider.get(FIRST_UUID) } returns "Vault"
            coEvery { accountNameProvider.get(SECOND_UUID) } returns "Savings"
            coEvery { accountDataSource.renameAccount(failing, any()) } throws
                InitializeException.RenameAccountException(RuntimeException("database locked"))

            useCase()

            coVerify(exactly = 0) { accountNameProvider.clear(FIRST_UUID) }
            coVerify(exactly = 1) { accountDataSource.renameAccount(moving, "Savings") }
            coVerify(exactly = 1) { accountNameProvider.clear(SECOND_UUID) }
        }

    @Test
    fun onlyViewOnlyAccountsAreMigrated() =
        runTest {
            coEvery { accountDataSource.getAllAccounts() } returns listOf(EditAccountFixtures.zashi())

            useCase()

            coVerify(exactly = 0) { accountNameProvider.get(any()) }
            coVerify(exactly = 0) { accountDataSource.renameAccount(any(), any()) }
        }

    @Test
    fun aRenameSavedWhileANameIsMovingLandsAfterTheMove() =
        runTest {
            val account = viewOnly(FIRST_UUID, sdkName = "Imported")
            val race = Race(account, gatedName = "Vault")

            launch { race.migration() }
            runCurrent()
            launch { race.rename(account, "Savings") }
            runCurrent()
            race.gate.complete(Unit)
            runCurrent()

            assertEquals(listOf("Vault", "Savings"), race.writtenNames)
            assertNull(race.names.get(FIRST_UUID))
        }

    @Test
    fun aRenameSavedBeforeTheMoveReadsTheStoredNameLeavesNothingToMove() =
        runTest {
            val account = viewOnly(FIRST_UUID, sdkName = "Imported")
            val race = Race(account, gatedName = "Savings")

            launch { race.rename(account, "Savings") }
            runCurrent()
            launch { race.migration() }
            runCurrent()
            race.gate.complete(Unit)
            runCurrent()

            assertEquals(listOf("Savings"), race.writtenNames)
            assertNull(race.names.get(FIRST_UUID))
        }

    /**
     * The migration and a user's rename sharing one [AccountNameLock] and one stored-name store that
     * holds "Vault" for [account]. A write of [gatedName] to the wallet suspends until [gate]
     * completes, and every name lands in [writtenNames] only once its write has finished, so the
     * list is the order in which the wallet saw the names.
     */
    private class Race(
        account: ViewOnlyAccount,
        gatedName: String,
    ) {
        val gate = CompletableDeferred<Unit>()
        val writtenNames = mutableListOf<String>()
        val names = InMemoryAccountNameProvider(mutableMapOf(account.sdkAccount.accountUuid to "Vault"))
        private val lock = AccountNameLock()
        private val dataSource =
            mockk<AccountDataSource>(relaxed = true) {
                coEvery { getAllAccounts() } returns listOf(account)
                coEvery { renameAccount(account, any()) } coAnswers {
                    val name = secondArg<String>()
                    if (name == gatedName) gate.await()
                    writtenNames += name
                }
            }

        suspend fun migration() = MigrateStoredAccountNamesUseCase(dataSource, names, lock)()

        suspend fun rename(
            account: ViewOnlyAccount,
            name: String
        ) = RenameViewOnlyAccountUseCase(dataSource, names, lock)(account, name)
    }

    private class InMemoryAccountNameProvider(
        private val stored: MutableMap<AccountUuid, String>
    ) : AccountNameProvider {
        override suspend fun get(accountUuid: AccountUuid): String? = stored[accountUuid]

        override suspend fun clear(accountUuid: AccountUuid) {
            stored.remove(accountUuid)
        }
    }

    private fun viewOnly(
        uuid: AccountUuid,
        sdkName: String?,
    ): ViewOnlyAccount =
        EditAccountFixtures.viewOnly().copy(
            sdkAccount =
                mockk<Account> {
                    every { accountUuid } returns uuid
                    every { accountName } returns sdkName
                }
        )

    private companion object {
        val FIRST_UUID: AccountUuid = AccountUuid.new(ByteArray(16) { (it + 3).toByte() })
        val SECOND_UUID: AccountUuid = AccountUuid.new(ByteArray(16) { (it + 4).toByte() })
    }
}
