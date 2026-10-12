package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.zcash.ui.common.model.ufvk.UfvkFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Only a full viewing key for the wallet's network with both an Orchard and a transparent component,
 * and one the SDK accepts, is importable. Everything else is rejected without reaching the SDK, and
 * no outcome prints the key.
 */
class ValidateUfvkUseCaseTest {
    private val derivedKeys = mutableListOf<String>()

    private var sdkAccepts = true

    private val useCase =
        ValidateUfvkUseCase(
            object : UfvkAddressDerivation {
                override suspend fun canDeriveAddress(
                    ufvk: String,
                    network: ZcashNetwork,
                ): Boolean {
                    derivedKeys += ufvk
                    return sdkAccepts
                }
            }
        )

    @Test
    fun aMainnetKeyWithEveryComponentIsValidOnMainnet() =
        runTest {
            val result = useCase(UfvkFixtures.MAINNET_FULL, ZcashNetwork.Mainnet)

            assertEquals(UfvkFixtures.MAINNET_FULL, assertIs<UfvkValidation.Valid>(result).ufvk)
            assertEquals(listOf(UfvkFixtures.MAINNET_FULL), derivedKeys)
        }

    @Test
    fun surroundingWhitespaceAndUpperCaseAreAccepted() =
        runTest {
            val result = useCase("  \n" + UfvkFixtures.MAINNET_FULL.uppercase() + "\t ", ZcashNetwork.Mainnet)

            assertEquals(UfvkFixtures.MAINNET_FULL, assertIs<UfvkValidation.Valid>(result).ufvk)
        }

    @Test
    fun aKeyForTheOtherNetworkIsRejected() =
        runTest {
            val testnet = UfvkFixtures.encode("uviewtest", UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL))

            assertInvalid(useCase(testnet, ZcashNetwork.Mainnet))
            assertInvalid(useCase(UfvkFixtures.MAINNET_FULL, ZcashNetwork.Testnet))
            assertTrue(derivedKeys.isEmpty())
        }

    @Test
    fun aTestnetKeyIsValidOnTestnet() =
        runTest {
            val testnet = UfvkFixtures.encode("uviewtest", UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL))

            assertIs<UfvkValidation.Valid>(useCase(testnet, ZcashNetwork.Testnet))
        }

    @Test
    fun aKeyWithoutATransparentComponentIsRejected() =
        runTest {
            assertInvalid(useCase(UfvkFixtures.MAINNET_WITHOUT_TRANSPARENT, ZcashNetwork.Mainnet))
            assertTrue(derivedKeys.isEmpty())
        }

    @Test
    fun aKeyWithoutAnOrchardComponentIsRejected() =
        runTest {
            val withoutOrchard =
                UfvkFixtures.encode(
                    "uview",
                    UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL).filterNot { it.typecode == ORCHARD }
                )

            assertInvalid(useCase(withoutOrchard, ZcashNetwork.Mainnet))
            assertTrue(derivedKeys.isEmpty())
        }

    @Test
    fun aSaplingOnlyKeyIsRejected() =
        runTest {
            val saplingOnly =
                UfvkFixtures.encode(
                    "uview",
                    UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL).filter { it.typecode == SAPLING }
                )

            assertInvalid(useCase(saplingOnly, ZcashNetwork.Mainnet))
        }

    @Test
    fun anIncomingViewingKeyIsRejected() =
        runTest {
            assertInvalid(useCase(UfvkFixtures.MAINNET_UIVK, ZcashNetwork.Mainnet))
            assertTrue(derivedKeys.isEmpty())
        }

    @Test
    fun garbageIsRejected() =
        runTest {
            listOf(
                "",
                "   ",
                "uview1",
                "hello",
                "u1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq",
                UfvkFixtures.MAINNET_FULL.dropLast(1),
                UfvkFixtures.MAINNET_FULL + "q",
            ).forEach { input ->
                assertInvalid(useCase(input, ZcashNetwork.Mainnet))
            }
            assertTrue(derivedKeys.isEmpty())
        }

    @Test
    fun aKeyTheSdkRejectsIsRejected() =
        runTest {
            sdkAccepts = false

            assertInvalid(useCase(UfvkFixtures.MAINNET_FULL, ZcashNetwork.Mainnet))
        }

    @Test
    fun theValidOutcomeNeverPrintsTheKey() =
        runTest {
            val result = useCase(UfvkFixtures.MAINNET_FULL, ZcashNetwork.Mainnet)

            assertFalse(result.toString().contains("uview"))
        }

    @Test
    fun theKeyPrefixFollowsTheNetwork() {
        assertEquals("uview1", ValidateUfvkUseCase.keyPrefixOf(ZcashNetwork.Mainnet))
        assertEquals("uviewtest1", ValidateUfvkUseCase.keyPrefixOf(ZcashNetwork.Testnet))
    }

    private fun assertInvalid(result: UfvkValidation) = assertEquals(UfvkValidation.Invalid, result)

    private companion object {
        const val SAPLING = 0x02
        const val ORCHARD = 0x03
    }
}
