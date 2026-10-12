package co.electriccoin.zcash.ui.screen.viewonly.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccountNameLimit
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.usecase.GetViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.RenameViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.screen.error.ErrorArgs
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyEditAccountArgs
import co.electriccoin.zcash.ui.screen.viewonly.ViewOnlyRemoveAccountArgs
import co.electriccoin.zcash.ui.screen.viewonly.name.ViewOnlyNameState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Renames the view-only account [ViewOnlyEditAccountArgs.accountKeyId] names, or opens the sheet that
 * removes it. The field starts with the account's current name and is capped at
 * [ViewOnlyNameState.MAX_NAME_LENGTH] code points as it is typed; Save stores a new, non-blank name
 * and returns to the account list, which already shows it; a failure to store it shows the error
 * screen and leaves the field as typed. An id that names no view-only account, the Zodl account or a
 * hardware wallet included, goes straight back.
 */
internal class ViewOnlyEditAccountVM(
    args: ViewOnlyEditAccountArgs,
    private val getViewOnlyAccount: GetViewOnlyAccountUseCase,
    private val renameViewOnlyAccount: RenameViewOnlyAccountUseCase,
    private val navigateToError: NavigateToErrorUseCase,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val accountKeyId = args.accountKeyId

    /** Null until the account's current name has been put into the field. */
    private val name = MutableStateFlow<String?>(null)

    private val isSaving = MutableStateFlow(false)

    private var saveJob: Job? = null

    val state: StateFlow<ViewOnlyEditAccountState?> =
        combine(
            getViewOnlyAccount.observe(accountKeyId).filterNotNull(),
            name.filterNotNull(),
            isSaving,
        ) { account, name, isSaving ->
            createState(account = account, name = name, isSaving = isSaving)
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

    override fun onCleared() {
        cancelSave()
        super.onCleared()
    }

    private fun createState(
        account: ViewOnlyAccount,
        name: String,
        isSaving: Boolean,
    ) = ViewOnlyEditAccountState.create(
        name = name,
        currentName = account.currentName(),
        isSaving = isSaving,
        onValueChange = ::onValueChange,
        onSaveClick = ::onSaveClick,
        onRemoveClick = ::onRemoveClick,
        onBack = ::onBack,
    )

    private fun onValueChange(value: String) = name.update { ViewOnlyAccountNameLimit.cap(value) }

    @Suppress("TooGenericExceptionCaught")
    private fun onSaveClick() {
        if (saveJob?.isActive == true) return
        val typed = name.value ?: return
        saveJob =
            viewModelScope.launch {
                val account = getViewOnlyAccount(accountKeyId)
                if (account == null) {
                    navigationRouter.back()
                    return@launch
                }
                if (!ViewOnlyEditAccountState.canSave(typed, account.currentName())) return@launch
                isSaving.update { true }
                try {
                    renameViewOnlyAccount(account, typed)
                    currentCoroutineContext().ensureActive()
                    navigationRouter.back()
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
        navigationRouter.forward(ViewOnlyRemoveAccountArgs(accountKeyId))
    }

    private fun onBack() {
        cancelSave()
        navigationRouter.back()
    }

    private fun cancelSave() {
        saveJob?.cancel()
        saveJob = null
    }

    /**
     * The name the account shows now, as the field edits it: the stored or imported name. An account
     * that shows the default view-only name has no name of its own, so the field starts empty.
     */
    private fun ViewOnlyAccount.currentName(): String = customName.orEmpty()
}
