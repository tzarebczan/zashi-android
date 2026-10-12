package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.common.provider.AccountNameProvider
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * A rename stores the trimmed name under the account's UUID, and a blank name is never stored.
 */
class RenameViewOnlyAccountUseCaseTest {
    private val accountNameProvider = mockk<AccountNameProvider>(relaxed = true)

    private val useCase = RenameViewOnlyAccountUseCase(accountNameProvider)

    @Test
    fun theTrimmedNameIsStoredUnderTheAccountsUuid() =
        runTest {
            useCase(ViewOnlyAccountFixtures.viewOnly(), "  Savings ")

            coVerify(exactly = 1) { accountNameProvider.save(ViewOnlyAccountFixtures.viewOnlyUuid, "Savings") }
        }

    @Test
    fun aBlankNameIsIgnored() =
        runTest {
            useCase(ViewOnlyAccountFixtures.viewOnly(), "   ")

            coVerify(exactly = 0) { accountNameProvider.save(any(), any()) }
        }
}
