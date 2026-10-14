package co.electriccoin.zcash.ui.common.serialization

import co.electriccoin.zcash.ui.common.model.metadata.AccountAppearanceMetadataV3
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder

/**
 * The account appearance block, read as null when it does not decode, a colour that does not parse
 * included, so one bad value drops only the appearance and never the whole metadata file.
 */
object LenientAccountAppearanceSerializer : KSerializer<AccountAppearanceMetadataV3?> {
    private val delegate = AccountAppearanceMetadataV3.serializer().nullable

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(
        encoder: Encoder,
        value: AccountAppearanceMetadataV3?
    ) {
        delegate.serialize(encoder, value)
    }

    @Suppress("SwallowedException")
    override fun deserialize(decoder: Decoder): AccountAppearanceMetadataV3? {
        if (decoder !is JsonDecoder) return delegate.deserialize(decoder)
        val element = decoder.decodeJsonElement()
        return try {
            decoder.json.decodeFromJsonElement(delegate, element)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
