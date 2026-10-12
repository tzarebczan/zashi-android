package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.repository.PendingViewOnlyImport
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository

/**
 * Takes the viewing key the user pasted or scanned: an importable key the wallet does not hold yet
 * becomes the pending import, to be named on the next screen. The key goes to memory only.
 */
class SubmitViewOnlyKeyUseCase(
    private val validateUfvk: ValidateUfvkUseCase,
    private val accountDataSource: AccountDataSource,
    private val pendingImportRepository: ViewOnlyPendingImportRepository,
) {
    suspend operator fun invoke(input: String): SubmitViewOnlyKeyResult =
        when (val validation = validateUfvk(input)) {
            UfvkValidation.Invalid -> {
                SubmitViewOnlyKeyResult.Invalid
            }

            is UfvkValidation.Valid -> {
                if (isAlreadyImported(validation.ufvk)) {
                    SubmitViewOnlyKeyResult.AlreadyImported
                } else {
                    pendingImportRepository.set(PendingViewOnlyImport(ufvk = validation.ufvk, name = ""))
                    SubmitViewOnlyKeyResult.Accepted
                }
            }
        }

    private suspend fun isAlreadyImported(ufvk: String) =
        accountDataSource.getAllAccounts().any { it.sdkAccount.ufvk?.lowercase() == ufvk }
}

enum class SubmitViewOnlyKeyResult {
    Accepted,
    Invalid,
    AlreadyImported,
}
