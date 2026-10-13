package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.type.AddressType
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.datasource.AccountDataSource
import co.electriccoin.zcash.ui.common.provider.SelectedAccountSpendableStorageProvider
import co.electriccoin.zcash.ui.screen.contact.AddZashiABContactArgs
import co.electriccoin.zcash.ui.screen.error.NavigateToErrorUseCase
import co.electriccoin.zcash.ui.screen.scan.ScanArgs
import co.electriccoin.zcash.ui.screen.scan.ScanFlow.ADDRESS_BOOK
import co.electriccoin.zcash.ui.screen.scan.ScanFlow.HOMEPAGE
import co.electriccoin.zcash.ui.screen.scan.ScanFlow.SEND
import co.electriccoin.zcash.ui.screen.send.Send

class OnAddressScannedUseCase(
    private val navigationRouter: NavigationRouter,
    private val prefillSend: PrefillSendUseCase,
    private val accountDataSource: AccountDataSource,
    private val navigateToError: NavigateToErrorUseCase,
    private val selectedAccountSpendableStorageProvider: SelectedAccountSpendableStorageProvider,
) {
    /**
     * An address scanned from home opens the send form, so an account that cannot spend gets the
     * unsupported-operation error in place of the scanner instead.
     */
    suspend operator fun invoke(
        address: String,
        addressType: AddressType,
        scanArgs: ScanArgs
    ) {
        require(addressType is AddressType.Valid)

        when (scanArgs.flow) {
            SEND -> {
                prefillSend.request(PrefillSendData.FromAddressScan(address = address))
                navigationRouter.back()
            }

            ADDRESS_BOOK -> {
                navigationRouter.replace(AddZashiABContactArgs(address))
            }

            HOMEPAGE -> {
                val rejected =
                    navigateToError.rejectIfSelectedAccountCannotSpend(
                        accountDataSource,
                        selectedAccountSpendableStorageProvider
                    ) { replace(it) }
                if (rejected) return
                navigationRouter.replace(
                    Send(
                        address,
                        when (addressType) {
                            AddressType.Shielded -> cash.z.ecc.sdk.model.AddressType.UNIFIED
                            AddressType.Tex -> cash.z.ecc.sdk.model.AddressType.TEX
                            AddressType.Transparent -> cash.z.ecc.sdk.model.AddressType.TRANSPARENT
                            AddressType.Unified -> cash.z.ecc.sdk.model.AddressType.UNIFIED
                            else -> cash.z.ecc.sdk.model.AddressType.UNIFIED
                        }
                    )
                )
            }
        }
    }
}
