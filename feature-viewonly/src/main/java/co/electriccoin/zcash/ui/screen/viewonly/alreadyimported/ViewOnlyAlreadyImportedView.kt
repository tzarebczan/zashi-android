package co.electriccoin.zcash.ui.screen.viewonly.alreadyimported

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.ZashiButton
import co.electriccoin.zcash.ui.design.component.ZashiScreenModalBottomSheet
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZashiColors
import co.electriccoin.zcash.ui.design.theme.typography.ZashiTypography
import co.electriccoin.zcash.ui.design.util.Compose
import co.electriccoin.zcash.ui.design.util.getValue

/**
 * Laid out like the flow's invalid-key and invalid-code sheets: the alert in a circle, a centred
 * title and message, and one button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOnlyAlreadyImportedView(state: ViewOnlyAlreadyImportedState?) {
    ZashiScreenModalBottomSheet(
        state = state,
        shape = RoundedCornerShape(34.dp),
        content = { innerState, contentPadding ->
            Column(
                modifier =
                    Modifier
                        .weight(1f, false)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = contentPadding.calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .background(ZashiColors.Surfaces.bgPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    innerState.icon.Compose(modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = innerState.title.getValue(),
                    style = ZashiTypography.textXl,
                    fontWeight = FontWeight.SemiBold,
                    color = ZashiColors.Text.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = innerState.message.getValue(),
                    style = ZashiTypography.textSm,
                    color = ZashiColors.Text.textTertiary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                ZashiButton(
                    state = innerState.closeButton,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        },
    )
}

@PreviewScreens
@Composable
private fun AlreadyImportedPreview() =
    ZcashTheme {
        ViewOnlyAlreadyImportedView(ViewOnlyAlreadyImportedState.preview)
    }
