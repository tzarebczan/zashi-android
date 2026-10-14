package co.electriccoin.zcash.ui.common.usecase

import androidx.compose.ui.graphics.Color
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.ui.common.model.ViewOnlyAccount
import co.electriccoin.zcash.ui.common.model.ZashiAccount
import co.electriccoin.zcash.ui.common.model.metadata.MetadataV3
import co.electriccoin.zcash.ui.design.R
import co.electriccoin.zcash.ui.design.util.imageRes
import co.electriccoin.zcash.ui.design.util.stringRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every account's colour comes from the repository's shared metadata map, empty at first, an account
 * without a colour left out; a view-only icon takes its colour, the default circle without one, and
 * every other account shows its own drawable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveAccountIconUseCaseTest {
    private val metadata = MutableStateFlow<Map<AccountUuid, MetadataV3>>(emptyMap())

    private val useCase = AccountIconFixtures.useCase(metadata)

    @Test
    fun theColorsStartEmptyThenFollowTheRepository() =
        runTest {
            val emitted = mutableListOf<Map<AccountUuid, Color>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase.observeAll().toList(emitted) }

            metadata.value =
                mapOf(
                    VIEW_ONLY_UUID to AccountIconFixtures.metadata(Color(PICKED)),
                    ZASHI_UUID to AccountIconFixtures.metadata(null)
                )
            metadata.value = mapOf(VIEW_ONLY_UUID to AccountIconFixtures.metadata(Color(OTHER)))

            assertEquals(
                listOf(emptyMap(), mapOf(VIEW_ONLY_UUID to Color(PICKED)), mapOf(VIEW_ONLY_UUID to Color(OTHER))),
                emitted
            )
        }

    @Test
    fun aViewOnlyIconTakesItsColor() {
        assertEquals(
            AccountIconFixtures.viewOnlyIcon(Color(PICKED)),
            useCase.iconOf(mapOf(VIEW_ONLY_UUID to Color(PICKED)), viewOnly())
        )
    }

    @Test
    fun iconOfFallsBackToTheDefaultIcon() {
        assertEquals(AccountIconFixtures.viewOnlyIcon(), useCase.iconOf(emptyMap(), viewOnly()))
        assertEquals(imageRes(R.drawable.ic_item_zashi), useCase.iconOf(emptyMap(), zashi()))
    }

    private fun zashi() =
        ZashiAccount(
            sdkAccount = Account.new(ZASHI_UUID),
            unifiedAddress = "u",
            transparentAddress = "t",
            saplingAddress = "s",
            orchardBalance = null,
            saplingBalance = null,
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = true,
        )

    private fun viewOnly() =
        ViewOnlyAccount(
            sdkAccount = Account.new(VIEW_ONLY_UUID),
            unifiedAddress = "u",
            transparentAddress = "t",
            orchardBalance = null,
            saplingBalance = null,
            ironwoodBalance = null,
            transparentBalance = null,
            isSelected = false,
            name = stringRes("Savings"),
            customName = "Savings",
            icon = VIEW_ONLY_ICON,
        )

    private companion object {
        val ZASHI_UUID: AccountUuid = AccountUuid.new(ByteArray(16) { it.toByte() })
        val VIEW_ONLY_UUID: AccountUuid = AccountUuid.new(ByteArray(16) { (it + 1).toByte() })
        const val VIEW_ONLY_ICON = 43
        const val PICKED = 0x80BE38F3
        const val OTHER = 0xFF0088FF
    }
}
