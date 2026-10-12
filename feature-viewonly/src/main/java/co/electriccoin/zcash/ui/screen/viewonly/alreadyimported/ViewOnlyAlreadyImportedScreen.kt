package co.electriccoin.zcash.ui.screen.viewonly.alreadyimported

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ViewOnlyAlreadyImportedScreen() {
    val vm = koinViewModel<ViewOnlyAlreadyImportedVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    ViewOnlyAlreadyImportedView(state)
}
