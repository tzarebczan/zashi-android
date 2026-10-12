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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Takes the viewing key the user pasted or scanned: an importable key the wallet does not hold yet
 * becomes the pending import, to be named on the next screen. The key goes to memory only.
 *
 * A key counts as held already when it shares any Orchard, Sapling (at the incoming-viewing-key
 * level) or transparent component with an account in the wallet, not only when it is the same
 * text: the SDK would otherwise silently
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
            UfvkComponentOverlap.sharesComponentWithAny(ufvk, existingKeys, ValidateUfvkUseCase.hrpOf(network))
        }
    }

    /**
     * When the accounts cannot be read, or no synchronizer is ready within
     * [SYNCHRONIZER_TIMEOUT], the import goes on without this check: the import itself still
     * reports a key the SDK finds in the wallet as already imported. The SDK's exception is dropped
     * without its message.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun existingKeys(): List<String> =
        try {
            withTimeoutOrNull(SYNCHRONIZER_TIMEOUT) { synchronizerProvider.getSynchronizer() }
                ?.getAccounts()
                ?.mapNotNull { it.ufvk }
                .orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }

    private companion object {
        val SYNCHRONIZER_TIMEOUT = 5.seconds
    }
}

/**
 * Whether a unified full viewing key shares a component with any key the wallet holds, read with
 * the hand-written [UnifiedFullViewingKeyEncoding] until the SDK offers this comparison itself.
 * Kept behind this one function so that swap touches nothing else.
 */
internal object UfvkComponentOverlap {
    private const val SAPLING_AK_NK_END = 64
    private const val SAPLING_DK_START = 96

    private val COMPARED_TYPECODES =
        setOf(
            UnifiedFullViewingKeyEncoding.TYPECODE_P2PKH,
            UnifiedFullViewingKeyEncoding.TYPECODE_SAPLING,
            UnifiedFullViewingKeyEncoding.TYPECODE_ORCHARD,
        )

    /**
     * True when [candidate] and one of [existingKeys] carry the same Orchard, Sapling or transparent
     * (P2PKH) component, or are the same text whatever the case. A key that does not decode for
     * [hrp] is compared by its text only. [candidate] is decoded once for the whole list.
     */
    fun sharesComponentWithAny(
        candidate: String,
        existingKeys: List<String>,
        hrp: String,
    ): Boolean {
        val candidateComponents = components(candidate, hrp)
        return existingKeys.any { existing ->
            candidate.equals(existing, ignoreCase = true) ||
                components(existing, hrp).any { component -> candidateComponents.any { it.sameAs(component) } }
        }
    }

    private fun UnifiedFullViewingKeyEncoding.Item.sameAs(other: UnifiedFullViewingKeyEncoding.Item) =
        typecode == other.typecode && data.contentEquals(other.data)

    private fun components(
        key: String,
        hrp: String,
    ): List<UnifiedFullViewingKeyEncoding.Item> =
        UnifiedFullViewingKeyEncoding
            .decode(key.lowercase(), hrp)
            .orEmpty()
            .filter { it.typecode in COMPARED_TYPECODES }
            .map { UnifiedFullViewingKeyEncoding.Item(it.typecode, comparedBytes(it)) }

    /**
     * Orchard and transparent items are compared whole. A Sapling item (ak, nk, ovk, dk, 32 bytes
     * each, as sapling-crypto's DiversifiableFullViewingKey::to_bytes writes it) is compared at the
     * incoming-viewing-key level, ak, nk and dk, without ovk: the SDK finds an existing account by
     * those, so a key that differs from one in its ovk alone would still replace that account.
     */
    private fun comparedBytes(item: UnifiedFullViewingKeyEncoding.Item): ByteArray =
        if (item.typecode == UnifiedFullViewingKeyEncoding.TYPECODE_SAPLING) {
            item.data.copyOfRange(0, SAPLING_AK_NK_END) +
                item.data.copyOfRange(SAPLING_DK_START, item.data.size)
        } else {
            item.data
        }
}

enum class SubmitViewOnlyKeyResult {
    Accepted,
    Invalid,
    AlreadyImported,
}
