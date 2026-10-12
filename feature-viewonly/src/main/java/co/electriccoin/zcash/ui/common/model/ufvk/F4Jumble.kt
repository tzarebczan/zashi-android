package co.electriccoin.zcash.ui.common.model.ufvk

/**
 * F4Jumble and its inverse as ZIP 316 specifies them for unified encodings: a four-round Feistel
 * construction over BLAKE2b that makes every bit of the encoding depend on every other bit.
 */
@Suppress("MagicNumber")
internal object F4Jumble {
    const val MIN_LENGTH = 48
    const val MAX_LENGTH = 4_194_368

    private val H_PREFIX = "UA_F4Jumble_H".toByteArray(Charsets.US_ASCII)
    private val G_PREFIX = "UA_F4Jumble_G".toByteArray(Charsets.US_ASCII)

    fun jumble(message: ByteArray): ByteArray {
        requireValidLength(message)
        val (left, right) = split(message)
        gRound(0, left, right)
        hRound(0, left, right)
        gRound(1, left, right)
        hRound(1, left, right)
        return left + right
    }

    fun unjumble(message: ByteArray): ByteArray {
        requireValidLength(message)
        val (left, right) = split(message)
        hRound(1, left, right)
        gRound(1, left, right)
        hRound(0, left, right)
        gRound(0, left, right)
        return left + right
    }

    private fun requireValidLength(message: ByteArray) =
        require(message.size in MIN_LENGTH..MAX_LENGTH) { "Invalid F4Jumble length" }

    private fun split(message: ByteArray): Pair<ByteArray, ByteArray> {
        val leftLength = minOf(Blake2b.MAX_OUT_BYTES, message.size / 2)
        return message.copyOfRange(0, leftLength) to message.copyOfRange(leftLength, message.size)
    }

    private fun hRound(
        i: Int,
        left: ByteArray,
        right: ByteArray,
    ) {
        val personal = H_PREFIX + byteArrayOf(i.toByte(), 0, 0)
        xorInto(left, 0, Blake2b.hash(left.size, personal, right))
    }

    private fun gRound(
        i: Int,
        left: ByteArray,
        right: ByteArray,
    ) {
        val chunks = (right.size + Blake2b.MAX_OUT_BYTES - 1) / Blake2b.MAX_OUT_BYTES
        for (j in 0 until chunks) {
            val personal = G_PREFIX + byteArrayOf(i.toByte(), (j and 0xFF).toByte(), (j shr 8).toByte())
            xorInto(right, j * Blake2b.MAX_OUT_BYTES, Blake2b.hash(Blake2b.MAX_OUT_BYTES, personal, left))
        }
    }

    private fun xorInto(
        target: ByteArray,
        offset: Int,
        source: ByteArray,
    ) {
        val count = minOf(source.size, target.size - offset)
        for (k in 0 until count) {
            target[offset + k] = (target[offset + k].toInt() xor source[k].toInt()).toByte()
        }
    }
}
