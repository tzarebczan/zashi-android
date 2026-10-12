package co.electriccoin.zcash.ui.screen.viewonly.addaccount

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.ZashiButton
import co.electriccoin.zcash.ui.design.component.ZashiButtonDefaults
import co.electriccoin.zcash.ui.design.component.ZashiScreenModalBottomSheet
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZashiColors
import co.electriccoin.zcash.ui.design.theme.typography.ZashiTypography
import co.electriccoin.zcash.ui.design.util.Compose
import co.electriccoin.zcash.ui.design.util.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOnlyAddAccountView(state: ViewOnlyAddAccountState?) {
    ZashiScreenModalBottomSheet(
        state = state,
        content = { innerState, contentPadding ->
            Content(
                state = innerState,
                modifier =
                    Modifier
                        .weight(1f, false)
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                        .padding(bottom = contentPadding.calculateBottomPadding())
            )
        },
    )
}

@Composable
private fun Content(
    state: ViewOnlyAddAccountState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = state.title.getValue(),
            style = ZashiTypography.textXl,
            fontWeight = FontWeight.SemiBold,
            color = ZashiColors.Text.textPrimary,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.options.forEach { option -> Option(option) }
        }
        Spacer(Modifier.height(24.dp))
        ZashiButton(
            state = state.dismissButton,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            defaultSecondaryColors =
                ZashiButtonDefaults.secondaryColors(
                    borderColor = ZashiColors.Btns.Secondary.btnSecondaryBorder
                )
        )
    }
}

@Composable
private fun Option(state: ViewOnlyAddAccountOptionState) {
    Surface(
        onClick = state.onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(state.testTag),
        shape = RoundedCornerShape(16.dp),
        color = ZashiColors.Surfaces.bgPrimary,
        border = BorderStroke(1.dp, ZashiColors.Surfaces.strokeSecondary),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.icon.Compose(modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.getValue(),
                    style = ZashiTypography.textSm,
                    fontWeight = FontWeight.Medium,
                    color = ZashiColors.Text.textPrimary,
                )
                Text(
                    text = state.subtitle.getValue(),
                    style = ZashiTypography.textXs,
                    color = ZashiColors.Text.textTertiary,
                )
            }
            Image(
                painter = painterResource(co.electriccoin.zcash.ui.design.R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(ZashiColors.Text.textTertiary),
            )
        }
    }
}

@PreviewScreens
@Composable
private fun AddAccountPreview() =
    ZcashTheme {
        ViewOnlyAddAccountView(ViewOnlyAddAccountState.preview)
    }

@PreviewScreens
@Composable
private fun HardwareWalletsConnectedPreview() =
    ZcashTheme {
        ViewOnlyAddAccountView(ViewOnlyAddAccountState.previewHardwareWalletsConnected)
    }
