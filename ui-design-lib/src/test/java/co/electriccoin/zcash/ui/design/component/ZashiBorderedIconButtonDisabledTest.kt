package co.electriccoin.zcash.ui.design.component

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import co.electriccoin.zcash.ui.design.R
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * A disabled bordered icon button, such as the scan button while an import is being submitted,
 * ignores taps and looks disabled: it draws differently from the same button enabled, while two
 * enabled copies draw identically.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZashiBorderedIconButtonDisabledTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var clicks = 0

    private var rootView: View? = null

    @Test
    fun aDisabledButtonIgnoresTaps() {
        setContent()

        composeTestRule.onNodeWithTag(ENABLED).assertIsEnabled()
        composeTestRule.onNodeWithTag(DISABLED).assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun aDisabledButtonIsDrawnDimmed() {
        setContent()

        val enabled = pixels(ENABLED)
        val otherEnabled = pixels(OTHER_ENABLED)
        val disabled = pixels(DISABLED)

        assertContentEquals(enabled, otherEnabled)
        assertFalse(enabled.contentEquals(disabled))
    }

    /**
     * The pixels of the visible 32 dp square at the centre of the button's touch target, read
     * from a software draw of the whole window.
     */
    private fun pixels(tag: String): IntArray {
        val bounds = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val bitmap = composeTestRule.runOnIdle { checkNotNull(rootView).drawToBitmap() }
        val half = with(composeTestRule.density) { 16.dp.toPx() }.toInt()
        val centerX = bounds.center.x.toInt()
        val centerY = bounds.center.y.toInt()
        val side = 2 * half
        val result = IntArray(side * side)
        bitmap.getPixels(result, 0, side, centerX - half, centerY - half, side, side)
        return result
    }

    private fun setContent() {
        composeTestRule.setContent {
            rootView = LocalView.current
            ZcashTheme {
                BlankSurface {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        TestButton(tag = ENABLED, isEnabled = true)
                        TestButton(tag = OTHER_ENABLED, isEnabled = true)
                        TestButton(tag = DISABLED, isEnabled = false)
                    }
                }
            }
        }
    }

    @Composable
    private fun TestButton(
        tag: String,
        isEnabled: Boolean
    ) {
        ZashiBorderedIconButton(
            state =
                IconButtonState(
                    icon = R.drawable.ic_item_keystone,
                    isEnabled = isEnabled,
                    onClick = { clicks++ },
                ),
            modifier = Modifier.testTag(tag),
        )
    }

    private companion object {
        const val ENABLED = "enabled"
        const val OTHER_ENABLED = "otherEnabled"
        const val DISABLED = "disabled"
    }
}
