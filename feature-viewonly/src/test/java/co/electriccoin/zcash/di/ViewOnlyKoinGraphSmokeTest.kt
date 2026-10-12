package co.electriccoin.zcash.di

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository
import co.electriccoin.zcash.ui.common.usecase.ClearClipboardUseCase
import co.electriccoin.zcash.ui.common.usecase.GetWalletAccountsUseCase
import co.electriccoin.zcash.ui.common.usecase.ImportViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.SubmitViewOnlyKeyUseCase
import co.electriccoin.zcash.ui.common.usecase.ValidateUfvkUseCase
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyAccountImporter
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavContributor
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavigator
import co.electriccoin.zcash.ui.screen.viewonly.addaccount.ViewOnlyAddAccountVM
import co.electriccoin.zcash.ui.screen.viewonly.alreadyimported.ViewOnlyAlreadyImportedVM
import co.electriccoin.zcash.ui.screen.viewonly.imported.ViewOnlyImportedVM
import co.electriccoin.zcash.ui.screen.viewonly.name.ViewOnlyNameVM
import co.electriccoin.zcash.ui.screen.viewonly.paste.ViewOnlyPasteVM
import co.electriccoin.zcash.ui.screen.viewonly.scan.ViewOnlyScanVM
import co.electriccoin.zcash.viewonly.di.featureViewOnlyModule
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Smoke-tests the Koin graph of [featureViewOnlyModule], like LedgerKoinGraphSmokeTest does for its
 * module: every contract ui-lib resolves, [ViewOnlyContent] included, and every use case and view
 * model of the import flow must come out of the module plus stubs for the types ui-lib owns. A
 * missing binding fails here with a NoDefinitionFoundException instead of when a screen opens on a
 * device. The main dispatcher is a standard test dispatcher so work the view models launch at
 * construction never runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewOnlyKoinGraphSmokeTest {
    private lateinit var koin: KoinApplication

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        val stubsModule =
            module {
                single<NavigationRouter> { mockk(relaxed = true) }
                single<AccountDataSource> { mockk(relaxed = true) }
                single<SynchronizerProvider> { mockk(relaxed = true) }
                factory { mockk<GetWalletAccountsUseCase>(relaxed = true) }
                factory { mockk<ClearClipboardUseCase>(relaxed = true) }
            }

        koin =
            koinApplication {
                modules(featureViewOnlyModule, stubsModule)
            }
    }

    @AfterTest
    fun tearDown() {
        koin.close()
        Dispatchers.resetMain()
    }

    @Test
    fun contracts_resolveFromKoin() {
        koin.koin.get<ViewOnlyNavContributor>()
        koin.koin.get<ViewOnlyNavigator>()
        koin.koin.get<ViewOnlyAccountImporter>()
    }

    @Test
    fun useCases_resolveFromKoin() {
        koin.koin.get<ValidateUfvkUseCase>()
        koin.koin.get<SubmitViewOnlyKeyUseCase>()
        koin.koin.get<ImportViewOnlyAccountUseCase>()
    }

    @Test
    fun viewModels_resolveFromKoin() {
        koin.koin.get<ViewOnlyAddAccountVM>()
        koin.koin.get<ViewOnlyPasteVM>()
        koin.koin.get<ViewOnlyScanVM>()
        koin.koin.get<ViewOnlyNameVM>()
        koin.koin.get<ViewOnlyImportedVM>()
        koin.koin.get<ViewOnlyAlreadyImportedVM>()
    }

    @Test
    fun content_resolvesFromKoin() {
        koin.koin.get<ViewOnlyContent>()
    }

    @Test
    fun pendingImport_isOneSharedInstance() {
        assertSame(koin.koin.get<ViewOnlyPendingImportRepository>(), koin.koin.get<ViewOnlyPendingImportRepository>())
    }
}
