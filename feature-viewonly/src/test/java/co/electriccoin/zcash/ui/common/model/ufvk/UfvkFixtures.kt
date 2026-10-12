package co.electriccoin.zcash.ui.common.model.ufvk

import co.electriccoin.zcash.ui.common.model.ufvk.UnifiedFullViewingKeyEncoding.Item
import java.io.ByteArrayOutputStream

/**
 * Public test vectors from librustzcash (`zcash_keys/src/keys.rs`, the UFVK and UIVK encoding
 * tests), derived there from a fixed test seed. They are no one's wallet keys. Variants the tests
 * need (another network, a missing component) are re-encoded from them with [encode].
 */
internal object UfvkFixtures {
    /** Mainnet UFVK with transparent, Sapling and Orchard components. */
    const val MAINNET_FULL =
        "uview1tg6rpjgju2s2j37gkgjq79qrh5lvzr6e0ed3n4sf4hu5qd35vmsh7avl80xa6mx7ryqce9hztwaqwrdthetp" +
            "y4pc0kce25x453hwcmax02p80pg5savlg865sft9reat07c5vlactr6l2pxtlqtqunt2j9gmvr8spcuzf07af80h5q" +
            "mut38h0gvcfa9k4rwujacwwca9vu8jev7wq6c725huv8qjmhss3hdj2vh8cfxhpqcm2qzc34msyrfxk5u6dqttt4vv" +
            "2mr0aajreww5yufpk0gn4xkfm888467k7v6fmw7syqq6cceu078yw8xja502jxr0jgum43lhvpzmf7eu5dmnn6cr6f" +
            "7p43yw8znzgxg598mllewnx076hljlvynhzwn5es94yrv65tdg3utuz2u3sras0wfcq4adxwdvlk387d22g3q98t5z" +
            "74quw2fa4wed32escx8dwh4mw35t4jwf35xyfxnu83mk5s4kw2glkgsshmxk"

    /** Mainnet UFVK with Sapling and Orchard components only. */
    const val MAINNET_WITHOUT_TRANSPARENT =
        "uview12z384wdq76ceewlsu0esk7d97qnd23v2qnvhujxtcf2lsq8g4hwzpx44fwxssnm5tg8skyh4tnc8gydwxefn" +
            "nm0hd0a6c6etmj0pp9jqkdsllkr70u8gpf7ndsfqcjlqn6dec3faumzqlqcmtjf8vp92h7kj38ph2786zx30hq2wru" +
            "8ae3excdwc8w0z3t9fuw7mt7xy5sn6s4e45kwm0cjp70wytnensgdnev286t3vew3yuwt2hcz865y037k30e428dvg" +
            "ne37xvyeal2vu8yjnznphf9t2rw3gdp0hk5zwq00ws8f3l3j5n3qkqgsyzrwx4qzmgq0xwwk4vz2r6vtsykgz089jn" +
            "cvycmem3535zjwvvtvjw8v98y0d5ydwte575gjm7a7k"

    /** Mainnet unified incoming viewing key. */
    const val MAINNET_UIVK =
        "uivk1z28yg638vjwusmf0zc9ad2j0mpv6s42wc5kqt004aaqfu5xxxgu7mdcydn9qf723fnryt34s6jyxyw0jt7spq" +
            "04c3v9ze6qe9gjjc5aglz8zv5pqtw58czd0actynww5n85z3052kzgy6cu0fyjafyp4sr4kppyrrwhwev2rr0awq6m" +
            "8d66esvk6fgacggqnswg5g9gkv6t6fj9ajhyd0gmel4yzscprpzduncc0e2lywufup6fvzf6y8cefez2r99pgge5yy" +
            "fuus0r60khgu895pln5e7nn77q6s9kh2uwf6lrfu06ma2kd7r05jjvl4hn6nupge8fajh0cazd7mkmz23t79w"

    fun encode(
        hrp: String,
        items: List<Item>,
    ): String {
        val raw = ByteArrayOutputStream()
        items.forEach { item ->
            writeCompactSize(raw, item.typecode.toLong())
            writeCompactSize(raw, item.data.size.toLong())
            raw.write(item.data)
        }
        raw.write(UnifiedFullViewingKeyEncoding.padding(hrp))
        return Bech32m.encode(hrp, F4Jumble.jumble(raw.toByteArray()))
    }

    fun itemsOf(
        ufvk: String,
        hrp: String = "uview",
    ): List<Item> = checkNotNull(UnifiedFullViewingKeyEncoding.decode(ufvk, hrp))

    @Suppress("MagicNumber")
    private fun writeCompactSize(
        out: ByteArrayOutputStream,
        value: Long,
    ) {
        require(value < 0xFD)
        out.write(value.toInt())
    }
}
