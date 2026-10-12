package co.electriccoin.zcash.ui.screen.viewonly.paste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.VersionInfo
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository
import co.electriccoin.zcash.ui.common.usecase.ClearClipboardUseCase
import co.electriccoin.zcash.ui.common.usecase.SubmitViewOnlyKeyResult
import co.electriccoin.zcash.ui.common.usecase.SubmitViewOnlyKeyUseCase
import co.electriccoin.zcash.ui.common.usecase.ValidateUfvkUseCase
import co.electriccoin.zcash.ui.design.component.ZashiConfirmationState
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyAlreadyImportedArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyNameArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyScanArgs
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Takes the full viewing key the user pastes or types and, once it is importable, goes on to name
 * the account. The key stays in this view model's memory and in the pending import only: it never
 * goes into a route, the saved instance state or a log.
 *
 * The keyboard pastes into the field directly, so a paste is recognised by a single edit that adds
 * a long run of text. When one happened, the clipboard is cleared on Continue and on back, so the
 * key does not stay there. The clipboard is never read: on recent Android versions every read
 * shows a system notice.
 */
internal class ViewOnlyPasteVM(
    private val submitViewOnlyKey: SubmitViewOnlyKeyUseCase,
    private val clearClipboard: ClearClipboardUseCase,
    private val pendingImportRepository: ViewOnlyPendingImportRepository,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val text = MutableStateFlow("")

    private val isSubmitting = MutableStateFlow(false)

    private val isInvalidKeySheetVisible = MutableStateFlow(false)

    private var hasPasted = false

    private var submitJob: Job? = null

    private var sheetJob: Job? = null

    val state: StateFlow<ViewOnlyPasteState> =
        combine(text, isSubmitting, ::createState)
            .stateIn(viewModel = this, initialValue = createState(text.value, isSubmitting.value))

    val invalidKeySheet: StateFlow<ZashiConfirmationState?> =
        isInvalidKeySheetVisible
            .map { isVisible ->
                ViewOnlyInvalidKeySheet
                    .create(
                        keyPrefix = ValidateUfvkUseCase.keyPrefixOf(VersionInfo.NETWORK),
                        onScanClick = ::onInvalidKeyScanClick,
                        onEditClick = ::hideInvalidKeySheet,
                    ).takeIf { isVisible }
            }.stateIn(viewModel = this, initialValue = null)

    /**
     * The paste screen is the root of the import flow: every later screen (scan, name, birthday,
     * keep-open, success) is opened on top of it, so this view model is cleared only once the whole
     * flow has left the back stack. The pending key goes with it, whichever way the flow ended.
     */
    override fun onCleared() {
        cancelSubmit()
        pendingImportRepository.clear()
        super.onCleared()
    }

    private fun createState(
        text: String,
        isSubmitting: Boolean,
    ) = ViewOnlyPasteState.create(
        text = text,
        isSubmitting = isSubmitting,
        onValueChange = ::onValueChange,
        onScanClick = ::onScanClick,
        onContinueClick = ::onContinueClick,
        onBack = ::onBack,
    )

    private fun onValueChange(value: String) {
        if (value.length - text.value.length >= PASTED_LENGTH) hasPasted = true
        text.update { value }
    }

    private fun onScanClick() = navigationRouter.forward(ViewOnlyScanArgs)

    private fun onContinueClick() {
        if (submitJob?.isActive == true || text.value.isBlank()) return
        submitJob =
            viewModelScope.launch {
                isSubmitting.update { true }
                try {
                    val result = submitViewOnlyKey(text.value)
                    currentCoroutineContext().ensureActive()
                    when (result) {
                        SubmitViewOnlyKeyResult.Accepted -> {
                            clearPasted()
                            navigationRouter.forward(ViewOnlyNameArgs)
                        }

                        SubmitViewOnlyKeyResult.AlreadyImported -> {
                            clearPasted()
                            navigationRouter.forward(ViewOnlyAlreadyImportedArgs)
                        }

                        SubmitViewOnlyKeyResult.Invalid -> {
                            isInvalidKeySheetVisible.update { true }
                        }
                    }
                } finally {
                    isSubmitting.update { false }
                }
            }
    }

    private fun hideInvalidKeySheet() = isInvalidKeySheetVisible.update { false }

    /**
     * Hides the sheet first and opens the scanner once it has animated away.
     */
    private fun onInvalidKeyScanClick() {
        if (sheetJob?.isActive == true) return
        sheetJob =
            viewModelScope.launch {
                hideInvalidKeySheet()
                delay(SHEET_HIDE_DELAY)
                navigationRouter.forward(ViewOnlyScanArgs)
            }
    }

    /**
     * Leaves the import: the running check is cancelled, the pending key forgotten and the
     * clipboard cleared when the key was pasted.
     */
    private fun onBack() {
        cancelSubmit()
        clearPasted()
        pendingImportRepository.clear()
        navigationRouter.back()
    }

    private fun cancelSubmit() {
        submitJob?.cancel()
        submitJob = null
    }

    private fun clearPasted() {
        if (!hasPasted) return
        hasPasted = false
        clearClipboard()
    }

    private companion object {
        /** An edit that adds at least this many characters at once is taken for a paste. */
        const val PASTED_LENGTH = 20
        val SHEET_HIDE_DELAY = 350.milliseconds
    }
}
