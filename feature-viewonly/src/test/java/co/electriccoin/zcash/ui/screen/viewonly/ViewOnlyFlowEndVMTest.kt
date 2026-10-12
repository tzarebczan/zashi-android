package co.electriccoin.zcash.ui.screen.viewonly

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.screen.viewonly.alreadyimported.ViewOnlyAlreadyImportedVM
import co.electriccoin.zcash.ui.screen.viewonly.imported.ViewOnlyImportedVM
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

/**
 * Both ends of the flow close it: the account is imported and selected, or it was in the wallet
 * already.
 */
class ViewOnlyFlowEndVMTest {
    @Test
    fun doneAndBackOnTheSuccessScreenReturnToTheRoot() {
        val navigationRouter = mockk<NavigationRouter>(relaxed = true)
        val state = ViewOnlyImportedVM(navigationRouter).state.value

        state.doneButton.onClick()
        state.onBack()

        verify(exactly = 2) { navigationRouter.backToRoot() }
    }

    @Test
    fun closingTheAlreadyImportedSheetReturnsToTheRoot() {
        val navigationRouter = mockk<NavigationRouter>(relaxed = true)
        val state = ViewOnlyAlreadyImportedVM(navigationRouter).state.value

        state.closeButton.onClick()

        verify(exactly = 1) { navigationRouter.backToRoot() }
    }
}
