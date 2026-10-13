package co.electriccoin.zcash.ui.screen.viewingkey.importvk

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.common.compose.SecureScreen
import co.electriccoin.zcash.ui.design.component.ZashiConfirmationBottomSheet
import org.koin.androidx.compose.koinViewModel

@Composable
fun ImportVKScreen() {
    SecureScreen()
    val vm = koinViewModel<ImportVKVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    val invalidKeySheet by vm.invalidKeySheet.collectAsStateWithLifecycle()
    BackHandler { state.onBack() }
    ImportVKView(state)
    ZashiConfirmationBottomSheet(invalidKeySheet)
}
