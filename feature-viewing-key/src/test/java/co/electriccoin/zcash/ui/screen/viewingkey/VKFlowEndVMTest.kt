package co.electriccoin.zcash.ui.screen.viewingkey

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.screen.viewingkey.alreadyimported.AlreadyImportedVKVM
import co.electriccoin.zcash.ui.screen.viewingkey.imported.ImportedVKVM
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

/**
 * Both ends of the flow close it: the account is imported and selected, or it was in the wallet
 * already.
 */
class VKFlowEndVMTest {
    @Test
    fun doneAndBackOnTheSuccessScreenReturnToTheRoot() {
        val navigationRouter = mockk<NavigationRouter>(relaxed = true)
        val state = ImportedVKVM(navigationRouter).state.value

        state.doneButton.onClick()
        state.onBack()

        verify(exactly = 2) { navigationRouter.backToRoot() }
    }

    @Test
    fun closingTheAlreadyImportedSheetReturnsToTheRoot() {
        val navigationRouter = mockk<NavigationRouter>(relaxed = true)
        val state = AlreadyImportedVKVM(navigationRouter).state.value

        state.closeButton.onClick()

        verify(exactly = 1) { navigationRouter.backToRoot() }
    }
}
