package co.electriccoin.zcash.ui.screen.viewonly.paste

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.common.compose.SecureScreen
import co.electriccoin.zcash.ui.design.component.ZashiConfirmationBottomSheet
import org.koin.androidx.compose.koinViewModel

@Composable
fun ViewOnlyPasteScreen() {
    SecureScreen()
    val vm = koinViewModel<ViewOnlyPasteVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    val invalidKeySheet by vm.invalidKeySheet.collectAsStateWithLifecycle()
    BackHandler { state.onBack() }
    ViewOnlyPasteView(state)
    ZashiConfirmationBottomSheet(invalidKeySheet)
}
