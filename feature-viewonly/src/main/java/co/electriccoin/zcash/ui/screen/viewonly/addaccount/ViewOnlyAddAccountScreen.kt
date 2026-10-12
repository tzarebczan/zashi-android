package co.electriccoin.zcash.ui.screen.viewonly.addaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ViewOnlyAddAccountScreen() {
    val vm = koinViewModel<ViewOnlyAddAccountVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    ViewOnlyAddAccountView(state)
}
