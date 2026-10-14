package co.electriccoin.zcash.ui.common.usecase

import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.model.toStorageKeyId
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * Real Zodl and view-only accounts for the rename and Edit Account tests, so `is` checks and names
 * behave as in the app.
 */
internal object EditAccountFixtures {
    val viewOnlyUuid: AccountUuid = AccountUuid.new(ByteArray(16) { (it + 1).toByte() })

    val zashiUuid: AccountUuid = AccountUuid.new(ByteArray(16) { it.toByte() })

    val viewOnlyKeyId: String = viewOnlyUuid.toStorageKeyId()

    val zashiKeyId: String = zashiUuid.toStorageKeyId()

    /** The drawable every test view-only account carries as its icon. */
    const val VIEW_ONLY_ICON = 43

    /**
     * A view-only account named [customName]; a null [customName] shows [defaultName], as an
     * account without a name of its own does.
     */
    fun viewOnly(
        customName: String? = "Zodl Vault",
        defaultName: StringResource = stringRes(DEFAULT_NAME_RES),
    ) = ViewOnlyAccount(
        sdkAccount = Account.new(viewOnlyUuid),
        unifiedAddress = "u",
        transparentAddress = "t",
        orchardBalance = null,
        saplingBalance = null,
        ironwoodBalance = null,
        transparentBalance = null,
        isSelected = false,
        name = customName?.let { stringRes(it) } ?: defaultName,
        customName = customName,
        icon = VIEW_ONLY_ICON,
    )

    fun zashi() =
        ZashiAccount(
            sdkAccount = Account.new(zashiUuid),
            unifiedAddress = "u",
            transparentAddress = "t",
            saplingAddress = "s",
            orchardBalance = null,
            saplingBalance = null,
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = true,
        )

    /** A use case over [accounts]; null means the accounts are not loaded yet. */
    fun getWalletAccounts(accounts: MutableStateFlow<List<WalletAccount>?>) =
        mockk<GetWalletAccountsUseCase> {
            every { observe() } returns accounts
            every { require() } returns accounts.filterNotNull()
        }

    /** Stands in for the module's default view-only name resource. */
    private const val DEFAULT_NAME_RES = 1
}
