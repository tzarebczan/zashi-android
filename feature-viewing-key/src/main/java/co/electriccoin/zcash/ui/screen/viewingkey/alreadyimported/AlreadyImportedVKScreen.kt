package co.electriccoin.zcash.ui.screen.viewingkey.alreadyimported

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlreadyImportedVKScreen() {
    val vm = koinViewModel<AlreadyImportedVKVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    AlreadyImportedVKView(state)
}
