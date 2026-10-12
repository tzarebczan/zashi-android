package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.model.Account
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.repository.PendingViewOnlyImport
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepositoryImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A submitted key becomes the pending import only when it is importable and not in the wallet yet;
 * the wallet's own keys count as already imported, whatever their case.
 */
class SubmitViewOnlyKeyUseCaseTest {
    private val repository = ViewOnlyPendingImportRepositoryImpl()

    @Test
    fun anImportableNewKeyBecomesThePendingImport() =
        runTest {
            val result = useCase(validation = UfvkValidation.Valid(KEY), existingKeys = listOf(OTHER_KEY))("  input ")

            assertEquals(SubmitViewOnlyKeyResult.Accepted, result)
            assertEquals(PendingViewOnlyImport(ufvk = KEY, name = ""), repository.get())
        }

    @Test
    fun aKeyTheWalletHoldsIsAlreadyImported() =
        runTest {
            val useCase =
                useCase(validation = UfvkValidation.Valid(KEY), existingKeys = listOf(OTHER_KEY, KEY.uppercase()))
            val result = useCase("input")

            assertEquals(SubmitViewOnlyKeyResult.AlreadyImported, result)
            assertNull(repository.get())
        }

    @Test
    fun anInvalidKeyIsNotKept() =
        runTest {
            val result = useCase(validation = UfvkValidation.Invalid, existingKeys = emptyList())("input")

            assertEquals(SubmitViewOnlyKeyResult.Invalid, result)
            assertNull(repository.get())
        }

    private fun useCase(
        validation: UfvkValidation,
        existingKeys: List<String?>,
    ) = SubmitViewOnlyKeyUseCase(
        validateUfvk = mockk { coEvery { this@mockk.invoke(any(), any()) } returns validation },
        accountDataSource =
            mockk<AccountDataSource> {
                coEvery { getAllAccounts() } returns existingKeys.map(::account)
            },
        pendingImportRepository = repository,
    )

    private fun account(ufvk: String?): WalletAccount {
        val sdkAccount = mockk<Account> { every { this@mockk.ufvk } returns ufvk }
        return mockk { every { this@mockk.sdkAccount } returns sdkAccount }
    }

    private companion object {
        const val KEY = "uview1pendingkeyfixture"
        const val OTHER_KEY = "uview1otherkeyfixture"
    }
}
