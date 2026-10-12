package co.electriccoin.zcash.ui.common.model.ufvk

/**
 * Reads the structure of a unified full viewing key as ZIP 316 encodes it: Bech32m over the
 * F4Jumbled concatenation of typecode-length-value items and a padding block that repeats the
 * human-readable part. It checks the encoding and the item framing only; whether the keys inside
 * are valid points is for the SDK to judge.
 *
 * Nothing here logs or echoes its input.
 */
@Suppress("MagicNumber")
internal object UnifiedFullViewingKeyEncoding {
    const val PADDING_LENGTH = 16

    const val TYPECODE_P2PKH = 0x00
    const val TYPECODE_P2SH = 0x01
    const val TYPECODE_SAPLING = 0x02
    const val TYPECODE_ORCHARD = 0x03

    private val KNOWN_LENGTHS =
        mapOf(
            TYPECODE_P2PKH to 65,
            TYPECODE_SAPLING to 128,
            TYPECODE_ORCHARD to 96,
        )

    /**
     * One typecode-length-value item of a unified encoding. [toString] leaves out the key bytes.
     */
    class Item(
        val typecode: Int,
        val data: ByteArray,
    ) {
        override fun toString() = "Item(typecode=$typecode, length=${data.size})"
    }

    /**
     * The items of [text] in their encoded order, or null when [text] is not a well-formed unified
     * full viewing key for [hrp]: a bad checksum, a different human-readable part, wrong padding,
     * broken framing, duplicate or out-of-order typecodes, a P2SH item (which a viewing key cannot
     * hold), or a known item of the wrong size.
     */
    fun decode(
        text: String,
        hrp: String,
    ): List<Item>? {
        val jumbled =
            Bech32m
                .decode(text)
                ?.takeIf { (decodedHrp, payload) ->
                    decodedHrp == hrp &&
                        hrp.length <= PADDING_LENGTH &&
                        payload.size in F4Jumble.MIN_LENGTH..F4Jumble.MAX_LENGTH
                }?.second
        val raw =
            jumbled
                ?.let(F4Jumble::unjumble)
                ?.takeIf { it.copyOfRange(it.size - PADDING_LENGTH, it.size).contentEquals(padding(hrp)) }
        return raw?.let { parseItems(it.copyOfRange(0, it.size - PADDING_LENGTH)) }
    }

    /**
     * The 16 bytes every unified encoding for [hrp] ends with before it is jumbled.
     */
    fun padding(hrp: String): ByteArray {
        val padding = ByteArray(PADDING_LENGTH)
        hrp.toByteArray(Charsets.US_ASCII).copyInto(padding)
        return padding
    }

    private fun parseItems(body: ByteArray): List<Item>? {
        val items = mutableListOf<Item>()
        var offset = 0
        var isValid = true
        while (isValid && offset < body.size) {
            val next = readItem(body, offset, items.lastOrNull()?.typecode)
            if (next == null) {
                isValid = false
            } else {
                items += next.first
                offset = next.second
            }
        }
        return items.takeIf { isValid && it.isNotEmpty() }
    }

    /**
     * The item at [offset] and the offset after it, or null when it is truncated, a P2SH item, not
     * after [previousTypecode] in typecode order, or a known item of the wrong size.
     */
    private fun readItem(
        body: ByteArray,
        offset: Int,
        previousTypecode: Int?,
    ): Pair<Item, Int>? {
        val typecode = readCompactSize(body, offset)
        val length = typecode?.let { readCompactSize(body, it.second) }
        if (typecode == null || length == null) return null
        val (code, _) = typecode
        val (size, start) = length
        val isValid =
            size <= body.size - start &&
                code <= Int.MAX_VALUE &&
                code.toInt() != TYPECODE_P2SH &&
                (previousTypecode == null || code > previousTypecode) &&
                KNOWN_LENGTHS[code.toInt()]?.let { it.toLong() == size } != false
        return if (isValid) {
            Item(code.toInt(), body.copyOfRange(start, start + size.toInt())) to start + size.toInt()
        } else {
            null
        }
    }

    /**
     * The Bitcoin-style CompactSize at [offset] and the offset after it, or null when it is
     * truncated or not minimally encoded.
     */
    private fun readCompactSize(
        bytes: ByteArray,
        offset: Int,
    ): Pair<Long, Int>? {
        val first = bytes.getOrNull(offset)?.toInt()?.and(0xFF)
        val width =
            when (first) {
                0xFD -> 2
                0xFE -> 4
                0xFF -> 8
                else -> 0
            }
        val value =
            when {
                first == null -> null
                width == 0 -> first.toLong()
                offset + 1 + width > bytes.size -> null
                else -> readLittleEndian(bytes, offset + 1, width).takeIf { it >= minimumOf(width) }
            }
        return value?.let { it to offset + 1 + width }
    }

    private fun readLittleEndian(
        bytes: ByteArray,
        offset: Int,
        width: Int,
    ): Long {
        var value = 0L
        for (i in width - 1 downTo 0) {
            value = (value shl 8) or (bytes[offset + i].toLong() and 0xFF)
        }
        return value
    }

    private fun minimumOf(width: Int) =
        when (width) {
            2 -> 0xFDL
            4 -> 0x10000L
            else -> 0x100000000L
        }
}
