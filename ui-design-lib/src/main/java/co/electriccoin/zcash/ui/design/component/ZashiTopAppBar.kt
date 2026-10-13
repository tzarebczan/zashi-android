package co.electriccoin.zcash.ui.design.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.theme.internal.SecondaryTypography
import co.electriccoin.zcash.ui.design.theme.internal.TopAppBarColors
import co.electriccoin.zcash.ui.design.theme.typography.ZashiTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ZashiSmallTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    colors: TopAppBarColors = ZcashTheme.colors.topAppBarColors,
    navigationAction: @Composable () -> Unit = {},
    hamburgerMenuActions: (@Composable RowScope.() -> Unit)? = null,
    regularActions: (@Composable RowScope.() -> Unit)? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    subtitleAllCaps: Boolean = true,
    titleStyle: TextStyle = SmallTopAppBarDefaults.titleStyle,
    subtitleStyle: TextStyle = SmallTopAppBarDefaults.subTitleStyle,
) {
    SmallTopAppBar(
        windowInsets = windowInsets,
        modifier = modifier,
        colors = colors,
        hamburgerMenuActions = hamburgerMenuActions,
        navigationAction = navigationAction,
        regularActions = regularActions,
        subTitle = subtitle,
        titleText = title,
        titleStyle = titleStyle,
        subTitleAllCaps = subtitleAllCaps,
        subTitleStyle = subtitleStyle,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ZashiSmallTopAppBar(
    content: (@Composable ColumnScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = ZcashTheme.colors.topAppBarColors,
    navigationAction: @Composable () -> Unit = {},
    hamburgerMenuActions: (@Composable RowScope.() -> Unit)? = null,
    regularActions: (@Composable RowScope.() -> Unit)? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    SmallTopAppBar(
        modifier = modifier,
        colors = colors,
        hamburgerMenuActions = hamburgerMenuActions,
        navigationAction = navigationAction,
        regularActions = regularActions,
        content = content,
        windowInsets = windowInsets,
    )
}

@PreviewScreens
@Composable
private fun ZashiSmallTopAppBarPreview() =
    ZcashTheme {
        ZashiSmallTopAppBar(
            title = "Test Title",
            subtitle = "Subtitle",
        )
    }
