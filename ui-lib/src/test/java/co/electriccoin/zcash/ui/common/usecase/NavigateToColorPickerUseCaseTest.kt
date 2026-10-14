package co.electriccoin.zcash.ui.common.usecase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.screen.colorpicker.ColorPickerArgs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [NavigateToColorPickerUseCase] opens the sheet seeded with the initial colour, returns the picked
 * colour with its alpha (a dismissal resolves to null), goes back once either way, and ignores an
 * answer to a different request.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigateToColorPickerUseCaseTest {
    private val forwarded = mutableListOf<Any>()
    private val router =
        mockk<NavigationRouter>(relaxed = true) {
            every { forward(*anyVararg()) } answers {
                (args[0] as Array<*>).filterNotNull().forEach { forwarded.add(it) }
            }
        }
    private val useCase = NavigateToColorPickerUseCase(router)

    @Test
    fun selectionResolvesToThePickedColorWithItsAlpha() =
        runTest(UnconfinedTestDispatcher()) {
            val result = async { useCase(Color(INITIAL)) }

            val args = forwarded.single() as ColorPickerArgs
            assertEquals(Color(INITIAL).toArgb(), args.initialArgb)

            useCase.onSelected(Color(PICKED), args)

            assertEquals(Color(PICKED), result.await())
            verify(exactly = 1) { router.back() }
        }

    @Test
    fun cancellationResolvesToNull() =
        runTest(UnconfinedTestDispatcher()) {
            val result = async { useCase(null) }

            val args = forwarded.single() as ColorPickerArgs
            assertNull(args.initialArgb)
            useCase.onCancelled(args)

            assertNull(result.await())
            verify(exactly = 1) { router.back() }
        }

    @Test
    fun ignoresAnAnswerToADifferentRequest() =
        runTest(UnconfinedTestDispatcher()) {
            val result = async { useCase(null) }
            val args = forwarded.single() as ColorPickerArgs

            useCase.onSelected(Color(INITIAL), args.copy(requestId = "stale"))
            assertTrue(result.isActive)

            useCase.onSelected(Color(PICKED), args)
            assertEquals(Color(PICKED), result.await())
        }

    private companion object {
        const val INITIAL = 0xFF34C759
        const val PICKED = 0x80BE38F3
    }
}
