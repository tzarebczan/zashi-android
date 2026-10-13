package co.electriccoin.zcash.ui.screen.viewingkey.remove

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.viewingkey.RemoveVKAccountArgs
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RemoveVKAccountScreen(args: RemoveVKAccountArgs) {
    val vm = koinViewModel<RemoveVKAccountVM> { parametersOf(args) }
    val state by vm.state.collectAsStateWithLifecycle()
    RemoveVKAccountView(state)
}
