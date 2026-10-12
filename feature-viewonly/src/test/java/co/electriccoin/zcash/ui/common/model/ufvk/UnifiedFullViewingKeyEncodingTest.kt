package co.electriccoin.zcash.ui.common.model.ufvk

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The pure-Kotlin reading of a unified full viewing key against published vectors: BLAKE2b
 * (RFC 7693), F4Jumble (the f4jumble crate's documented example) and the librustzcash UFVK
 * encodings, whose padding check only passes when both primitives are right.
 */
class UnifiedFullViewingKeyEncodingTest {
    @Test
    fun blake2bMatchesTheRfcVector() {
        assertEquals(
            "ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d1" +
                "7d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923",
            Blake2b.hash(64, ByteArray(0), "abc".toByteArray()).toHex()
        )
    }

    @Test
    fun blake2bOfTheEmptyInputMatchesTheKnownDigest() {
        assertEquals(
            "786a02f742015903c6c6fd852552d272912f4740e15847618a86e217f71f5419" +
                "d25e1031afee585313896444934eb04b903a685b1448b755d56f701afe9be2ce",
            Blake2b.hash(64, ByteArray(0), ByteArray(0)).toHex()
        )
    }

    @Test
    fun f4JumbleMatchesTheCrateExampleBothWays() {
        val message = "The package from Alice arrives tomorrow morning.".toByteArray()
        val jumbled = "861c51ee746b0313476967a3483e7e1ff77a2952a17d3ed9e0ab0f502e1179430322da9967b613545b1c36353046ca27"

        assertEquals(jumbled, F4Jumble.jumble(message).toHex())
        assertContentEquals(message, F4Jumble.unjumble(jumbled.fromHex()))
    }

    @Test
    fun theFullVectorHoldsTransparentSaplingAndOrchard() {
        val items = assertNotNull(UnifiedFullViewingKeyEncoding.decode(UfvkFixtures.MAINNET_FULL, "uview"))

        assertEquals(listOf(0x00, 0x02, 0x03), items.map { it.typecode })
        assertEquals(listOf(65, 128, 96), items.map { it.data.size })
    }

    @Test
    fun theVectorWithoutTransparentHoldsSaplingAndOrchard() {
        val items =
            assertNotNull(UnifiedFullViewingKeyEncoding.decode(UfvkFixtures.MAINNET_WITHOUT_TRANSPARENT, "uview"))

        assertEquals(listOf(0x02, 0x03), items.map { it.typecode })
    }

    @Test
    fun reEncodingTheItemsGivesTheSameKey() {
        assertEquals(
            UfvkFixtures.MAINNET_FULL,
            UfvkFixtures.encode("uview", UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL))
        )
    }

    @Test
    fun anotherHumanReadablePartIsRejected() {
        assertNull(UnifiedFullViewingKeyEncoding.decode(UfvkFixtures.MAINNET_FULL, "uviewtest"))
        assertNull(UnifiedFullViewingKeyEncoding.decode(UfvkFixtures.MAINNET_UIVK, "uview"))
    }

    @Test
    fun aBrokenChecksumIsRejected() {
        val last = UfvkFixtures.MAINNET_FULL.last()
        val broken = UfvkFixtures.MAINNET_FULL.dropLast(1) + if (last == 'q') 'p' else 'q'

        assertNull(UnifiedFullViewingKeyEncoding.decode(broken, "uview"))
    }

    @Test
    fun paddingOfAnotherNetworkIsRejected() {
        val items = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL)
        val testnetPaddedAsMainnet =
            Bech32m.encode(
                "uview",
                F4Jumble.jumble(
                    items
                        .flatMap { listOf(it.typecode.toByte(), it.data.size.toByte()) + it.data.toList() }
                        .toByteArray() + UnifiedFullViewingKeyEncoding.padding("uviewtest")
                )
            )

        assertNull(UnifiedFullViewingKeyEncoding.decode(testnetPaddedAsMainnet, "uview"))
    }

    @Test
    fun outOfOrderOrDuplicateItemsAreRejected() {
        val items = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL)

        assertNull(UnifiedFullViewingKeyEncoding.decode(UfvkFixtures.encode("uview", items.reversed()), "uview"))
        assertNull(
            UnifiedFullViewingKeyEncoding.decode(UfvkFixtures.encode("uview", items + items.last()), "uview")
        )
    }

    @Test
    fun aKnownItemOfTheWrongSizeIsRejected() {
        val items = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL)
        val truncatedOrchard = items.dropLast(1) + UnifiedFullViewingKeyEncoding.Item(0x03, ByteArray(95))

        assertNull(UnifiedFullViewingKeyEncoding.decode(UfvkFixtures.encode("uview", truncatedOrchard), "uview"))
    }

    @Test
    fun anItemNeverPrintsItsKeyBytes() {
        val item = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL).first()

        assertEquals("Item(typecode=0, length=65)", item.toString())
    }

    @Test
    fun garbageIsRejected() {
        listOf("", "uview1", "uview1qqqqqq", "not a key", "u1abcdefghijkl").forEach { text ->
            assertNull(UnifiedFullViewingKeyEncoding.decode(text, "uview"), text)
        }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun String.fromHex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
