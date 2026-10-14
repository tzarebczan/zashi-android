package co.electriccoin.zcash.ui.common.serialization

import androidx.compose.ui.graphics.Color
import co.electriccoin.zcash.ui.design.component.colorpicker.parseRgbaHex
import co.electriccoin.zcash.ui.design.component.colorpicker.toRgbaHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A colour as `#RRGGBBAA` in sRGB, written upper-case and read in either case. Anything else fails
 * with a [SerializationException].
 */
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Color
    ) {
        encoder.encodeString(value.toRgbaHex())
    }

    override fun deserialize(decoder: Decoder): Color {
        val text = decoder.decodeString()
        return parseRgbaHex(text) ?: throw SerializationException("Not a #RRGGBBAA colour")
    }
}
