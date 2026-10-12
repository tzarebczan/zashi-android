package co.electriccoin.zcash.ui.common.appbar

import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.Zip32AccountIndex
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.DistributionDimension
import co.electriccoin.zcash.ui.common.model.KeystoneAccount
import co.electriccoin.zcash.ui.common.model.LedgerAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.provider.GetVersionInfoProvider
import co.electriccoin.zcash.ui.common.repository.ConfigurationRepository
import co.electriccoin.zcash.ui.common.usecase.GetWalletAccountsUseCase
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.design.R
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.fixture.VersionInfoFixture
import co.electriccoin.zcash.ui.screen.integrations.IntegrationsArgs
import co.electriccoin.zcash.ui.screen.more.MoreArgs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The top app bar shows the selected account as the account itself describes it - its avatar and
 * name - and only a view-only account adds the small label from the view-only feature above the
 * name. A view-only account is not a hardware wallet, so it never changes where the more button
 * leads.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZashiTopAppBarVMTest {
    private val sdkAccount = Account.new(AccountUuid.new(ByteArray(16) { it.toByte() }))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun spendableAccountsShowTheirOwnAvatarAndNameWithoutALabel() {
        listOf(zashi(isSelected = true), keystone(isSelected = true), ledger(isSelected = true)).forEach { account ->
            val switch = assertNotNull(vm(listOf(account)).state.value.accountSwitchState)

            assertEquals(account.icon, switch.icon)
            assertEquals(account.name, switch.name)
            assertNull(switch.subtitle)
        }
    }

    @Test
    fun theVendorAvatarsAreTheOnesTheBarAlwaysShowed() {
        assertEquals(
            listOf(R.drawable.ic_item_zashi, R.drawable.ic_item_keystone, R.drawable.ic_item_ledger),
            listOf(zashi(isSelected = true), keystone(isSelected = true), ledger(isSelected = true)).map {
                assertNotNull(vm(listOf(it)).state.value.accountSwitchState).icon
            }
        )
    }

    @Test
    fun aViewOnlyAccountShowsItsAvatarAndNameUnderTheViewOnlyLabel() {
        val viewOnly = viewOnly(isSelected = true)
        val switch = assertNotNull(vm(listOf(zashi(isSelected = false), viewOnly)).state.value.accountSwitchState)

        assertEquals(VIEW_ONLY_ICON, switch.icon)
        assertEquals(stringRes("Savings"), switch.name)
        assertEquals(VIEW_ONLY_LABEL, switch.subtitle)
    }

    @Test
    fun theSwitchShimmersUntilAnAccountIsSelected() {
        assertNull(vm(accounts = null).state.value.accountSwitchState)
        assertNull(vm(listOf(zashi(isSelected = false))).state.value.accountSwitchState)
    }

    @Test
    fun aViewOnlyAccountDoesNotCountAsAConnectedHardwareWallet() =
        runTest {
            val navigationRouter = mockk<NavigationRouter>(relaxed = true)
            val vm =
                vm(
                    listOf(zashi(isSelected = false), viewOnly(isSelected = true)),
                    navigationRouter = navigationRouter,
                )

            val moreButton = vm.state.value.moreButton
            moreButton.onClick()

            verify(exactly = 1) { navigationRouter.forward(IntegrationsArgs) }
            verify(exactly = 0) { navigationRouter.forward(MoreArgs) }
        }

    @Test
    fun aConnectedHardwareWalletStillSkipsTheEmptyIntegrations() =
        runTest {
            val navigationRouter = mockk<NavigationRouter>(relaxed = true)
            val vm =
                vm(
                    listOf(zashi(isSelected = false), keystone(isSelected = true)),
                    navigationRouter = navigationRouter,
                )

            val moreButton = vm.state.value.moreButton
            moreButton.onClick()

            verify(exactly = 1) { navigationRouter.forward(MoreArgs) }
        }

    private fun vm(
        accounts: List<WalletAccount>?,
        navigationRouter: NavigationRouter = mockk(relaxed = true),
    ) = ZashiTopAppBarVM(
        getWalletAccountsUseCase =
            mockk<GetWalletAccountsUseCase> {
                every { observe() } returns MutableStateFlow(accounts)
            },
        standardPreferenceProvider = mockk(relaxed = true),
        navigationRouter = navigationRouter,
        getVersionInfo =
            mockk<GetVersionInfoProvider> {
                every { this@mockk() } returns
                    VersionInfoFixture.new(distributionDimension = DistributionDimension.FOSS)
            },
        configurationRepository =
            mockk<ConfigurationRepository> {
                coEvery { isFlexaAvailable() } returns false
            },
        viewOnlyContent =
            mockk<ViewOnlyContent> {
                every { topBarSubtitle } returns VIEW_ONLY_LABEL
            },
    )

    private fun zashi(isSelected: Boolean) =
        ZashiAccount(
            sdkAccount = sdkAccount,
            unifiedAddress = "u-zashi",
            transparentAddress = "t-zashi",
            saplingAddress = "s-zashi",
            orchardBalance = null,
            saplingBalance = null,
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = isSelected,
        )

    private fun keystone(isSelected: Boolean) =
        KeystoneAccount(
            sdkAccount = sdkAccount,
            unifiedAddress = "u-keystone",
            transparentAddress = "t-keystone",
            orchardBalance = null,
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = isSelected,
        )

    private fun ledger(isSelected: Boolean) =
        LedgerAccount(
            sdkAccount = sdkAccount,
            unifiedAddress = "u-ledger",
            transparentAddress = "t-ledger",
            orchardBalance = null,
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = isSelected,
            deviceIdentity = null,
            zip32AccountIndex = Zip32AccountIndex.new(0L),
        )

    private fun viewOnly(isSelected: Boolean) =
        ViewOnlyAccount(
            sdkAccount = sdkAccount,
            unifiedAddress = "u-view-only",
            transparentAddress = "t-view-only",
            orchardBalance = null,
            saplingBalance = null,
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = isSelected,
            name = stringRes("Savings"),
            customName = "Savings",
            icon = VIEW_ONLY_ICON,
        )
}

/**
 * A resource-backed text: mockk cannot return the inline `StringResource.ByString` from a mocked
 * interface property.
 */
private val VIEW_ONLY_LABEL = StringResource.ByResource(resource = 7, args = emptyList())

private const val VIEW_ONLY_ICON = 43
