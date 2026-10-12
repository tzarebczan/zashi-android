package co.electriccoin.zcash.ui.design.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import co.electriccoin.zcash.ui.design.R
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.stringRes
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * As the trailing icon of a 40 dp text field, the bordered icon button keeps the field 40 dp tall
 * and still has a 48 dp touch target, including the part that overflows the field above and below.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZashiBorderedIconButtonTouchTargetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var clicks = 0

    @Test
    fun theFieldStaysFortyDpTallAndTheButtonHasAFortyEightDpTarget() {
        setContent()

        val field = composeTestRule.onNodeWithTag(FIELD_TAG).getUnclippedBoundsInRoot()
        val button = composeTestRule.onNodeWithContentDescription(SCAN).getUnclippedBoundsInRoot()
        assertEquals(40.dp, field.height)
        assertEquals(48.dp, button.width)
        assertEquals(48.dp, button.height)
    }

    @Test
    fun aTapOnTheTargetOutsideTheFieldAndOutsideTheVisibleButtonReachesTheButton() {
        setContent()

        val button = composeTestRule.onNodeWithContentDescription(SCAN)
        button.performTouchInput { click(Offset(x = width / 2f, y = 1.dp.toPx())) }
        button.performTouchInput { click(Offset(x = width / 2f, y = height - 1.dp.toPx())) }
        button.performTouchInput { click(Offset(x = 1.dp.toPx(), y = height / 2f)) }

        assertEquals(3, clicks)
    }

    private fun setContent() {
        composeTestRule.setContent {
            ZcashTheme {
                Column(Modifier.width(360.dp)) {
                    Spacer(Modifier.height(24.dp))
                    ZashiTextField(
                        state = TextFieldState(value = stringRes(""), onValueChange = {}),
                        modifier = Modifier.testTag(FIELD_TAG),
                        innerModifier = ZashiTextFieldDefaults.innerModifier.height(40.dp),
                        singleLine = true,
                        trailingIcon = {
                            ZashiBorderedIconButton(
                                state =
                                    IconButtonState(
                                        icon = R.drawable.ic_item_keystone,
                                        contentDescription = stringRes(SCAN),
                                        onClick = { clicks++ },
                                    ),
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        },
                        contentPadding = PaddingValues(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    private companion object {
        const val SCAN = "Scan"
        const val FIELD_TAG = "field"
    }
}
