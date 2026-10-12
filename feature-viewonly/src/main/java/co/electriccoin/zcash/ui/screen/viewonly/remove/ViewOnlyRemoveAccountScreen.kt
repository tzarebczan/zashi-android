package co.electriccoin.zcash.ui.screen.viewonly.remove

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyRemoveAccountArgs
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ViewOnlyRemoveAccountScreen(args: ViewOnlyRemoveAccountArgs) {
    val vm = koinViewModel<ViewOnlyRemoveAccountVM> { parametersOf(args) }
    val state by vm.state.collectAsStateWithLifecycle()
    ViewOnlyRemoveAccountView(state)
}
