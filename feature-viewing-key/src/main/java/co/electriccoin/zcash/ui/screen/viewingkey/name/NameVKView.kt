package co.electriccoin.zcash.ui.screen.viewingkey.name

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import co.electriccoin.zcash.ui.design.component.BlankBgScaffold
import co.electriccoin.zcash.ui.design.component.ZashiButton
import co.electriccoin.zcash.ui.design.component.rememberZashiFrostState
import co.electriccoin.zcash.ui.design.component.zashiFrostSource
import co.electriccoin.zcash.ui.design.component.zashiFrostedHeader
import co.electriccoin.zcash.ui.design.newcomponent.PreviewScreens
import co.electriccoin.zcash.ui.design.theme.ZcashTheme
import co.electriccoin.zcash.ui.design.util.scaffoldPadding
import co.electriccoin.zcash.ui.screen.viewingkey.NameVKField
import co.electriccoin.zcash.ui.screen.viewingkey.VKHeader
import co.electriccoin.zcash.ui.screen.viewingkey.VKTopAppBar

@Composable
fun NameVKView(state: NameVKState) {
    val hazeState = rememberZashiFrostState()
    BlankBgScaffold(
        topBar = {
            VKTopAppBar(
                title = state.title,
                subtitle = state.subtitle,
                onBack = state.onBack,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .zashiFrostedHeader(hazeState),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .zashiFrostSource(hazeState)
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .scaffoldPadding(paddingValues)
            ) {
                Spacer(Modifier.height(4.dp))
                VKHeader(avatar = state.avatar, heading = state.heading, message = state.message)
                Spacer(Modifier.height(32.dp))
                NameVKField(
                    label = state.fieldLabel,
                    field = state.field,
                    placeholder = state.placeholder,
                    counter = state.counter,
                    onDone = { if (state.continueButton.isEnabled) state.continueButton.onClick() },
                    fieldTestTag = NameVKTag.FIELD,
                    counterTestTag = NameVKTag.COUNTER,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(24.dp))
                ZashiButton(
                    state = state.continueButton,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(NameVKTag.CONTINUE_BUTTON)
                )
            }
        }
    }
}

@PreviewScreens
@Composable
private fun EmptyPreview() =
    ZcashTheme {
        NameVKView(NameVKState.preview)
    }

@PreviewScreens
@Composable
private fun FilledPreview() =
    ZcashTheme {
        NameVKView(NameVKState.previewFilled)
    }

@PreviewScreens
@Composable
private fun LimitPreview() =
    ZcashTheme {
        NameVKView(NameVKState.previewLimit)
    }
