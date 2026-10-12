package co.electriccoin.zcash.ui.common.model.ufvk

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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

    @Test
    fun theSameKeyWithAPlainBech32ChecksumIsRejected() {
        val bech32 = withBech32Checksum(UfvkFixtures.MAINNET_FULL)

        assertNotEquals(UfvkFixtures.MAINNET_FULL, bech32)
        assertEquals(UfvkFixtures.MAINNET_FULL.dropLast(6), bech32.dropLast(6))
        assertNull(UnifiedFullViewingKeyEncoding.decode(bech32, "uview"))
    }

    @Test
    fun aNonMinimalCompactSizeIsRejected() {
        val items = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL)
        val minimal = body(items)
        val orchard = items.last()
        val nonMinimalTypecode =
            body(items.dropLast(1)) + byteArrayOf(0xFD.toByte(), 0x03, 0x00, orchard.data.size.toByte()) + orchard.data

        assertNotNull(UnifiedFullViewingKeyEncoding.decode(encodeRaw("uview", minimal), "uview"))
        assertNull(UnifiedFullViewingKeyEncoding.decode(encodeRaw("uview", nonMinimalTypecode), "uview"))
    }

    @Test
    fun anItemWhoseLengthNeedsThreeBytesIsRead() {
        val items = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL)
        val long = ByteArray(LONG_ITEM_LENGTH) { it.toByte() }
        val raw =
            body(items) + byteArrayOf(UNKNOWN_TYPECODE.toByte(), 0xFD.toByte(), LONG_ITEM_LENGTH.toByte(), 0x00) + long

        val decoded = assertNotNull(UnifiedFullViewingKeyEncoding.decode(encodeRaw("uview", raw), "uview"))

        assertEquals(listOf(0x00, 0x02, 0x03, UNKNOWN_TYPECODE), decoded.map { it.typecode })
        assertContentEquals(long, decoded.last().data)
    }

    @Test
    fun anUnknownTypecodeInOrderIsAccepted() {
        val items = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL)
        val withUnknown = items + UnifiedFullViewingKeyEncoding.Item(UNKNOWN_TYPECODE, ByteArray(5) { 7 })

        val decoded =
            assertNotNull(UnifiedFullViewingKeyEncoding.decode(UfvkFixtures.encode("uview", withUnknown), "uview"))

        assertEquals(listOf(0x00, 0x02, 0x03, UNKNOWN_TYPECODE), decoded.map { it.typecode })
    }

    @Test
    fun aP2shItemIsRejected() {
        val items = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL)
        val withP2sh =
            listOf(UnifiedFullViewingKeyEncoding.Item(UnifiedFullViewingKeyEncoding.TYPECODE_P2SH, ByteArray(20))) +
                items.drop(1)

        assertNull(UnifiedFullViewingKeyEncoding.decode(UfvkFixtures.encode("uview", withP2sh), "uview"))
    }

    @Test
    fun aHumanReadablePartLongerThanThePaddingIsRejected() {
        val hrp = "uviewlongerthan16"
        val items = UfvkFixtures.itemsOf(UfvkFixtures.MAINNET_FULL)
        val text = Bech32m.encode(hrp, F4Jumble.jumble(body(items) + hrp.toByteArray().copyOf(16)))

        assertNull(UnifiedFullViewingKeyEncoding.decode(text, hrp))
    }

    /**
     * Re-checksums a Bech32m string with the original Bech32 constant (1): both checksums are the
     * same polynomial residue XORed with their constant, so swapping the constant swaps the scheme.
     */
    private fun withBech32Checksum(bech32m: String): String {
        val charset = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
        val checksum = bech32m.takeLast(6).fold(0) { acc, c -> (acc shl 5) or charset.indexOf(c) }
        val swapped = checksum xor BECH32M_CONSTANT xor 1
        val swappedChars = (5 downTo 0).joinToString("") { charset[(swapped ushr (5 * it)) and 31].toString() }
        return bech32m.dropLast(6) + swappedChars
    }

    private fun body(items: List<UnifiedFullViewingKeyEncoding.Item>) =
        items
            .flatMap { listOf(it.typecode.toByte(), it.data.size.toByte()) + it.data.toList() }
            .toByteArray()

    private fun encodeRaw(
        hrp: String,
        body: ByteArray,
    ) = Bech32m.encode(hrp, F4Jumble.jumble(body + UnifiedFullViewingKeyEncoding.padding(hrp)))

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun String.fromHex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private companion object {
        const val BECH32M_CONSTANT = 0x2bc830a3
        const val UNKNOWN_TYPECODE = 0x10
        const val LONG_ITEM_LENGTH = 0xFD
    }
}
