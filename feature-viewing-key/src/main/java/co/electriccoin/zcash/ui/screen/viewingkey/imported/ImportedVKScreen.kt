package co.electriccoin.zcash.ui.screen.viewingkey.imported

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ImportedVKScreen() {
    val vm = koinViewModel<ImportedVKVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler { state.onBack() }
    ImportedVKView(state)
}
