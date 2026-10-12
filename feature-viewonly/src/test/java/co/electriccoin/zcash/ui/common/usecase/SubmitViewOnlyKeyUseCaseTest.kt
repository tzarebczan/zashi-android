package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.zcash.ui.common.model.ufvk.UfvkFixtures
import co.electriccoin.zcash.ui.common.model.ufvk.UnifiedFullViewingKeyEncoding.Item
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.repository.PendingViewOnlyImport
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepositoryImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A submitted key becomes the pending import only when it is importable and shares no Orchard,
 * Sapling or transparent component with an account the wallet holds: an identical key, a key that
 * contains an existing one, a key an existing one contains and a key that overlaps one all count as
 * already imported, while an unrelated key is accepted. A Sapling component counts as shared when
 * its ak, nk and dk match, whatever its ovk, on mainnet and testnet alike.
 */
class SubmitViewOnlyKeyUseCaseTest {
    private val repository = ViewOnlyPendingImportRepositoryImpl()

    private val fullItems = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL)

    private val full = UfvkFixtures.MAINNET_FULL

    private val saplingAndOrchard = encode(fullItems.filter { it.typecode != P2PKH })

    private val transparentAndOrchard = encode(fullItems.filter { it.typecode != SAPLING })

    private val transparentAndSapling = encode(fullItems.filter { it.typecode != ORCHARD })

    private val unrelated = encode(fullItems.map { item -> Item(item.typecode, item.data.flipped()) })

    @Test
    fun anUnrelatedKeyBecomesThePendingImport() =
        runTest {
            val result = useCase(candidate = unrelated, existingKeys = listOf(full, null)).submit("  input ")

            assertEquals(SubmitViewOnlyKeyResult.Accepted, result)
            assertEquals(PendingViewOnlyImport(ufvk = unrelated, name = ""), repository.get())
        }

    @Test
    fun anIdenticalKeyIsAlreadyImportedWhateverItsCase() =
        runTest {
            assertAlreadyImported(candidate = full, existing = full)
            assertAlreadyImported(candidate = full, existing = full.uppercase())
        }

    @Test
    fun aKeyThatContainsAnExistingKeyIsAlreadyImported() =
        runTest {
            assertAlreadyImported(candidate = full, existing = saplingAndOrchard)
        }

    @Test
    fun aKeyThatAnExistingKeyContainsIsAlreadyImported() =
        runTest {
            assertAlreadyImported(candidate = transparentAndOrchard, existing = full)
        }

    @Test
    fun aKeyThatOverlapsAnExistingKeyIsAlreadyImported() =
        runTest {
            assertAlreadyImported(candidate = transparentAndOrchard, existing = saplingAndOrchard)
            assertAlreadyImported(candidate = transparentAndOrchard, existing = transparentAndSapling)
        }

    @Test
    fun aKeyThatSharesOnlyItsSaplingComponentIsAlreadyImported() =
        runTest {
            assertAlreadyImported(candidate = onlySaplingKept(), existing = full)
        }

    @Test
    fun aSaplingComponentThatDiffersOnlyInItsOutgoingViewingKeyIsAlreadyImported() =
        runTest {
            assertAlreadyImported(candidate = onlySaplingKept(flippedSaplingRange = OVK), existing = full)
        }

    @Test
    fun aSaplingComponentThatDiffersInItsIncomingViewingKeyPartsIsAccepted() =
        runTest {
            listOf(AK, NK, DK).forEach { range ->
                repository.clear()
                val candidate = onlySaplingKept(flippedSaplingRange = range)

                val result = useCase(candidate = candidate, existingKeys = listOf(full)).submit("input")

                assertEquals(SubmitViewOnlyKeyResult.Accepted, result, "flipped bytes $range")
            }
        }

    @Test
    fun anOverlapIsFoundForATestnetKey() =
        runTest {
            val testnetFull = UfvkFixtures.encode(TESTNET_HRP, fullItems)
            val testnetSaplingOnly = UfvkFixtures.encode(TESTNET_HRP, itemsWithOnlySaplingKept(null))

            val result =
                useCase(candidate = testnetSaplingOnly, existingKeys = listOf(unrelated, testnetFull))
                    .invoke("input", ZcashNetwork.Testnet)

            assertEquals(SubmitViewOnlyKeyResult.AlreadyImported, result)
            assertNull(repository.get())
        }

    @Test
    fun anInvalidKeyIsNotKept() =
        runTest {
            val result = useCase(candidate = null, existingKeys = emptyList()).submit("input")

            assertEquals(SubmitViewOnlyKeyResult.Invalid, result)
            assertNull(repository.get())
        }

    @Test
    fun accountsThatCannotBeReadLeaveTheCheckToTheImport() =
        runTest {
            val synchronizer = mockk<Synchronizer> { coEvery { getAccounts() } throws IllegalStateException("db") }
            val result = useCase(candidate = full, synchronizer = synchronizer).submit("input")

            assertEquals(SubmitViewOnlyKeyResult.Accepted, result)
        }

    @Test
    fun aSynchronizerThatNeverLoadsLeavesTheCheckToTheImport() =
        runTest {
            val result =
                SubmitViewOnlyKeyUseCase(
                    validateUfvk =
                        mockk {
                            coEvery { this@mockk.invoke(any(), any()) } returns UfvkValidation.Valid(full)
                        },
                    synchronizerProvider =
                        mockk<SynchronizerProvider> { coEvery { getSynchronizer() } coAnswers { awaitCancellation() } },
                    pendingImportRepository = repository,
                ).submit("input")

            assertEquals(SubmitViewOnlyKeyResult.Accepted, result)
        }

    private suspend fun assertAlreadyImported(
        candidate: String,
        existing: String,
    ) {
        repository.clear()
        val result = useCase(candidate = candidate, existingKeys = listOf(unrelated, existing)).submit("input")

        assertEquals(SubmitViewOnlyKeyResult.AlreadyImported, result)
        assertNull(repository.get())
    }

    private fun useCase(
        candidate: String?,
        existingKeys: List<String?> = emptyList(),
        synchronizer: Synchronizer =
            mockk { coEvery { getAccounts() } returns existingKeys.map(::account) },
    ) = SubmitViewOnlyKeyUseCase(
        validateUfvk =
            mockk {
                coEvery { this@mockk.invoke(any(), any()) } returns
                    (candidate?.let { UfvkValidation.Valid(it) } ?: UfvkValidation.Invalid)
            },
        synchronizerProvider = mockk<SynchronizerProvider> { coEvery { getSynchronizer() } returns synchronizer },
        pendingImportRepository = repository,
    )

    private suspend fun SubmitViewOnlyKeyUseCase.submit(input: String) = invoke(input, ZcashNetwork.Mainnet)

    private fun account(ufvk: String?): Account = mockk { every { this@mockk.ufvk } returns ufvk }

    private fun encode(items: List<Item>) = UfvkFixtures.encode("uview", items)

    /**
     * [full] with its Orchard and transparent items replaced and its Sapling item kept, except for
     * the bytes in [flippedSaplingRange].
     */
    private fun onlySaplingKept(flippedSaplingRange: IntRange? = null) =
        encode(itemsWithOnlySaplingKept(flippedSaplingRange))

    private fun itemsWithOnlySaplingKept(flippedSaplingRange: IntRange?) =
        fullItems.map { item ->
            if (item.typecode == SAPLING) {
                Item(
                    item.typecode,
                    ByteArray(item.data.size) { index ->
                        val byte = item.data[index]
                        if (flippedSaplingRange != null && index in flippedSaplingRange) {
                            (byte.toInt() xor 0xFF).toByte()
                        } else {
                            byte
                        }
                    }
                )
            } else {
                Item(item.typecode, item.data.flipped())
            }
        }

    private fun ByteArray.flipped() = ByteArray(size) { (this[it].toInt() xor 0xFF).toByte() }

    private companion object {
        const val P2PKH = 0x00
        const val SAPLING = 0x02
        const val ORCHARD = 0x03

        const val TESTNET_HRP = "uviewtest"

        val AK = 0 until 32
        val NK = 32 until 64
        val OVK = 64 until 96
        val DK = 96 until 128
    }
}
