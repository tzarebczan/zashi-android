package co.electriccoin.zcash.ui.screen.viewonly.imported

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ViewOnlyImportedScreen() {
    val vm = koinViewModel<ViewOnlyImportedVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler { state.onBack() }
    ViewOnlyImportedView(state)
}
