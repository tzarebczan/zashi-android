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
 * Accounts for the remove tests: a real [ViewOnlyAccount] (so `is` checks and its name
 * behave as in the app) and a Zodl account that must never be renamed or removed.
 */
internal object ViewOnlyAccountFixtures {
    val viewOnlyUuid: AccountUuid = AccountUuid.new(ByteArray(16) { (it + 1).toByte() })

    val zashiUuid: AccountUuid = AccountUuid.new(ByteArray(16) { it.toByte() })

    val viewOnlyKeyId: String = viewOnlyUuid.toStorageKeyId()

    val zashiKeyId: String = zashiUuid.toStorageKeyId()

    /** Stands in for the module's default view-only name resource. */
    const val DEFAULT_NAME_RES = 1

    /**
     * A view-only account named [customName]; a null [customName] shows [defaultName], as an
     * account without a name of its own does.
     */
    fun viewOnly(
        customName: String? = "Zodl Vault",
        isSelected: Boolean = false,
        defaultName: StringResource = stringRes(DEFAULT_NAME_RES),
    ) = ViewOnlyAccount(
        sdkAccount = sdkAccount(viewOnlyUuid),
        unifiedAddress = "u",
        transparentAddress = "t",
        orchardBalance = null,
        saplingBalance = null,
        ironwoodBalance = null,
        transparentBalance = null,
        isSelected = isSelected,
        name = customName?.let { stringRes(it) } ?: defaultName,
        customName = customName,
        icon = 0,
    )

    fun zashi(isSelected: Boolean = false) =
        mockk<ZashiAccount> {
            every { sdkAccount } returns sdkAccount(zashiUuid)
            every { this@mockk.isSelected } returns isSelected
            every { isSpendable } returns true
        }

    /** A use case over [accounts]; null means the accounts are not loaded yet. */
    fun getWalletAccounts(accounts: MutableStateFlow<List<WalletAccount>?>) =
        mockk<GetWalletAccountsUseCase> {
            every { observe() } returns accounts
            every { require() } returns accounts.filterNotNull()
        }

    private fun sdkAccount(uuid: AccountUuid) =
        mockk<Account> {
            every { accountUuid } returns uuid
        }
}
