package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import co.electriccoin.zcash.ui.common.model.VersionInfo
import co.electriccoin.zcash.ui.common.model.ufvk.UnifiedFullViewingKeyEncoding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decides whether text the user pasted or scanned is a unified full viewing key this wallet can
 * import as a view-only account.
 *
 * Accepted is only a key for the wallet's own network (`uview1` on mainnet, `uviewtest1` on
 * testnet) that carries both an Orchard and a transparent component, and that the SDK can derive
 * an address from. An incoming viewing key (`uivk`), any other text, a key for the other network
 * and a key missing either component are rejected: the account list combines one address flow per
 * account and waits for every one of them, and the Orchard or transparent address of a key without
 * that component would never arrive, which would stall every account in the wallet.
 *
 * Every whitespace character in the input, line breaks and zero-width spaces included, is dropped
 * first, since a key copied from an email or a PDF is often broken across lines. What remains has
 * to be all lower case or all upper case, as Bech32m requires; a mixed-case key is rejected. An
 * accepted key is returned in its canonical lower-case form.
 *
 * The input is never logged or echoed, and an SDK failure is reduced to a rejection without its
 * message, which can contain the key.
 */
internal class ValidateUfvkUseCase(
    private val ufvkAddressDerivation: UfvkAddressDerivation,
) {
    suspend operator fun invoke(
        input: String,
        network: ZcashNetwork = VersionInfo.NETWORK,
    ): UfvkValidation {
        val compact = input.filterNot { it.isWhitespace() || it in INVISIBLE_CHARACTERS }
        val isSingleCase = compact == compact.lowercase() || compact == compact.uppercase()
        val candidate = compact.lowercase()
        val items =
            if (isSingleCase && candidate.startsWith(keyPrefixOf(network))) {
                withContext(Dispatchers.Default) { UnifiedFullViewingKeyEncoding.decode(candidate, hrpOf(network)) }
            } else {
                null
            }
        val typecodes = items.orEmpty().map { it.typecode }.toSet()
        val isValid =
            UnifiedFullViewingKeyEncoding.TYPECODE_ORCHARD in typecodes &&
                UnifiedFullViewingKeyEncoding.TYPECODE_P2PKH in typecodes &&
                ufvkAddressDerivation.canDeriveAddress(candidate, network)
        return if (isValid && items != null) UfvkValidation.Valid(candidate, items) else UfvkValidation.Invalid
    }

    companion object {
        private const val MAINNET_HRP = "uview"
        private const val TESTNET_HRP = "uviewtest"
        private const val SEPARATOR = "1"

        /**
         * Zero-width characters that [Char.isWhitespace] does not cover but copied text often
         * carries: zero-width space, non-joiner and joiner, word joiner and the byte-order mark.
         */
        private val INVISIBLE_CHARACTERS = setOf('\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF')

        /** How every full viewing key for [network] starts, such as `uview1` on mainnet. */
        fun keyPrefixOf(network: ZcashNetwork) = hrpOf(network) + SEPARATOR

        /** The human-readable part of a full viewing key for [network], such as `uview` on mainnet. */
        fun hrpOf(network: ZcashNetwork) = if (network == ZcashNetwork.Mainnet) MAINNET_HRP else TESTNET_HRP
    }
}

/**
 * The outcome of [ValidateUfvkUseCase].
 */
sealed interface UfvkValidation {
    /**
     * [ufvk] is importable; [items] are its decoded components, so later checks need not decode it
     * again. [toString] leaves the key and its components out.
     */
    class Valid internal constructor(
        val ufvk: String,
        internal val items: List<UnifiedFullViewingKeyEncoding.Item>,
    ) : UfvkValidation {
        override fun toString() = "Valid(ufvk=***)"
    }

    data object Invalid : UfvkValidation
}

/**
 * The SDK's last word on a viewing key that is well formed: whether it can derive an address from
 * it, which also checks that the keys inside are valid curve points. A seam so that the use case can
 * be tested without the native library.
 */
internal interface UfvkAddressDerivation {
    suspend fun canDeriveAddress(
        ufvk: String,
        network: ZcashNetwork,
    ): Boolean
}

internal class SdkUfvkAddressDerivation : UfvkAddressDerivation {
    /**
     * The SDK's exception is swallowed on purpose: its message can echo the key.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun canDeriveAddress(
        ufvk: String,
        network: ZcashNetwork,
    ): Boolean =
        withContext(Dispatchers.Default) {
            try {
                DerivationTool.getInstance().deriveUnifiedAddress(viewingKey = ufvk, network = network).isNotBlank()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        }
}
