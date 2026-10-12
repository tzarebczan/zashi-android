package co.electriccoin.zcash.ui.common.model.ufvk

/**
 * Bech32m (BIP 350) without the 90-character limit, which unified encodings lift (ZIP 316).
 */
@Suppress("MagicNumber")
internal object Bech32m {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private const val CONSTANT = 0x2bc830a3
    private const val CHECKSUM_LENGTH = 6
    private val GENERATOR = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    /**
     * The human-readable part and the 8-bit payload of [text], or null when [text] is not a valid
     * Bech32m string. Mixed case is rejected, as BIP 350 requires.
     */
    fun decode(text: String): Pair<String, ByteArray>? {
        val lower = text.lowercase()
        val separator = lower.lastIndexOf('1')
        val hrp = lower.substring(0, maxOf(separator, 0))
        val isWellFormed =
            (lower == text || text.uppercase() == text) &&
                separator >= 1 &&
                separator + CHECKSUM_LENGTH + 1 <= lower.length &&
                hrp.all { it.code in 33..126 }
        val values =
            lower
                .takeIf { isWellFormed }
                ?.substring(separator + 1)
                ?.map { CHARSET.indexOf(it) }
                ?.toIntArray()
                ?.takeIf { decoded -> decoded.all { it >= 0 } && polymod(expandHrp(hrp) + decoded) == CONSTANT }
        val payload = values?.let { convertBits(it.copyOfRange(0, it.size - CHECKSUM_LENGTH), 5, 8, pad = false) }
        return payload?.let { bytes -> hrp to ByteArray(bytes.size) { bytes[it].toByte() } }
    }

    fun encode(
        hrp: String,
        payload: ByteArray,
    ): String {
        val bytes = IntArray(payload.size) { payload[it].toInt() and 0xFF }
        val values = checkNotNull(convertBits(bytes, 8, 5, pad = true))
        val checksum = polymod(expandHrp(hrp) + values + IntArray(CHECKSUM_LENGTH)) xor CONSTANT
        val all =
            values + IntArray(CHECKSUM_LENGTH) { (checksum ushr (5 * (CHECKSUM_LENGTH - 1 - it))) and 31 }
        return hrp + "1" + all.joinToString("") { CHARSET[it].toString() }
    }

    private fun polymod(values: IntArray): Int {
        var checksum = 1
        for (value in values) {
            val top = checksum ushr 25
            checksum = ((checksum and 0x1ffffff) shl 5) xor value
            for (i in GENERATOR.indices) {
                if ((top ushr i) and 1 == 1) checksum = checksum xor GENERATOR[i]
            }
        }
        return checksum
    }

    private fun expandHrp(hrp: String): IntArray =
        IntArray(hrp.length) { hrp[it].code ushr 5 } + intArrayOf(0) + IntArray(hrp.length) { hrp[it].code and 31 }

    private fun convertBits(
        data: IntArray,
        fromBits: Int,
        toBits: Int,
        pad: Boolean,
    ): IntArray? {
        var accumulator = 0
        var bits = 0
        val maxValue = (1 shl toBits) - 1
        val result = ArrayList<Int>(data.size * fromBits / toBits + 1)
        for (value in data) {
            accumulator = (accumulator shl fromBits) or value
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                result.add((accumulator ushr bits) and maxValue)
            }
            accumulator = accumulator and ((1 shl bits) - 1)
        }
        if (pad) {
            if (bits > 0) result.add((accumulator shl (toBits - bits)) and maxValue)
        } else if (bits >= fromBits || accumulator != 0) {
            return null
        }
        return result.toIntArray()
    }
}
