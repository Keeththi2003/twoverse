package app.twoverse.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.common.ExpiryBadge
import app.twoverse.core.designsystem.text.longText
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.MemorySender
import coil3.compose.AsyncImage

private val TileHeight = 112.dp
private val PlaceholderIconSize = 26.dp
private const val PlaceholderIconAlpha = 0.7f
private val BadgeInset = 8.dp
private val BadgeHorizontalPadding = 8.dp
private val BadgeVerticalPadding = 3.dp
private val NewDotSize = 9.dp
private val NewDotInset = 10.dp

/** One memory in the vault grid: photo (or placeholder), expiry badge and new dot. */
@Composable
internal fun VaultTileItem(
    tile: VaultTile,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val shape = TwoverseTheme.shapes.vaultTile
    val description = tileDescription(tile)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TileHeight)
            .clip(shape)
            .background(tint)
            .then(if (colors.isDark) Modifier.border(1.dp, colors.outline, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (tile.imageUrl != null) {
            AsyncImage(
                model = tile.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_image),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier
                    .size(PlaceholderIconSize)
                    .alpha(PlaceholderIconAlpha),
            )
        }
        tile.expiryBadge?.let { badge ->
            Text(
                text = badge.shortText(),
                style = TwoverseTheme.textStyles.badge,
                color = colors.onSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(BadgeInset)
                    .background(colors.surface, TwoverseTheme.shapes.circle)
                    .padding(horizontal = BadgeHorizontalPadding, vertical = BadgeVerticalPadding),
            )
        }
        if (tile.isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(NewDotInset)
                    .size(NewDotSize)
                    .background(colors.accent, TwoverseTheme.shapes.circle),
            )
        }
    }
}

@Composable
private fun ExpiryBadge.shortText(): String = when (this) {
    is ExpiryBadge.Hours -> stringResource(R.string.vault_badge_hours, amount)
    is ExpiryBadge.Days -> stringResource(R.string.vault_badge_days, amount)
}

@Composable
private fun tileDescription(tile: VaultTile): String {
    val parts = buildList {
        add(
            stringResource(
                if (tile.sender == MemorySender.Me) R.string.vault_tile_from_you else R.string.vault_tile_from_her,
            ),
        )
        if (tile.isNew) add(stringResource(R.string.vault_tile_new))
        tile.expiryBadge?.let { add(stringResource(R.string.vault_tile_expires_in, it.longText())) }
    }
    return parts.joinToString(separator = ", ")
}
