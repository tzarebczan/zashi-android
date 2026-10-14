package co.electriccoin.zcash.ui.common.usecase

import androidx.compose.ui.graphics.Color
import cash.z.ecc.android.sdk.model.AccountUuid
import co.electriccoin.zcash.ui.common.model.metadata.AccountAppearanceMetadataV3
import co.electriccoin.zcash.ui.common.model.metadata.AccountMetadataV3
import co.electriccoin.zcash.ui.common.model.metadata.MetadataV3
import co.electriccoin.zcash.ui.common.repository.MetadataRepository
import co.electriccoin.zcash.ui.common.viewonly.ViewOnlyContent
import co.electriccoin.zcash.ui.design.util.ImageResource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/** A real [ObserveAccountIconUseCase] over every account's [allMetadata]. */
internal object AccountIconFixtures {
    const val VIEW_ONLY_GLYPH = 4242

    fun useCase(allMetadata: StateFlow<Map<AccountUuid, MetadataV3>> = MutableStateFlow(emptyMap())) =
        ObserveAccountIconUseCase(
            metadataRepository = mockk<MetadataRepository> { every { metadata } returns allMetadata },
            viewOnlyContent = mockk<ViewOnlyContent> { every { accountIconForeground } returns VIEW_ONLY_GLYPH },
        )

    /** An account's metadata with [color] as its icon colour, none without one. */
    fun metadata(color: Color?) =
        MetadataV3(
            lastUpdated = Instant.ofEpochSecond(1),
            accountMetadata =
                AccountMetadataV3(
                    bookmarked = emptyList(),
                    read = emptyList(),
                    annotations = emptyList(),
                    appearance =
                        color?.let { AccountAppearanceMetadataV3(color = it, lastUpdated = Instant.ofEpochSecond(1)) }
                )
        )

    /** The view-only icon in [color], the default circle without one. */
    fun viewOnlyIcon(color: Color? = null) =
        ImageResource.Layered(
            background = color ?: Color.Unspecified,
            foreground = ImageResource.ByDrawable(VIEW_ONLY_GLYPH)
        )
}
