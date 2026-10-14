package co.electriccoin.zcash.ui.common.serialization

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** [ColorSerializer] writing `#RRGGBBAA` upper-case, reading either case, and rejecting the rest. */
class ColorSerializerTest {
    @Test
    fun aColorRoundTripsWithItsAlphaLast() {
        val encoded = Json.encodeToString(ColorSerializer, Color(0x80BE38F3))

        assertEquals("\"#BE38F380\"", encoded)
        assertEquals(Color(0x80BE38F3), Json.decodeFromString(ColorSerializer, encoded))
    }

    @Test
    fun anOpaqueColorKeepsFullAlpha() {
        assertEquals("\"#0088FFFF\"", Json.encodeToString(ColorSerializer, Color(0xFF0088FF)))
        assertEquals(Color(0x000088FF), Json.decodeFromString(ColorSerializer, "\"#0088FF00\""))
    }

    @Test
    fun lowerAndUpperCaseInputReadTheSame() {
        assertEquals(Color(0x80BE38F3), Json.decodeFromString(ColorSerializer, "\"#be38f380\""))
        assertEquals(Color(0x80BE38F3), Json.decodeFromString(ColorSerializer, "\"#BE38F380\""))
        assertEquals(Color(0x80BE38F3), Json.decodeFromString(ColorSerializer, "\"#Be38F380\""))
    }

    @Test
    fun anythingButRrggbbaaFails() {
        listOf("\"BE38F380\"", "\"#BE38F3\"", "\"#BE38F38G\"", "\"#BE38F3800\"", "\"\"", "\"purple\"").forEach { text ->
            assertFailsWith<SerializationException>(text) { Json.decodeFromString(ColorSerializer, text) }
        }
    }
}
