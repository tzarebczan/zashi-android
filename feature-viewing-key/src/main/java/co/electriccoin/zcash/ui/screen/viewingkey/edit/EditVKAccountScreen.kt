package co.electriccoin.zcash.ui.screen.viewingkey.edit

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.viewingkey.EditVKAccountArgs
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditVKAccountScreen(args: EditVKAccountArgs) {
    val vm = koinViewModel<EditVKAccountVM> { parametersOf(args) }
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler(state != null) { state?.onBack?.invoke() }
    state?.let { EditVKAccountView(it) }
}
