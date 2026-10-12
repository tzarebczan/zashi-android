package co.electriccoin.zcash.ui.common.model.ufvk

/**
 * Unkeyed BLAKE2b (RFC 7693) with a variable digest length and a 16-byte personalization, the only
 * form F4Jumble needs. No salt and no key.
 */
@Suppress("MagicNumber")
internal object Blake2b {
    private const val BLOCK_BYTES = 128
    const val MAX_OUT_BYTES = 64
    private const val PERSONAL_BYTES = 16
    private const val ROUNDS = 12

    private val IV =
        longArrayOf(
            0x6a09e667f3bcc908L,
            -0x4498517a7b3558c5L,
            0x3c6ef372fe94f82bL,
            -0x5ab00ac5a0e2c90fL,
            0x510e527fade682d1L,
            -0x64fa9773d4c193e1L,
            0x1f83d9abfb41bd6bL,
            0x5be0cd19137e2179L,
        )

    private val SIGMA =
        arrayOf(
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            intArrayOf(14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3),
            intArrayOf(11, 8, 12, 0, 5, 2, 15, 13, 10, 14, 3, 6, 7, 1, 9, 4),
            intArrayOf(7, 9, 3, 1, 13, 12, 11, 14, 2, 6, 5, 10, 4, 0, 15, 8),
            intArrayOf(9, 0, 5, 7, 2, 4, 10, 15, 14, 1, 11, 12, 6, 8, 3, 13),
            intArrayOf(2, 12, 6, 10, 0, 11, 8, 3, 4, 13, 7, 5, 15, 14, 1, 9),
            intArrayOf(12, 5, 1, 15, 14, 13, 4, 10, 0, 7, 6, 3, 9, 2, 8, 11),
            intArrayOf(13, 11, 7, 14, 12, 1, 3, 9, 5, 0, 15, 4, 8, 6, 2, 10),
            intArrayOf(6, 15, 14, 9, 11, 3, 0, 8, 12, 2, 13, 7, 1, 4, 10, 5),
            intArrayOf(10, 2, 8, 4, 7, 6, 1, 5, 15, 11, 9, 14, 3, 12, 13, 0),
        )

    /**
     * The [outLength]-byte BLAKE2b digest of [input] personalized with [personal], which is either
     * empty or exactly 16 bytes.
     */
    fun hash(
        outLength: Int,
        personal: ByteArray,
        input: ByteArray,
    ): ByteArray {
        require(outLength in 1..MAX_OUT_BYTES)
        require(personal.isEmpty() || personal.size == PERSONAL_BYTES)

        val h = IV.copyOf()
        h[0] = h[0] xor (0x01010000L or outLength.toLong())
        if (personal.isNotEmpty()) {
            h[6] = h[6] xor readLong(personal, 0)
            h[7] = h[7] xor readLong(personal, 8)
        }

        val blockCount = maxOf(1, (input.size + BLOCK_BYTES - 1) / BLOCK_BYTES)
        val block = ByteArray(BLOCK_BYTES)
        for (index in 0 until blockCount) {
            val offset = index * BLOCK_BYTES
            val length = minOf(BLOCK_BYTES, input.size - offset)
            block.fill(0)
            if (length > 0) input.copyInto(block, 0, offset, offset + length)
            val isLast = index == blockCount - 1
            compress(h, block, (offset + maxOf(length, 0)).toLong(), isLast)
        }

        val out = ByteArray(outLength)
        for (i in 0 until outLength) {
            out[i] = (h[i / 8] ushr (8 * (i % 8))).toByte()
        }
        return out
    }

    private fun compress(
        h: LongArray,
        block: ByteArray,
        counter: Long,
        isLast: Boolean,
    ) {
        val m = LongArray(16) { readLong(block, it * 8) }
        val v = LongArray(16)
        h.copyInto(v, 0)
        IV.copyInto(v, 8)
        v[12] = v[12] xor counter
        if (isLast) v[14] = v[14].inv()

        for (round in 0 until ROUNDS) {
            val s = SIGMA[round % SIGMA.size]
            mix(v, 0, 4, 8, 12, m[s[0]], m[s[1]])
            mix(v, 1, 5, 9, 13, m[s[2]], m[s[3]])
            mix(v, 2, 6, 10, 14, m[s[4]], m[s[5]])
            mix(v, 3, 7, 11, 15, m[s[6]], m[s[7]])
            mix(v, 0, 5, 10, 15, m[s[8]], m[s[9]])
            mix(v, 1, 6, 11, 12, m[s[10]], m[s[11]])
            mix(v, 2, 7, 8, 13, m[s[12]], m[s[13]])
            mix(v, 3, 4, 9, 14, m[s[14]], m[s[15]])
        }

        for (i in 0 until 8) {
            h[i] = h[i] xor v[i] xor v[i + 8]
        }
    }

    @Suppress("LongParameterList")
    private fun mix(
        v: LongArray,
        a: Int,
        b: Int,
        c: Int,
        d: Int,
        x: Long,
        y: Long,
    ) {
        v[a] = v[a] + v[b] + x
        v[d] = (v[d] xor v[a]).rotateRight(32)
        v[c] = v[c] + v[d]
        v[b] = (v[b] xor v[c]).rotateRight(24)
        v[a] = v[a] + v[b] + y
        v[d] = (v[d] xor v[a]).rotateRight(16)
        v[c] = v[c] + v[d]
        v[b] = (v[b] xor v[c]).rotateRight(63)
    }

    private fun readLong(
        bytes: ByteArray,
        offset: Int,
    ): Long {
        var result = 0L
        for (i in 7 downTo 0) {
            result = (result shl 8) or (bytes[offset + i].toLong() and 0xFF)
        }
        return result
    }
}
