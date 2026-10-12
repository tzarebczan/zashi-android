package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.zcash.ui.common.model.VersionInfo
import co.electriccoin.zcash.ui.common.model.ufvk.UnifiedFullViewingKeyEncoding
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.repository.PendingViewOnlyImport
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Takes the viewing key the user pasted or scanned: an importable key the wallet does not hold yet
 * becomes the pending import, to be named on the next screen. The key goes to memory only.
 *
 * A key counts as held already when it shares any Orchard, Sapling or transparent component with
 * an account in the wallet, not only when it is the same text: the SDK would otherwise silently
 * widen an existing account with a key that contains it, or refuse a key that overlaps one. The
 * existing keys are read from the synchronizer's own account list, which does not wait for any
 * account's addresses or balances.
 */
class SubmitViewOnlyKeyUseCase(
    private val validateUfvk: ValidateUfvkUseCase,
    private val synchronizerProvider: SynchronizerProvider,
    private val pendingImportRepository: ViewOnlyPendingImportRepository,
) {
    suspend operator fun invoke(
        input: String,
        network: ZcashNetwork = VersionInfo.NETWORK,
    ): SubmitViewOnlyKeyResult =
        when (val validation = validateUfvk(input, network)) {
            UfvkValidation.Invalid -> {
                SubmitViewOnlyKeyResult.Invalid
            }

            is UfvkValidation.Valid -> {
                if (isAlreadyImported(validation.ufvk, network)) {
                    SubmitViewOnlyKeyResult.AlreadyImported
                } else {
                    pendingImportRepository.set(PendingViewOnlyImport(ufvk = validation.ufvk, name = ""))
                    SubmitViewOnlyKeyResult.Accepted
                }
            }
        }

    private suspend fun isAlreadyImported(
        ufvk: String,
        network: ZcashNetwork,
    ): Boolean {
        val existingKeys = existingKeys()
        return withContext(Dispatchers.Default) {
            existingKeys.any { UfvkComponentOverlap.sharesComponent(ufvk, it, ValidateUfvkUseCase.hrpOf(network)) }
        }
    }

    /**
     * When the accounts cannot be read, the import goes on without this check: the import itself
     * still reports a key the SDK finds in the wallet as already imported. The SDK's exception is
     * dropped without its message.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun existingKeys(): List<String> =
        try {
            synchronizerProvider.getSynchronizer().getAccounts().mapNotNull { it.ufvk }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
}

/**
 * Whether two unified full viewing keys share a component, read with the hand-written
 * [UnifiedFullViewingKeyEncoding] until the SDK offers this comparison itself. Kept behind this one
 * function so that swap touches nothing else.
 */
internal object UfvkComponentOverlap {
    private val COMPARED_TYPECODES =
        setOf(
            UnifiedFullViewingKeyEncoding.TYPECODE_P2PKH,
            UnifiedFullViewingKeyEncoding.TYPECODE_SAPLING,
            UnifiedFullViewingKeyEncoding.TYPECODE_ORCHARD,
        )

    /**
     * True when [candidate] and [existing] carry an identical Orchard, Sapling or transparent
     * (P2PKH) item, or are the same text whatever the case. A key that does not decode for [hrp]
     * is compared by its text only.
     */
    fun sharesComponent(
        candidate: String,
        existing: String,
        hrp: String,
    ): Boolean =
        candidate.equals(existing, ignoreCase = true) ||
            sharesItem(
                candidateItems = UnifiedFullViewingKeyEncoding.decode(candidate.lowercase(), hrp).orEmpty(),
                existingItems = UnifiedFullViewingKeyEncoding.decode(existing.lowercase(), hrp).orEmpty(),
            )

    private fun sharesItem(
        candidateItems: List<UnifiedFullViewingKeyEncoding.Item>,
        existingItems: List<UnifiedFullViewingKeyEncoding.Item>,
    ): Boolean =
        candidateItems.any { item ->
            item.typecode in COMPARED_TYPECODES &&
                existingItems.any { it.typecode == item.typecode && it.data.contentEquals(item.data) }
        }
}

enum class SubmitViewOnlyKeyResult {
    Accepted,
    Invalid,
    AlreadyImported,
}
