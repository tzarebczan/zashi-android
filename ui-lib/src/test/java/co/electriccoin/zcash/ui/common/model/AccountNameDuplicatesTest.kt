package co.electriccoin.zcash.ui.common.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A name is a duplicate when another account shows it, compared in its saved form and ignoring case;
 * the account being renamed never counts, and a blank name is never a duplicate.
 */
class AccountNameDuplicatesTest {
    private val names = mapOf(ZODL to "Zodl", VAULT to "Zodl Vault", KEYSTONE to "Keystone")

    @Test
    fun aNameAnotherAccountShowsIsADuplicateInAnyCase() {
        assertTrue(AccountNameDuplicates.isDuplicate("Zodl", names))
        assertTrue(AccountNameDuplicates.isDuplicate("zodl", names))
        assertTrue(AccountNameDuplicates.isDuplicate("KEYSTONE", names, excluding = VAULT))
    }

    @Test
    fun surroundingWhitespaceIsIgnoredOnBothSides() {
        assertTrue(AccountNameDuplicates.isDuplicate("  zodl vault ", names))
        assertTrue(AccountNameDuplicates.isDuplicate("Savings", mapOf(VAULT to " savings  ")))
    }

    @Test
    fun aNameCappedToAnotherAccountsNameIsADuplicate() {
        assertTrue(AccountNameDuplicates.isDuplicate("Zodl Savings 2026", mapOf(VAULT to "Zodl Savings")))
    }

    @Test
    fun theAccountsOwnNameIsNotADuplicate() {
        assertFalse(AccountNameDuplicates.isDuplicate("Zodl Vault", names, excluding = VAULT))
        assertFalse(AccountNameDuplicates.isDuplicate("ZODL VAULT", names, excluding = VAULT))
    }

    @Test
    fun aUniqueOrBlankNameIsNotADuplicate() {
        assertFalse(AccountNameDuplicates.isDuplicate("Savings", names))
        assertFalse(AccountNameDuplicates.isDuplicate("Zodl 2", names))
        assertFalse(AccountNameDuplicates.isDuplicate("   ", mapOf(VAULT to "")))
        assertFalse(AccountNameDuplicates.isDuplicate("Zodl", emptyMap<String, String>()))
    }

    private companion object {
        const val ZODL = "zodl"
        const val VAULT = "vault"
        const val KEYSTONE = "keystone"
    }
}
