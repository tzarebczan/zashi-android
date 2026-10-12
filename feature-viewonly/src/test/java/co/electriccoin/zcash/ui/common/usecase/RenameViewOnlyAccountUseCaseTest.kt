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
    fun aLongNameIsCappedByCodePointsWithoutSplittingAnEmoji() =
        runTest {
            useCase(ViewOnlyAccountFixtures.viewOnly(), "Vault " + LOCK.repeat(7))

            coVerify(exactly = 1) {
                accountNameProvider.save(ViewOnlyAccountFixtures.viewOnlyUuid, "Vault " + LOCK.repeat(6))
            }
        }

    @Test
    fun aJoinerSequenceCutAtTheLimitLeavesNoDanglingJoiner() =
        runTest {
            useCase(ViewOnlyAccountFixtures.viewOnly(), "Our homes " + FAMILY)

            coVerify(exactly = 1) {
                accountNameProvider.save(ViewOnlyAccountFixtures.viewOnlyUuid, "Our homes " + MAN)
            }
        }

    @Test
    fun aBlankNameIsIgnored() =
        runTest {
            useCase(ViewOnlyAccountFixtures.viewOnly(), "   ")

            coVerify(exactly = 0) { accountNameProvider.save(any(), any()) }
        }
}

private const val LOCK = "\uD83D\uDD10"

private const val MAN = "\uD83D\uDC68"

private const val WOMAN = "\uD83D\uDC69"

private const val GIRL = "\uD83D\uDC67"

private const val ZWJ = "\u200D"

private const val FAMILY = MAN + ZWJ + WOMAN + ZWJ + GIRL
