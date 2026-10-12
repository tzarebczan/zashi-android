package co.electriccoin.zcash.ui.screen.viewonly

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.ZashiSmallTopAppBar
import co.electriccoin.zcash.ui.design.component.ZashiTopAppBarBackNavigation
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZashiColors
import co.electriccoin.zcash.ui.design.theme.typography.ZashiTypography
import co.electriccoin.zcash.ui.design.util.Compose
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.getValue

/**
 * The top app bar every screen of the import flow shares: back, the flow's title and its subtitle.
 */
@Composable
internal fun ViewOnlyTopAppBar(
    title: StringResource,
    subtitle: StringResource,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ZashiSmallTopAppBar(
        modifier = modifier,
        title = title.getValue(),
        subtitle = subtitle.getValue(),
        navigationAction = { ZashiTopAppBarBackNavigation(onBack = onBack) },
        colors = ZcashTheme.colors.topAppBarColors.copyColors(containerColor = Color.Transparent),
    )
}

/**
 * The account avatar over a heading and a message, which opens every screen of the import flow.
 */
@Composable
internal fun ViewOnlyHeader(
    avatar: ImageResource.ByDrawable,
    heading: StringResource,
    message: StringResource,
    modifier: Modifier = Modifier,
    headingStyleIsLarge: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        avatar.Compose(modifier = Modifier.size(AVATAR_SIZE))
        Spacer(Modifier.height(20.dp))
        Text(
            text = heading.getValue(),
            style = if (headingStyleIsLarge) ZashiTypography.header6 else ZashiTypography.textXl,
            fontWeight = FontWeight.SemiBold,
            color = ZashiColors.Text.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message.getValue(),
            style = ZashiTypography.textSm,
            color = ZashiColors.Text.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

private val AVATAR_SIZE = 90.dp
