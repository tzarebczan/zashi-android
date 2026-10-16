package co.electriccoin.zcash.ui.screen.viewingkey.name

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.AccountNameDuplicates
import co.electriccoin.zcash.ui.common.model.AccountNameLimit
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository
import co.electriccoin.zcash.ui.common.usecase.NavigateToColorPickerUseCase
import co.electriccoin.zcash.ui.common.usecase.ObserveAccountNamesUseCase
import co.electriccoin.zcash.ui.screen.connecthw.HWWalletEnrollment
import co.electriccoin.zcash.ui.screen.connecthw.date.HWDateArgs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Names the pending view-only account, then goes straight on to the shared birthday date screen:
 * a view-only import always asks when the account was created, never whether it is new. The name
 * is capped at [AccountNameLimit.MAX_LENGTH] code points as it is typed. Without a pending key,
 * after process death, there is nothing to name and the flow returns to the root. The avatar's pencil
 * badge opens the colour picker, the avatar follows the picks live and keeps the last one however the
 * picker closes; Continue keeps the picked colour with the pending import, in memory
 * only, for the import to store with the new account. A name another account already shows, by
 * [AccountNameDuplicates], disables Continue and says so under the field.
 */
class NameVKVM(
    private val pendingImportRepository: ViewOnlyPendingImportRepository,
    private val navigationRouter: NavigationRouter,
    private val navigateToColorPicker: NavigateToColorPickerUseCase,
    accountNames: ObserveAccountNamesUseCase,
) : ViewModel() {
    private val name = MutableStateFlow(pendingImportRepository.get()?.name.orEmpty())

    private val color = MutableStateFlow(pendingImportRepository.get()?.color)

    /** The name every existing account shows, which the new name must not repeat. */
    private val existingNames: StateFlow<Map<String, String>> =
        accountNames.observe().stateIn(viewModel = this, initialValue = emptyMap())

    private var colorPickerJob: Job? = null

    val state: StateFlow<NameVKState> =
        combine(name, color, existingNames) { name, color, existing ->
            createState(name, color, isDuplicate(name, existing))
        }.stateIn(viewModel = this, initialValue = createState(name.value, color.value, isDuplicate = false))

    init {
        if (pendingImportRepository.get() == null) {
            navigationRouter.backToRoot()
        }
    }

    private fun createState(
        name: String,
        color: Color?,
        isDuplicate: Boolean,
    ) = NameVKState.create(
        name = name,
        color = color,
        isDuplicate = isDuplicate,
        onAvatarEditClick = ::onAvatarEditClick,
        onValueChange = ::onValueChange,
        onContinueClick = ::onContinueClick,
        onBack = ::onBack,
    )

    private fun onAvatarEditClick() {
        if (colorPickerJob?.isActive == true) return
        colorPickerJob =
            viewModelScope.launch {
                navigateToColorPicker(color.value).collect { picked -> color.update { picked } }
            }
    }

    private fun onValueChange(value: String) = name.update { AccountNameLimit.cap(value) }

    private fun onContinueClick() {
        val normalized = AccountNameLimit.normalize(name.value)
        if (normalized.isEmpty() || isDuplicate(normalized, existingNames.value)) return
        val pending = pendingImportRepository.get()
        if (pending == null) {
            navigationRouter.backToRoot()
            return
        }
        pendingImportRepository.set(pending.copy(name = normalized, color = color.value))
        navigationRouter.forward(HWDateArgs(HWWalletEnrollment.ViewOnly))
    }

    /**
     * Returns to the key, which the paste screen still holds; the pending import is forgotten until
     * the key is submitted again.
     */
    private fun onBack() {
        colorPickerJob?.cancel()
        pendingImportRepository.clear()
        navigationRouter.back()
    }

    private fun isDuplicate(
        name: String,
        existing: Map<String, String>
    ) = AccountNameDuplicates.isDuplicate(name, existing)
}
