package co.electriccoin.zcash.ui.screen.accountlist

import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.Zip32AccountIndex
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.KeystoneAccount
import co.electriccoin.zcash.ui.common.model.LedgerAccount
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.model.toStorageKeyId
import co.electriccoin.zcash.ui.common.usecase.AccountIconFixtures
import co.electriccoin.zcash.ui.common.usecase.GetWalletAccountsUseCase
import co.electriccoin.zcash.ui.common.usecase.SelectWalletAccountUseCase
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyNavigator
import co.electriccoin.zcash.ui.design.R
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.asPrivacySensitive
import co.electriccoin.zcash.ui.design.util.imageRes
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.ExternalUrl
import co.electriccoin.zcash.ui.screen.accountlist.options.WalletOptionsArgs
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
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue
import co.electriccoin.zcash.ui.R as UiR

/**
 * The full-screen "Wallets & Hardware" selector: the wallets grouped into Zodl, Keystone, Ledger and
 * view-only sections in that order whatever order they arrive in, each row with its avatar, its
 * balance (privacy-sensitive, a placeholder while unknown) and its trailing action; the Keystone
 * promo while no Keystone is connected; and the "Add Account" call to action once the wallets
 * are loaded.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountListVMTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sectionsFollowTheFixedOrderWhateverOrderTheAccountsArriveIn() =
        runTest {
            val vm =
                vm(
                    listOf(
                        viewOnly(id = 4, isSelected = false),
                        ledger(id = 3, isSelected = false),
                        zashi(id = 1, isSelected = true),
                        keystone(id = 2, isSelected = false),
                    )
                )
            collect(vm)

            val sections = assertNotNull(vm.state.value.sections)
            assertEquals(
                listOf(
                    stringRes(UiR.string.hwWallet_section_zodl),
                    stringRes(UiR.string.hwWallet_section_keystone),
                    stringRes(UiR.string.hwWallet_section_ledger),
                    VIEW_ONLY_SECTION,
                ),
                sections.map { it.title }
            )
            assertEquals(
                listOf(R.drawable.ic_item_zashi, R.drawable.ic_item_keystone, R.drawable.ic_item_ledger)
                    .map { imageRes(it) } + AccountIconFixtures.viewOnlyIcon(),
                sections.flatMap { section -> section.items.map { it.icon } }
            )
            assertTrue(sections.all { it.count == null })
        }

    @Test
    fun emptySectionsAreLeftOut() =
        runTest {
            val vm = vm(listOf(zashi(id = 1, isSelected = true), viewOnly(id = 2, isSelected = false)))
            collect(vm)

            assertEquals(
                listOf(stringRes(UiR.string.hwWallet_section_zodl), VIEW_ONLY_SECTION),
                assertNotNull(vm.state.value.sections).map { it.title }
            )
        }

    @Test
    fun severalViewOnlyAccountsTurnTheHeaderPluralAndShowTheirCount() =
        runTest {
            val vm =
                vm(
                    listOf(
                        zashi(id = 1, isSelected = true),
                        viewOnly(id = 2, isSelected = false),
                        viewOnly(id = 3, isSelected = false),
                        viewOnly(id = 4, isSelected = false),
                    )
                )
            collect(vm)

            val section = assertNotNull(vm.state.value.sections).last()
            assertEquals(VIEW_ONLY_SECTION_PLURAL, section.title)
            assertEquals(3, section.items.size)
            assertNotNull(section.count)
        }

    @Test
    fun onlyTheSelectedRowIsDrawnSelected() =
        runTest {
            val vm =
                vm(
                    listOf(
                        zashi(id = 1, isSelected = false),
                        keystone(id = 2, isSelected = false),
                        ledger(id = 3, isSelected = true),
                    )
                )
            collect(vm)

            val items = assertNotNull(vm.state.value.sections).flatMap { it.items }
            assertEquals(listOf(false, false, true), items.map { it.isSelected })
        }

    @Test
    fun everyRowIsTitledWithItsAccountName() =
        runTest {
            val accounts =
                listOf(
                    zashi(id = 1, isSelected = true),
                    keystone(id = 2, isSelected = false),
                    ledger(id = 3, isSelected = false),
                    viewOnly(id = 4, isSelected = false),
                )
            val vm = vm(accounts)
            collect(vm)

            assertEquals(
                accounts.map { it.name },
                assertNotNull(vm.state.value.sections).flatMap { section -> section.items.map { it.title } }
            )
        }

    @Test
    fun theZodlRowEndsInAChevronAndEveryOtherRowInTheOptionsAction() =
        runTest {
            val vm =
                vm(
                    listOf(
                        zashi(id = 1, isSelected = false),
                        keystone(id = 2, isSelected = true),
                        ledger(id = 3, isSelected = false),
                        viewOnly(id = 4, isSelected = false),
                    )
                )
            collect(vm)

            val items = assertNotNull(vm.state.value.sections).flatMap { it.items }
            assertEquals(
                listOf(
                    R.drawable.ic_wallet_list_chevron,
                    R.drawable.ic_wallet_list_more,
                    R.drawable.ic_wallet_list_more,
                    R.drawable.ic_wallet_list_more,
                ),
                items.map { assertNotNull(it.trailingAction).icon }
            )
        }

    @Test
    fun aRowShowsTheAccountsBalanceAsPrivacySensitiveText() =
        runTest {
            val vm =
                vm(
                    listOf(
                        zashi(id = 1, isSelected = true, balance = Zatoshi(100)),
                        keystone(id = 2, isSelected = false, balance = Zatoshi(10)),
                    )
                )
            collect(vm)

            val items = assertNotNull(vm.state.value.sections).flatMap { it.items }
            assertEquals(stringRes(Zatoshi(300)).asPrivacySensitive(), items[0].subtitle)
            assertEquals(stringRes(Zatoshi(30)).asPrivacySensitive(), items[1].subtitle)
        }

    @Test
    fun aViewOnlyRowAppendsTheViewOnlyLabelToItsBalance() =
        runTest {
            val vm = vm(listOf(zashi(id = 1, isSelected = true), viewOnly(id = 2, isSelected = false, Zatoshi(5))))
            collect(vm)

            val row = assertNotNull(vm.state.value.sections).last().items.single()
            assertEquals(stringRes(Zatoshi(15)).asPrivacySensitive() + " · " + VIEW_ONLY_LABEL, row.subtitle)
        }

    @Test
    fun aBalanceThatHasNotLoadedIsAPlaceholderNeverZero() =
        runTest {
            val vm =
                vm(
                    listOf(
                        zashi(id = 1, isSelected = true, balance = null),
                        viewOnly(id = 2, isSelected = false, balance = null),
                    )
                )
            collect(vm)

            val items = assertNotNull(vm.state.value.sections).flatMap { it.items }
            assertTrue(items.all { it.subtitle == null })
        }

    @Test
    fun tappingARowSelectsThatAccountAndCloses() =
        runTest {
            val selectWalletAccount = mockk<SelectWalletAccountUseCase>(relaxed = true)
            val ledger = ledger(id = 2, isSelected = false)
            val vm = vm(listOf(zashi(id = 1, isSelected = true), ledger), selectWalletAccount = selectWalletAccount)
            collect(vm)

            assertNotNull(vm.state.value.sections)[1].items.single().onClick()
            runCurrent()

            coVerify(exactly = 1) { selectWalletAccount.invoke(ledger, true) }
        }

    @Test
    fun anUnchangedAccountKeepsAnEqualRowAcrossEmissions() =
        runTest {
            val zashi = zashi(id = 1, isSelected = true)
            val keystone = keystone(id = 2, isSelected = false)
            val viewOnly = viewOnly(id = 3, isSelected = false)
            val accounts = MutableStateFlow<List<WalletAccount>?>(listOf(zashi, keystone, viewOnly))
            val vm = vm(accounts)
            collect(vm)
            val before = assertNotNull(vm.state.value.sections).flatMap { it.items }

            accounts.value = listOf(zashi.copy(), keystone.copy(transparentBalance = Zatoshi(2)), viewOnly.copy())
            runCurrent()

            val after = assertNotNull(vm.state.value.sections).flatMap { it.items }
            assertEquals(before[0], after[0])
            assertNotEquals(before[1], after[1])
            assertEquals(before[1].onClick, after[1].onClick)
            assertEquals(before[1].trailingAction?.onClick, after[1].trailingAction?.onClick)
            assertEquals(before[2], after[2])
        }

    @Test
    fun theCallbacksOfARemovedAccountAreDropped() =
        runTest {
            val zashi = zashi(id = 1, isSelected = true)
            val keystone = keystone(id = 2, isSelected = false)
            val accounts = MutableStateFlow<List<WalletAccount>?>(listOf(zashi, keystone))
            val vm = vm(accounts)
            collect(vm)
            val before = assertNotNull(vm.state.value.sections).flatMap { it.items }[1]

            accounts.value = listOf(zashi)
            runCurrent()
            accounts.value = listOf(zashi, keystone)
            runCurrent()

            val after = assertNotNull(vm.state.value.sections).flatMap { it.items }[1]
            assertNotSame(before.onClick, after.onClick)
            assertNotSame(before.trailingAction?.onClick, after.trailingAction?.onClick)
        }

    @Test
    fun tappingARowSelectsTheAccountAsItIsWhenTapped() =
        runTest {
            val selectWalletAccount = mockk<SelectWalletAccountUseCase>(relaxed = true)
            val ledger = ledger(id = 2, isSelected = false)
            val accounts = MutableStateFlow<List<WalletAccount>?>(listOf(zashi(id = 1, isSelected = true), ledger))
            val vm = vm(accounts, selectWalletAccount = selectWalletAccount)
            collect(vm)
            val onClick = assertNotNull(vm.state.value.sections)[1].items.single().onClick
            val updated = ledger.copy(transparentBalance = Zatoshi(7))

            accounts.value = listOf(zashi(id = 1, isSelected = true), updated)
            runCurrent()
            onClick()
            runCurrent()

            coVerify(exactly = 1) { selectWalletAccount.invoke(updated, true) }
        }

    @Test
    fun theChevronIsDecorativeAndTheOptionsButtonNamesItsWallet() =
        runTest {
            val vm = vm(listOf(zashi(id = 1, isSelected = true), viewOnly(id = 2, isSelected = false)))
            collect(vm)

            val sections = assertNotNull(vm.state.value.sections)
            assertNull(assertNotNull(sections[0].items.single().trailingAction).contentDescription)
            assertEquals(
                stringRes(UiR.string.hwWallet_walletOptions_contentDescription, stringRes("Savings")),
                assertNotNull(sections[1].items.single().trailingAction).contentDescription
            )
        }

    @Test
    fun theZodlChevronSelectsLikeTheRow() =
        runTest {
            val selectWalletAccount = mockk<SelectWalletAccountUseCase>(relaxed = true)
            val zashi = zashi(id = 1, isSelected = false)
            val vm = vm(listOf(zashi, keystone(id = 2, isSelected = true)), selectWalletAccount = selectWalletAccount)
            collect(vm)

            assertNotNull(assertNotNull(vm.state.value.sections)[0].items.single().trailingAction).onClick()
            runCurrent()

            coVerify(exactly = 1) { selectWalletAccount.invoke(zashi, true) }
        }

    @Test
    fun theOptionsOfAHardwareWalletOpenItsWalletOptionsWithoutSelectingIt() =
        runTest {
            val selectWalletAccount = mockk<SelectWalletAccountUseCase>(relaxed = true)
            val navigationRouter = mockk<NavigationRouter>(relaxed = true)
            val keystone = keystone(id = 2, isSelected = false)
            val ledger = ledger(id = 3, isSelected = false)
            val vm =
                vm(
                    listOf(zashi(id = 1, isSelected = true), keystone, ledger),
                    selectWalletAccount = selectWalletAccount,
                    navigationRouter = navigationRouter,
                )
            collect(vm)

            val sections = assertNotNull(vm.state.value.sections)
            assertNotNull(sections[1].items.single().trailingAction).onClick()
            assertNotNull(sections[2].items.single().trailingAction).onClick()
            runCurrent()

            verify(exactly = 1) {
                navigationRouter.forward(WalletOptionsArgs(keystone.sdkAccount.accountUuid.toStorageKeyId()))
            }
            verify(exactly = 1) {
                navigationRouter.forward(WalletOptionsArgs(ledger.sdkAccount.accountUuid.toStorageKeyId()))
            }
            coVerify(exactly = 0) { selectWalletAccount.invoke(any(), any()) }
        }

    @Test
    fun theOptionsOfAViewOnlyAccountOpenItsEditFlowWithoutSelectingIt() =
        runTest {
            val selectWalletAccount = mockk<SelectWalletAccountUseCase>(relaxed = true)
            val viewOnlyNavigator = mockk<ViewOnlyNavigator>(relaxed = true)
            val viewOnly = viewOnly(id = 2, isSelected = false)
            val vm =
                vm(
                    listOf(zashi(id = 1, isSelected = true), viewOnly),
                    selectWalletAccount = selectWalletAccount,
                    viewOnlyNavigator = viewOnlyNavigator,
                )
            collect(vm)

            assertNotNull(assertNotNull(vm.state.value.sections)[1].items.single().trailingAction).onClick()
            runCurrent()

            verify(exactly = 1) { viewOnlyNavigator.forwardToEditAccount(viewOnly.sdkAccount.accountUuid) }
            coVerify(exactly = 0) { selectWalletAccount.invoke(any(), any()) }
        }

    @Test
    fun thePromoShowsUntilAKeystoneIsConnected() =
        runTest {
            listOf(
                listOf(zashi(id = 1, isSelected = true)),
                listOf(zashi(id = 1, isSelected = true), ledger(id = 2, isSelected = false)),
                listOf(zashi(id = 1, isSelected = true), viewOnly(id = 2, isSelected = false)),
            ).forEach { accounts ->
                val vm = vm(accounts)
                collect(vm)
                assertNotNull(vm.state.value.keystonePromo)
            }

            listOf(
                listOf(zashi(id = 1, isSelected = true), keystone(id = 2, isSelected = false)),
                listOf(
                    zashi(id = 1, isSelected = true),
                    keystone(id = 2, isSelected = false),
                    ledger(id = 3, isSelected = false)
                ),
            ).forEach { accounts ->
                val vm = vm(accounts)
                collect(vm)
                assertNull(vm.state.value.keystonePromo)
            }
        }

    @Test
    fun thePromoOpensTheKeystoneShopInTheBrowserOnTopOfThisScreen() =
        runTest {
            val navigationRouter = mockk<NavigationRouter>(relaxed = true)
            val vm = vm(listOf(zashi(id = 1, isSelected = true)), navigationRouter = navigationRouter)
            collect(vm)

            val promo = assertNotNull(vm.state.value.keystonePromo)
            assertEquals(stringRes(UiR.string.hwWallet_promo_title), promo.title)
            promo.onClick()

            verify(exactly = 1) {
                navigationRouter.forward(ExternalUrl("https://keyst.one/shop/products/keystone-3-pro?discount=Zodl"))
            }
            verify(exactly = 0) { navigationRouter.replace(*anyVararg()) }
        }

    @Test
    fun theCallToActionShowsOnceTheAccountsAreLoadedWhateverIsConnected() =
        runTest {
            listOf(
                listOf(zashi(id = 1, isSelected = true)),
                listOf(zashi(id = 1, isSelected = true), keystone(id = 2, isSelected = false)),
                listOf(
                    zashi(id = 1, isSelected = true),
                    keystone(id = 2, isSelected = false),
                    ledger(id = 3, isSelected = false)
                ),
                listOf(zashi(id = 1, isSelected = false), viewOnly(id = 2, isSelected = true)),
            ).forEach { accounts ->
                val vm = vm(accounts)
                collect(vm)

                assertNotNull(vm.state.value.addAccountButton)
            }
        }

    @Test
    fun whileLoadingTheScreenShowsPlaceholdersOnly() =
        runTest {
            val vm = vm(accounts = null)
            collect(vm)

            val state = vm.state.value
            assertNull(state.sections)
            assertNull(state.keystonePromo)
            assertNull(state.addAccountButton)
            assertEquals(stringRes(UiR.string.hwWallet_drawer_title), state.title)
        }

    @Test
    fun theCallToActionCarriesTheViewOnlyCopyAndOpensTheAddAccountOptions() =
        runTest {
            val navigationRouter = mockk<NavigationRouter>(relaxed = true)
            val viewOnlyNavigator = mockk<ViewOnlyNavigator>(relaxed = true)
            val vm =
                vm(
                    listOf(zashi(id = 1, isSelected = true)),
                    navigationRouter = navigationRouter,
                    viewOnlyNavigator = viewOnlyNavigator,
                )
            collect(vm)

            val button = assertNotNull(vm.state.value.addAccountButton)
            assertEquals(ButtonStyle.PRIMARY, button.style)
            assertEquals(ADD_ACCOUNT, button.text)
            assertEquals(ADD_ACCOUNT_ICON, button.icon)
            button.onClick()

            verify(exactly = 1) { viewOnlyNavigator.forwardToAddAccount() }
            verify(exactly = 0) { navigationRouter.forward(any()) }
        }

    @Test
    fun backClosesTheScreen() =
        runTest {
            val navigationRouter = mockk<NavigationRouter>(relaxed = true)
            val vm = vm(listOf(zashi(id = 1, isSelected = true)), navigationRouter = navigationRouter)
            collect(vm)

            vm.state.value.onBack()

            verify(exactly = 1) { navigationRouter.back() }
        }

    private fun vm(
        accounts: List<WalletAccount>?,
        selectWalletAccount: SelectWalletAccountUseCase = mockk(relaxed = true),
        navigationRouter: NavigationRouter = mockk(relaxed = true),
        viewOnlyNavigator: ViewOnlyNavigator = mockk(relaxed = true),
    ) = vm(MutableStateFlow(accounts), selectWalletAccount, navigationRouter, viewOnlyNavigator)

    private fun vm(
        accounts: MutableStateFlow<List<WalletAccount>?>,
        selectWalletAccount: SelectWalletAccountUseCase = mockk(relaxed = true),
        navigationRouter: NavigationRouter = mockk(relaxed = true),
        viewOnlyNavigator: ViewOnlyNavigator = mockk(relaxed = true),
    ) = AccountListVM(
        getWalletAccounts =
            mockk<GetWalletAccountsUseCase> {
                every { observe() } returns accounts
            },
        selectWalletAccount = selectWalletAccount,
        navigationRouter = navigationRouter,
        viewOnlyNavigator = viewOnlyNavigator,
        viewOnlyContent =
            mockk<ViewOnlyContent> {
                every { addAccountButton } returns ADD_ACCOUNT
                every { addAccountButtonIcon } returns ADD_ACCOUNT_ICON
                every { accountListSection } returns VIEW_ONLY_SECTION
                every { accountListSectionPlural } returns VIEW_ONLY_SECTION_PLURAL
                every { topBarSubtitle } returns VIEW_ONLY_LABEL
            },
        observeAccountIcon = AccountIconFixtures.useCase(),
    )

    private fun sdkAccount(id: Int) = Account.new(AccountUuid.new(ByteArray(16) { (it + id * 16).toByte() }))

    /**
     * A pool balance of [amount], all of it available. The accounts below put it in every shielded
     * pool, so a Zodl account (no transparent funds) totals 3x and a Keystone or view-only account
     * (as much again in the transparent pool) 3x [amount] as well.
     */
    private fun balance(amount: Zatoshi?) = amount?.let { WalletBalance(it, Zatoshi(0), Zatoshi(0)) }

    private fun zashi(
        id: Int,
        isSelected: Boolean,
        balance: Zatoshi? = Zatoshi(1),
    ) = ZashiAccount(
        sdkAccount = sdkAccount(id),
        unifiedAddress = "u-zashi",
        transparentAddress = "t-zashi",
        saplingAddress = "s-zashi",
        orchardBalance = balance(balance),
        saplingBalance = balance(balance),
        ironwoodBalance = balance(balance),
        transparentBalance = balance?.let { Zatoshi(0) },
        isSelected = isSelected,
    )

    private fun keystone(
        id: Int,
        isSelected: Boolean,
        balance: Zatoshi? = Zatoshi(1),
    ) = KeystoneAccount(
        sdkAccount = sdkAccount(id),
        unifiedAddress = "u-keystone",
        transparentAddress = "t-keystone",
        orchardBalance = balance(balance),
        ironwoodBalance = balance(balance),
        transparentBalance = balance,
        isSelected = isSelected,
    )

    private fun ledger(
        id: Int,
        isSelected: Boolean,
    ) = LedgerAccount(
        sdkAccount = sdkAccount(id),
        unifiedAddress = "u-ledger",
        transparentAddress = "t-ledger",
        orchardBalance = balance(Zatoshi(1)),
        ironwoodBalance = balance(Zatoshi(1)),
        transparentBalance = Zatoshi(1),
        isSelected = isSelected,
        deviceIdentity = null,
        zip32AccountIndex = Zip32AccountIndex.new(0L),
    )

    private fun viewOnly(
        id: Int,
        isSelected: Boolean,
        balance: Zatoshi? = Zatoshi(1),
    ) = ViewOnlyAccount(
        sdkAccount = sdkAccount(id),
        unifiedAddress = "u-view-only",
        transparentAddress = "t-view-only",
        orchardBalance = balance(balance),
        saplingBalance = balance(balance?.let { Zatoshi(0) }),
        ironwoodBalance = balance(balance),
        transparentBalance = balance,
        isSelected = isSelected,
        name = stringRes("Savings"),
        customName = "Savings",
        icon = AccountIconFixtures.VIEW_ONLY_ICON,
    )

    private fun TestScope.collect(vm: AccountListVM) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
    }
}

/**
 * Resource-backed texts: mockk cannot return the inline `StringResource.ByString` from a mocked
 * interface property.
 */
private val ADD_ACCOUNT = StringResource.ByResource(resource = 1, args = emptyList())

private val VIEW_ONLY_SECTION = StringResource.ByResource(resource = 2, args = emptyList())

private val VIEW_ONLY_SECTION_PLURAL = StringResource.ByResource(resource = 3, args = emptyList())

private val VIEW_ONLY_LABEL = StringResource.ByResource(resource = 4, args = emptyList())

private const val ADD_ACCOUNT_ICON = 42
