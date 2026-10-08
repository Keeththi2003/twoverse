package app.twoverse.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.text.partnerName
import app.twoverse.R
import app.twoverse.core.common.ExpiryBadge
import app.twoverse.core.designsystem.component.TwoverseChip
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.MemorySender

private const val GridColumns = 3
private val TitleIconSize = 22.dp
private val SubtitleGap = 6.dp
private val SectionGap = 18.dp
private val FabSize = 60.dp
private val FabIconSize = 26.dp
private val FabShadowRadius = 24.dp
private val FabShadowOffset = 10.dp
private const val FabShadowAlpha = 0.35f
private val GridBottomPadding = FabSize + 40.dp

/** Ours (FR-VLT-1 to FR-VLT-5, FR-VLT-9). */
@Composable
fun VaultScreen(
    uiState: VaultUiState,
    onFilterSelected: (VaultFilter) -> Unit,
    onOpenMemory: (String) -> Unit,
    onAddMemory: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
    ) {
        if (uiState is VaultUiState.Locked) {
            OursLocked(onUnlock = onUnlock)
            return@Box
        }
        if (uiState !is VaultUiState.Success) return@Box
        Column(modifier = Modifier.padding(horizontal = spacing.screenHorizontal)) {
            VaultHeader(totalCount = uiState.totalCount)
            FilterChips(
                selected = uiState.filter,
                onFilterSelected = onFilterSelected,
                modifier = Modifier.padding(top = SectionGap - spacing.xs),
            )
            if (uiState.tiles.isEmpty()) {
                EmptyVault(
                    messageRes = if (uiState.totalCount == 0) R.string.vault_empty else R.string.vault_filter_empty,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GridColumns),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    contentPadding = PaddingValues(top = SectionGap - spacing.xs, bottom = GridBottomPadding),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(uiState.tiles, key = { _, tile -> tile.id }) { index, tile ->
                        VaultTileItem(
                            tile = tile,
                            tint = colors.vaultTileTints[index % colors.vaultTileTints.size],
                            onClick = { onOpenMemory(tile.id) },
                        )
                    }
                }
            }
        }
        AddMemoryButton(
            onClick = onAddMemory,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(spacing.lg),
        )
    }
}

@Composable
private fun VaultHeader(totalCount: Int) {
    val colors = TwoverseTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.vault_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(TitleIconSize),
        )
    }
    Text(
        text = stringResource(
            R.string.vault_subtitle,
            pluralStringResource(R.plurals.home_memory_count, totalCount, totalCount),
        ),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(top = SubtitleGap),
    )
}

@Composable
private fun FilterChips(
    selected: VaultFilter,
    onFilterSelected: (VaultFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
    ) {
        VaultFilter.entries.forEach { filter ->
            TwoverseChip(
                label = filter.label(),
                selected = filter == selected,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun EmptyVault(messageRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = GridBottomPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = TwoverseTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddMemoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val shape = TwoverseTheme.shapes.circle
    val description = stringResource(R.string.vault_add_memory)
    Box(
        modifier = modifier
            .size(FabSize)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = FabShadowRadius,
                    color = colors.primary,
                    offset = DpOffset(0.dp, FabShadowOffset),
                    alpha = FabShadowAlpha,
                ),
            )
            .clip(shape)
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(FabIconSize),
        )
    }
}

@Composable
private fun VaultFilter.label(): String = when (this) {
    VaultFilter.All -> stringResource(R.string.vault_filter_all)
    VaultFilter.FromMe -> stringResource(R.string.vault_filter_from_you)
    VaultFilter.FromPartner -> stringResource(R.string.vault_filter_from_partner, partnerName())
    VaultFilter.Expiring -> stringResource(R.string.vault_filter_expiring)
}

@PreviewLightDark
@Composable
private fun VaultScreenPreview() {
    TwoverseTheme {
        VaultScreen(
            uiState = VaultUiState.Success(
                filter = VaultFilter.All,
                totalCount = 17,
                tiles = List(12) { index ->
                    VaultTile(
                        id = "memory-$index",
                        sender = if (index % 2 == 0) MemorySender.Partner else MemorySender.Me,
                        imageUrl = null,
                        isNew = index == 0 || index == 2,
                        expiryBadge = when (index) {
                            1 -> ExpiryBadge.Hours(24)
                            4 -> ExpiryBadge.Days(7)
                            8 -> ExpiryBadge.Days(2)
                            else -> null
                        },
                    )
                },
            ),
            onFilterSelected = {},
            onOpenMemory = {},
            onAddMemory = {},
            onUnlock = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun VaultScreenEmptyPreview() {
    TwoverseTheme {
        VaultScreen(
            uiState = VaultUiState.Success(filter = VaultFilter.All, totalCount = 0, tiles = emptyList()),
            onFilterSelected = {},
            onOpenMemory = {},
            onAddMemory = {},
            onUnlock = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun VaultScreenLockedPreview() {
    TwoverseTheme {
        VaultScreen(
            uiState = VaultUiState.Locked,
            onFilterSelected = {},
            onOpenMemory = {},
            onAddMemory = {},
            onUnlock = {},
        )
    }
}
