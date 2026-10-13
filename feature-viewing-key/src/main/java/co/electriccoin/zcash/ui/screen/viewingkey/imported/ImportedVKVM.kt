package co.electriccoin.zcash.ui.screen.viewingkey.imported

import androidx.lifecycle.ViewModel
import co.electriccoin.zcash.ui.NavigationRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The end of the import. The account is already imported and selected by the time this screen
 * shows, so Done and back both close the flow and land on the new account's home.
 */
class ImportedVKVM(
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    val state: StateFlow<ImportedVKState> =
        MutableStateFlow(
            ImportedVKState.create(onDoneClick = ::onDone, onBack = ::onDone)
        ).asStateFlow()

    private fun onDone() = navigationRouter.backToRoot()
}
