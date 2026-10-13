package co.electriccoin.zcash.ui.common.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the full viewing key and the name a view-only import collected until the account is
 * imported. Memory only: the key never reaches a navigation route, saved state, preferences or a
 * log, and process death drops it, after which the flow has to start again.
 */
interface ViewOnlyPendingImportRepository {
    fun get(): PendingViewOnlyImport?

    fun set(pendingImport: PendingViewOnlyImport)

    fun clear()
}

class ViewOnlyPendingImportRepositoryImpl : ViewOnlyPendingImportRepository {
    private val pending = MutableStateFlow<PendingViewOnlyImport?>(null)

    override fun get(): PendingViewOnlyImport? = pending.value

    override fun set(pendingImport: PendingViewOnlyImport) = pending.update { pendingImport }

    override fun clear() = pending.update { null }
}

/**
 * A full viewing key waiting to be imported under [name].
 */
data class PendingViewOnlyImport(
    val ufvk: String,
    val name: String,
) {
    /**
     * Overridden so the viewing key is never printed.
     */
    override fun toString() = "PendingViewOnlyImport(ufvk=***, name=$name)"
}
