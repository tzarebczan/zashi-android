package co.electriccoin.zcash.ui.design.component.colorpicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** The colour's red, green and blue as six upper-case hex digits, without alpha or `#`. */
fun Color.toHex(): String =
    (toArgb() and RGB_MASK)
        .toString(HEX_RADIX)
        .uppercase()
        .padStart(HEX_LENGTH, '0')

/** The colour as `#RRGGBBAA` in sRGB, upper-case, alpha last. */
fun Color.toRgbaHex(): String {
    val alpha = (toArgb() ushr ALPHA_SHIFT).toString(HEX_RADIX).uppercase().padStart(2, '0')
    return "#" + toHex() + alpha
}

/** A colour from `#RRGGBBAA`, the form [toRgbaHex] writes; null for anything else. */
fun parseRgbaHex(text: String): Color? {
    val digits = text.removePrefix("#")
    if (!text.startsWith("#") || digits.length != RGBA_LENGTH || !digits.all { it.isHexDigit() }) return null
    val rgba = digits.toLong(HEX_RADIX)
    return Color(((rgba and ALPHA_MASK) shl ALPHA_SHIFT) or (rgba ushr ALPHA_BITS))
}

/** An opaque colour from six hex digits, with or without a leading `#`; null for anything else. */
fun parseHexColor(text: String): Color? {
    val digits = text.trim().removePrefix("#")
    if (digits.length != HEX_LENGTH || !digits.all { it.isHexDigit() }) return null
    return Color(digits.toInt(HEX_RADIX) or OPAQUE_MASK)
}

/** The hex digits of [text], upper-cased and capped at six, for the hex field to show as it is typed. */
fun filterHexInput(text: String): String =
    text
        .filter { it.isHexDigit() }
        .uppercase()
        .take(HEX_LENGTH)

private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private const val HEX_LENGTH = 6
private const val RGBA_LENGTH = 8
private const val HEX_RADIX = 16
private const val RGB_MASK = 0xFFFFFF
private const val OPAQUE_MASK = 0xFF shl 24
private const val ALPHA_SHIFT = 24
private const val ALPHA_BITS = 8
private const val ALPHA_MASK = 0xFFL
