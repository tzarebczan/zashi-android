package co.electriccoin.zcash.ui.screen.viewingkey.name

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun NameVKScreen() {
    val vm = koinViewModel<NameVKVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler { state.onBack() }
    NameVKView(state)
}
