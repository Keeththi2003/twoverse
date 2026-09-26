package app.twoverse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val ChipHeight = 36.dp
private val ChipHorizontalPadding = 14.dp

/** Selectable filter chip: primary when selected, chip colour otherwise. */
@Composable
fun TwoverseChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val shape = TwoverseTheme.shapes.chip
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = ChipHeight)
            .clip(shape)
            .background(if (selected) colors.primary else colors.chip)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = ChipHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onPrimary else colors.onSurface,
        )
    }
}

@PreviewLightDark
@Composable
private fun TwoverseChipPreview() {
    TwoversePreviewBackground {
        Row(horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs)) {
            TwoverseChip(label = "All", selected = true, onClick = {})
            TwoverseChip(label = "From you", selected = false, onClick = {})
            TwoverseChip(label = "Expiring", selected = false, onClick = {})
        }
    }
}
