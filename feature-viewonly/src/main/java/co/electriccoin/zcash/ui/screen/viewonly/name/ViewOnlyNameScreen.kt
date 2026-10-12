package co.electriccoin.zcash.ui.screen.viewonly.name

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ViewOnlyNameScreen() {
    val vm = koinViewModel<ViewOnlyNameVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler { state.onBack() }
    ViewOnlyNameView(state)
}
