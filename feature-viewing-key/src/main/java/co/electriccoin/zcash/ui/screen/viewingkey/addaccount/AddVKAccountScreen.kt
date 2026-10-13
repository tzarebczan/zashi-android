package co.electriccoin.zcash.ui.screen.viewingkey.addaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddVKAccountScreen() {
    val vm = koinViewModel<AddVKAccountVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    AddVKAccountView(state)
}
