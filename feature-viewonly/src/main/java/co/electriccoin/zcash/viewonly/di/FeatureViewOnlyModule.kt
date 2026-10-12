package co.electriccoin.zcash.viewonly.di

import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepository
import co.electriccoin.zcash.ui.common.repository.ViewOnlyPendingImportRepositoryImpl
import co.electriccoin.zcash.ui.common.usecase.GetViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.ImportViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.RemoveViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.RenameViewOnlyAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.SdkUfvkAddressDerivation
import co.electriccoin.zcash.ui.common.usecase.SubmitViewOnlyKeyUseCase
import co.electriccoin.zcash.ui.common.usecase.UfvkAddressDerivation
import co.electriccoin.zcash.ui.common.usecase.ValidateUfvkUseCase
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyAccountImporter
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavContributor
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavigator
import co.electriccoin.zcash.ui.screen.viewonly.addaccount.ViewOnlyAddAccountVM
import co.electriccoin.zcash.ui.screen.viewonly.alreadyimported.ViewOnlyAlreadyImportedVM
import co.electriccoin.zcash.ui.screen.viewonly.edit.ViewOnlyEditAccountVM
import co.electriccoin.zcash.ui.screen.viewonly.imported.ViewOnlyImportedVM
import co.electriccoin.zcash.ui.screen.viewonly.name.ViewOnlyNameVM
import co.electriccoin.zcash.ui.screen.viewonly.paste.ViewOnlyPasteVM
import co.electriccoin.zcash.ui.screen.viewonly.remove.ViewOnlyRemoveAccountVM
import co.electriccoin.zcash.ui.screen.viewonly.scan.ViewOnlyScanVM
import co.electriccoin.zcash.viewonly.ViewOnlyAccountImporterImpl
import co.electriccoin.zcash.viewonly.ViewOnlyContentImpl
import co.electriccoin.zcash.viewonly.ViewOnlyNavContributorImpl
import co.electriccoin.zcash.viewonly.ViewOnlyNavigatorImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Everything the view-only feature contributes to the app's Koin graph: the implementations of
 * ui-lib's view-only contracts (see ViewOnlyContracts.kt), the module's copy and images behind
 * [ViewOnlyContent], the in-memory pending import, and the use cases and view models of the import
 * flow and of the edit and remove flow.
 * Wired in ZcashApplication.startKoin next to the Ledger, migration and voting modules.
 */
val featureViewOnlyModule =
    module {
        singleOf(::ViewOnlyContentImpl) bind ViewOnlyContent::class
        singleOf(::ViewOnlyNavContributorImpl) bind ViewOnlyNavContributor::class
        singleOf(::ViewOnlyNavigatorImpl) bind ViewOnlyNavigator::class
        singleOf(::ViewOnlyAccountImporterImpl) bind ViewOnlyAccountImporter::class

        singleOf(::ViewOnlyPendingImportRepositoryImpl) bind ViewOnlyPendingImportRepository::class

        factoryOf(::SdkUfvkAddressDerivation) bind UfvkAddressDerivation::class
        factoryOf(::ValidateUfvkUseCase)
        factoryOf(::SubmitViewOnlyKeyUseCase)
        singleOf(::ImportViewOnlyAccountUseCase)
        factoryOf(::GetViewOnlyAccountUseCase)
        factoryOf(::RenameViewOnlyAccountUseCase)
        factoryOf(::RemoveViewOnlyAccountUseCase)

        viewModelOf(::ViewOnlyAddAccountVM)
        viewModelOf(::ViewOnlyPasteVM)
        viewModelOf(::ViewOnlyScanVM)
        viewModelOf(::ViewOnlyNameVM)
        viewModelOf(::ViewOnlyImportedVM)
        viewModelOf(::ViewOnlyAlreadyImportedVM)
        viewModelOf(::ViewOnlyEditAccountVM)
        viewModelOf(::ViewOnlyRemoveAccountVM)
    }
