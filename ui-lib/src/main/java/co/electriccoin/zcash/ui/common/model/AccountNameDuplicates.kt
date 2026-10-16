package co.electriccoin.zcash.ui.common.model

import java.util.Locale

/**
 * The rule that keeps account names unique in the app. The wallet database allows two accounts to
 * share a name, so the app refuses a name another account already shows. Names are compared in their
 * saved form, [AccountNameLimit.normalize], ignoring case.
 */
object AccountNameDuplicates {
    /**
     * Whether [name] matches the name of any account in [namesByAccount] other than [excluding],
     * which is the account being renamed, so keeping its own name is never a duplicate. A name that
     * is blank once normalized is never a duplicate; it is refused as blank instead.
     */
    fun <K> isDuplicate(
        name: String,
        namesByAccount: Map<K, String>,
        excluding: K? = null
    ): Boolean {
        val candidate = comparable(name)
        if (candidate.isEmpty()) return false
        return namesByAccount.any { (account, existing) -> account != excluding && comparable(existing) == candidate }
    }

    private fun comparable(name: String) = AccountNameLimit.normalize(name).lowercase(Locale.ROOT)
}
