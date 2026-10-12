package co.electriccoin.zcash.ui.screen.accountlist

import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.Zip32AccountIndex
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.KeystoneAccount
import co.electriccoin.zcash.ui.common.model.LedgerAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.usecase.GetWalletAccountsUseCase
import co.electriccoin.zcash.ui.common.usecase.SelectWalletAccountUseCase
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavigator
import co.electriccoin.zcash.ui.design.R
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The "Wallets & Hardware" sheet: one row per wallet with its vendor icon and the selected badge,
 * and an "Add Account" call to action that appears once the wallets are loaded and opens the
 * view-only feature's options sheet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountListVMTest {
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
    fun eachVendorGetsItsOwnIconAndOnlyTheSelectedRowIsMarked() =
        runTest {
            val vm = vm(listOf(zashi(isSelected = false), keystone(isSelected = false), ledger(isSelected = true)))
            collect(vm)

            val items = assertNotNull(vm.state.value?.items)
            assertEquals(
                listOf(R.drawable.ic_item_zashi, R.drawable.ic_item_keystone, R.drawable.ic_item_ledger),
                items.map { it.icon }
            )
            assertEquals(listOf(false, false, true), items.map { it.isSelected })
        }

    @Test
    fun aViewOnlyRowShowsTheNameAndAvatarTheAccountCarries() =
        runTest {
            val vm = vm(listOf(zashi(isSelected = false), viewOnly(isSelected = true)))
            collect(vm)

            val row = assertNotNull(vm.state.value?.items)[1]
            assertEquals(stringRes("Savings"), row.title)
            assertEquals(VIEW_ONLY_ICON, row.icon)
            assertTrue(row.isSelected)
        }

    @Test
    fun everyRowIsTitledWithItsAccountName() =
        runTest {
            val accounts =
                listOf(
                    zashi(isSelected = true),
                    keystone(isSelected = false),
                    ledger(isSelected = false),
                    viewOnly(isSelected = false),
                )
            val vm = vm(accounts)
            collect(vm)

            assertEquals(accounts.map { it.name }, assertNotNull(vm.state.value?.items).map { it.title })
        }

    @Test
    fun theCallToActionShowsOnceTheAccountsAreLoadedWhateverIsConnected() =
        runTest {
            listOf(
                listOf(zashi(isSelected = true)),
                listOf(zashi(isSelected = true), keystone(isSelected = false)),
                listOf(zashi(isSelected = true), keystone(isSelected = false), ledger(isSelected = false)),
                listOf(zashi(isSelected = false), viewOnly(isSelected = true)),
            ).forEach { accounts ->
                val vm = vm(accounts)
                collect(vm)

                assertNotNull(vm.state.value?.addWalletButton)
            }
        }

    @Test
    fun theCallToActionStaysHiddenWhileTheAccountsAreStillLoading() =
        runTest {
            val vm = vm(accounts = null)
            collect(vm)

            val state = assertNotNull(vm.state.value)
            assertNull(state.items)
            assertTrue(state.isLoading)
            assertNull(state.addWalletButton)
        }

    @Test
    fun theCallToActionCarriesTheViewOnlyCopyAndOpensTheAddAccountOptions() =
        runTest {
            val navigationRouter = mockk<NavigationRouter>(relaxed = true)
            val viewOnlyNavigator = mockk<ViewOnlyNavigator>(relaxed = true)
            val vm =
                vm(
                    listOf(zashi(isSelected = true)),
                    navigationRouter = navigationRouter,
                    viewOnlyNavigator = viewOnlyNavigator,
                )
            collect(vm)

            val button = assertNotNull(vm.state.value?.addWalletButton)
            assertEquals(ButtonStyle.PRIMARY, button.style)
            assertEquals(ADD_ACCOUNT, button.text)
            assertEquals(ADD_ACCOUNT_ICON, button.icon)
            button.onClick()

            verify(exactly = 1) { viewOnlyNavigator.forwardToAddAccount() }
            verify(exactly = 0) { navigationRouter.forward(any()) }
        }

    @Test
    fun tappingARowSelectsThatAccount() =
        runTest {
            val selectWalletAccount = mockk<SelectWalletAccountUseCase>(relaxed = true)
            val ledger = ledger(isSelected = false)
            val vm = vm(listOf(zashi(isSelected = true), ledger), selectWalletAccount = selectWalletAccount)
            collect(vm)

            assertNotNull(vm.state.value?.items)[1].onClick()
            runCurrent()

            coVerify(exactly = 1) { selectWalletAccount.invoke(ledger, true) }
        }

    private fun vm(
        accounts: List<WalletAccount>?,
        selectWalletAccount: SelectWalletAccountUseCase = mockk(relaxed = true),
        navigationRouter: NavigationRouter = mockk(relaxed = true),
        viewOnlyNavigator: ViewOnlyNavigator = mockk(relaxed = true),
    ) = AccountListVM(
        getWalletAccounts =
            mockk<GetWalletAccountsUseCase> {
                every { observe() } returns MutableStateFlow(accounts)
            },
        selectWalletAccount = selectWalletAccount,
        navigationRouter = navigationRouter,
        viewOnlyNavigator = viewOnlyNavigator,
        viewOnlyContent =
            mockk<ViewOnlyContent> {
                every { addAccountButton } returns ADD_ACCOUNT
                every { addAccountButtonIcon } returns ADD_ACCOUNT_ICON
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
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = isSelected,
            name = stringRes("Savings"),
            icon = VIEW_ONLY_ICON,
        )

    private fun TestScope.collect(vm: AccountListVM) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
    }
}

/**
 * A resource-backed text: mockk cannot return the inline `StringResource.ByString` from a mocked
 * interface property.
 */
private val ADD_ACCOUNT = StringResource.ByResource(resource = 1, args = emptyList())

private const val ADD_ACCOUNT_ICON = 42

private const val VIEW_ONLY_ICON = 43
