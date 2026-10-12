package co.electriccoin.zcash.ui.screen.viewonly.alreadyimported

import androidx.lifecycle.ViewModel
import co.electriccoin.zcash.ui.NavigationRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Closing the sheet ends the import flow: the account it would add is already in the wallet.
 */
class ViewOnlyAlreadyImportedVM(
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    val state: StateFlow<ViewOnlyAlreadyImportedState> =
        MutableStateFlow(ViewOnlyAlreadyImportedState.create(onBack = ::onBack)).asStateFlow()

    private fun onBack() = navigationRouter.backToRoot()
}
