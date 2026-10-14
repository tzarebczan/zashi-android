package co.electriccoin.zcash.ui.screen.viewingkey.edit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountNameLimit
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.repository.MetadataRepository
import co.electriccoin.zcash.ui.common.usecase.GetViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToColorPickerUseCase
import co.electriccoin.zcash.ui.common.usecase.RenameViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.screen.error.ErrorArgs
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase
import co.electriccoin.zcash.ui.screen.viewingkey.EditVKAccountArgs
import co.electriccoin.zcash.ui.screen.viewingkey.RemoveVKAccountArgs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Renames the view-only account [EditVKAccountArgs.accountKeyId] names, or opens the sheet that
 * removes it. The field starts with the account's current name and is capped at
 * [ViewOnlyAccountNameLimit.MAX_LENGTH] code points as it is typed; Save stores a new, non-blank name
 * and returns to the account list, which already shows it; a failure to store it shows the error
 * screen and leaves the field as typed. An id that names no view-only account, the Zodl account or a
 * hardware wallet included, goes straight back. The avatar shows the account's stored colour; its
 * pencil badge opens the colour picker on the colour shown, and a newly picked colour enables Save as
 * a new name does. Save stores whichever of the two changed; a colour that could not be stored shows
 * the error screen and keeps the picked colour, as a failed rename does.
 */
internal class EditVKAccountVM(
    args: EditVKAccountArgs,
    private val getViewOnlyAccount: GetViewOnlyAccountUseCase,
    private val renameViewOnlyAccount: RenameViewOnlyAccountUseCase,
    private val navigateToError: NavigateToErrorUseCase,
    private val navigationRouter: NavigationRouter,
    private val navigateToColorPicker: NavigateToColorPickerUseCase,
    private val metadataRepository: MetadataRepository,
) : ViewModel() {
    private val accountKeyId = args.accountKeyId

    /** Null until the account's current name has been put into the field. */
    private val name = MutableStateFlow<String?>(null)

    private val isSaving = MutableStateFlow(false)

    /** The colour picked on this screen, null until one is. */
    private val pickedColor = MutableStateFlow<Color?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val storedColor: StateFlow<Color?> =
        getViewOnlyAccount
            .observe(accountKeyId)
            .filterNotNull()
            .map { it.sdkAccount.accountUuid }
            .distinctUntilChanged()
            .flatMapLatest(metadataRepository::observeColor)
            .stateIn(viewModel = this, initialValue = null)

    private var saveJob: Job? = null

    private var colorPickerJob: Job? = null

    val state: StateFlow<EditVKAccountState?> =
        combine(
            getViewOnlyAccount
                .observe(accountKeyId)
                .filterNotNull()
                .map { it.currentName() }
                .distinctUntilChanged(),
            name.filterNotNull(),
            isSaving,
            storedColor,
            pickedColor,
        ) { currentName, name, isSaving, stored, picked ->
            createState(
                currentName = currentName,
                name = name,
                isSaving = isSaving,
                color = picked ?: stored,
                isColorChanged = newColor(picked, stored) != null
            )
        }.stateIn(this)

    init {
        viewModelScope.launch {
            val account = getViewOnlyAccount(accountKeyId)
            if (account == null) {
                navigationRouter.back()
            } else {
                name.update { it ?: ViewOnlyAccountNameLimit.cap(account.currentName()) }
            }
        }
    }

    private fun createState(
        currentName: String,
        name: String,
        isSaving: Boolean,
        color: Color?,
        isColorChanged: Boolean,
    ) = EditVKAccountState.create(
        name = name,
        currentName = currentName,
        isSaving = isSaving,
        color = color,
        isColorChanged = isColorChanged,
        onAvatarEditClick = ::onAvatarEditClick,
        onValueChange = ::onValueChange,
        onSaveClick = ::onSaveClick,
        onRemoveClick = ::onRemoveClick,
        onBack = ::onBack,
    )

    private fun onAvatarEditClick() {
        if (colorPickerJob?.isActive == true) return
        colorPickerJob =
            viewModelScope.launch {
                navigateToColorPicker(pickedColor.value ?: storedColor.value)?.let { picked ->
                    pickedColor.update { picked }
                }
            }
    }

    private fun onValueChange(value: String) = name.update { ViewOnlyAccountNameLimit.cap(value) }

    @Suppress("TooGenericExceptionCaught")
    private fun onSaveClick() {
        if (saveJob?.isActive == true) return
        val typed = name.value ?: return
        val picked = pickedColor.value
        saveJob =
            viewModelScope.launch {
                val account = getViewOnlyAccount(accountKeyId)
                if (account == null) {
                    navigationRouter.back()
                    return@launch
                }
                val renames = EditVKAccountState.canSave(typed, account.currentName())
                val color = newColor(picked, storedColor.value)
                if (!renames && color == null) return@launch
                isSaving.update { true }
                try {
                    if (renames) renameViewOnlyAccount(account, typed)
                    val isColorSaved = color == null || metadataRepository.setAccountColor(account, color)
                    ensureActive()
                    if (isColorSaved) {
                        navigationRouter.back()
                    } else {
                        navigateToError(ErrorArgs.General(AccountColorNotSavedException()))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    navigateToError(ErrorArgs.General(e))
                } finally {
                    isSaving.update { false }
                }
            }
    }

    private fun onRemoveClick() {
        if (saveJob?.isActive == true) return
        navigationRouter.forward(RemoveVKAccountArgs(accountKeyId))
    }

    private fun onBack() {
        saveJob?.cancel()
        colorPickerJob?.cancel()
        navigationRouter.back()
    }

    /**
     * The name the account shows now, as the field edits it: the stored or imported name. An account
     * that shows the default view-only name has no name of its own, so the field starts empty.
     */
    private fun ViewOnlyAccount.currentName(): String = customName.orEmpty()

    /** [picked] when it differs from the [stored] colour, otherwise null. */
    private fun newColor(
        picked: Color?,
        stored: Color?
    ): Color? = picked?.takeIf { it.toArgb() != stored?.toArgb() }
}

/** The picked colour could not be written to the account's metadata. */
internal class AccountColorNotSavedException : Exception("The account colour was not saved")
