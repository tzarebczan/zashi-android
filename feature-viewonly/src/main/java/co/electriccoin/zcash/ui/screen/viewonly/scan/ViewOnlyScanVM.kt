package co.electriccoin.zcash.ui.screen.viewonly.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.usecase.SubmitViewOnlyKeyResult
import co.electriccoin.zcash.ui.common.usecase.SubmitViewOnlyKeyUseCase
import co.electriccoin.zcash.ui.design.component.ZashiConfirmationState
import co.electriccoin.zcash.ui.screen.scan.ImageToQrCodeResult
import co.electriccoin.zcash.ui.screen.scan.ScanValidationState
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyAlreadyImportedArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyNameArgs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Scans a full viewing key QR code, or picks an image of one. A code that is not an importable key
 * opens the invalid-code sheet, and the camera's further results are ignored while it is up. An
 * importable key replaces the scanner with the naming screen, so back from there returns to the
 * paste screen. The key is never logged.
 *
 * Checking a key suspends, so the scanned-once guard runs under a mutex.
 */
internal class ViewOnlyScanVM(
    private val submitViewOnlyKey: SubmitViewOnlyKeyUseCase,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val validation = MutableStateFlow(ScanValidationState.NONE)

    private val isInvalidQrSheetVisible = MutableStateFlow(false)

    private val mutex = Mutex()

    private var hasBeenScannedSuccessfully = false

    private var sheetJob: Job? = null

    val state: StateFlow<ViewOnlyScanState> =
        validation
            .map(::createState)
            .stateIn(viewModel = this, initialValue = createState(validation.value))

    val invalidQrSheet: StateFlow<ZashiConfirmationState?> =
        isInvalidQrSheetVisible
            .map { isVisible ->
                ViewOnlyInvalidQrSheet
                    .create(
                        onPasteClick = ::onPasteClick,
                        onScanAgainClick = ::hideInvalidQrSheet,
                    ).takeIf { isVisible }
            }.stateIn(viewModel = this, initialValue = null)

    private fun createState(validation: ScanValidationState) =
        ViewOnlyScanState.create(validation = validation, onBack = ::onBack)

    fun onScanned(result: String) = viewModelScope.launch { handle(result) }

    fun onImageScanned(result: ImageToQrCodeResult) =
        viewModelScope.launch {
            when (result) {
                is ImageToQrCodeResult.SingleCode -> handle(result.text)
                ImageToQrCodeResult.MultipleCodes -> setValidation(ScanValidationState.SEVERAL_CODES_FOUND)
                ImageToQrCodeResult.NoCode -> setValidation(ScanValidationState.INVALID_IMAGE)
            }
        }

    private suspend fun handle(text: String) =
        mutex.withLock {
            if (hasBeenScannedSuccessfully || isInvalidQrSheetVisible.value) return@withLock
            when (submitViewOnlyKey(text)) {
                SubmitViewOnlyKeyResult.Accepted -> {
                    hasBeenScannedSuccessfully = true
                    setValidation(ScanValidationState.VALID)
                    navigationRouter.replace(ViewOnlyNameArgs)
                }

                SubmitViewOnlyKeyResult.AlreadyImported -> {
                    hasBeenScannedSuccessfully = true
                    navigationRouter.replace(ViewOnlyAlreadyImportedArgs)
                }

                SubmitViewOnlyKeyResult.Invalid -> {
                    setValidation(ScanValidationState.NONE)
                    isInvalidQrSheetVisible.update { true }
                }
            }
        }

    private fun hideInvalidQrSheet() = isInvalidQrSheetVisible.update { false }

    /**
     * Hides the sheet, then returns to the paste screen, which is what opened the scanner.
     */
    private fun onPasteClick() {
        if (sheetJob?.isActive == true) return
        sheetJob =
            viewModelScope.launch {
                hideInvalidQrSheet()
                delay(SHEET_HIDE_DELAY)
                navigationRouter.back()
            }
    }

    private fun onBack() = navigationRouter.back()

    private fun setValidation(validation: ScanValidationState) = this.validation.update { validation }

    private companion object {
        val SHEET_HIDE_DELAY = 350.milliseconds
    }
}
