package co.electriccoin.zcash.ui.screen.viewingkey.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import co.electriccoin.zcash.ui.design.theme.colors.ZashiColors
import co.electriccoin.zcash.ui.design.theme.typography.ZashiTypography
import co.electriccoin.zcash.ui.design.util.Compose
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.scaffoldPadding
import co.electriccoin.zcash.ui.screen.viewingkey.NameVKField
import co.electriccoin.zcash.ui.screen.viewingkey.VKHeader
import co.electriccoin.zcash.ui.screen.viewingkey.VKTopAppBar

/**
 * The import's name screen with an edit top bar, and the removal entry under the field: the
 * confirmation hint, the destructive Remove Account button and Save changes.
 */
@Composable
fun EditVKAccountView(state: EditVKAccountState) {
    val hazeState = rememberZashiFrostState()
    BlankBgScaffold(
        topBar = {
            VKTopAppBar(
                title = state.title,
                subtitle = null,
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
                    onDone = { if (state.saveButton.isEnabled) state.saveButton.onClick() },
                    fieldTestTag = EditVKAccountTag.FIELD,
                    counterTestTag = EditVKAccountTag.COUNTER,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(24.dp))
                RemoveInfo(state)
                Spacer(Modifier.height(16.dp))
                ZashiButton(
                    state = state.removeButton,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(EditVKAccountTag.REMOVE_BUTTON)
                )
                Spacer(Modifier.height(8.dp))
                ZashiButton(
                    state = state.saveButton,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(EditVKAccountTag.SAVE_BUTTON)
                )
            }
        }
    }
}

@Composable
private fun RemoveInfo(state: EditVKAccountState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.removeInfoIcon.Compose(modifier = Modifier.size(16.dp))
        Text(
            text = state.removeInfo.getValue(),
            style = ZashiTypography.textXs,
            color = ZashiColors.Text.textTertiary,
        )
    }
}

@PreviewScreens
@Composable
private fun DefaultPreview() =
    ZcashTheme {
        EditVKAccountView(EditVKAccountState.preview)
    }

@PreviewScreens
@Composable
private fun ChangedPreview() =
    ZcashTheme {
        EditVKAccountView(EditVKAccountState.previewChanged)
    }

@PreviewScreens
@Composable
private fun LimitPreview() =
    ZcashTheme {
        EditVKAccountView(EditVKAccountState.previewLimit)
    }
