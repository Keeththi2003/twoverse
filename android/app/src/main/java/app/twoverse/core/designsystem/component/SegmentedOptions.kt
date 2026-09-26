package app.twoverse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val SegmentHeight = 44.dp

/** Equal-width single-choice options, e.g. memory expiry (Never / 24 h / 7 days / 30 days). */
@Composable
fun SegmentedOptions(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val shape = TwoverseTheme.shapes.segment
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .heightIn(min = SegmentHeight)
                    .clip(shape)
                    .background(if (selected) colors.primary else colors.surface)
                    .border(1.dp, if (selected) colors.primary else colors.outline, shape)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) colors.onPrimary else colors.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SegmentedOptionsPreview() {
    TwoversePreviewBackground {
        SegmentedOptions(
            options = listOf("Never", "24 h", "7 days", "30 days"),
            selectedIndex = 2,
            onSelect = {},
        )
    }
}
