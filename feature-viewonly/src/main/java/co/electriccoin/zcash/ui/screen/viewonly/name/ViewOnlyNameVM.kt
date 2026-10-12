package co.electriccoin.zcash.ui.screen.viewonly.name

import androidx.lifecycle.ViewModel
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository
import co.electriccoin.zcash.ui.screen.connecthw.HWWalletEnrollment
import co.electriccoin.zcash.ui.screen.connecthw.neworactive.HWNewOrActiveArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Names the pending view-only account, then goes on to the shared birthday question. The name is
 * capped at [ViewOnlyNameState.MAX_NAME_LENGTH] characters as it is typed. Without a pending key,
 * after process death, there is nothing to name and the flow returns to the root.
 */
class ViewOnlyNameVM(
    private val pendingImportRepository: ViewOnlyPendingImportRepository,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val name = MutableStateFlow(pendingImportRepository.get()?.name.orEmpty())

    val state: StateFlow<ViewOnlyNameState> =
        name.map(::createState).stateIn(viewModel = this, initialValue = createState(name.value))

    init {
        if (pendingImportRepository.get() == null) {
            navigationRouter.backToRoot()
        }
    }

    private fun createState(name: String) =
        ViewOnlyNameState.create(
            name = name,
            onValueChange = ::onValueChange,
            onContinueClick = ::onContinueClick,
            onBack = ::onBack,
        )

    private fun onValueChange(value: String) = name.update { value.take(ViewOnlyNameState.MAX_NAME_LENGTH) }

    private fun onContinueClick() {
        val trimmed = name.value.trim()
        if (trimmed.isEmpty()) return
        val pending = pendingImportRepository.get()
        if (pending == null) {
            navigationRouter.backToRoot()
            return
        }
        pendingImportRepository.set(pending.copy(name = trimmed))
        navigationRouter.forward(HWNewOrActiveArgs(HWWalletEnrollment.ViewOnly))
    }

    /**
     * Returns to the key, which the paste screen still holds; the pending import is forgotten until
     * the key is submitted again.
     */
    private fun onBack() {
        pendingImportRepository.clear()
        navigationRouter.back()
    }
}
