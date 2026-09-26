package app.twoverse.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.theme.TwoverseTheme

/** Single-choice dialog for a settings value (precision, unit, appearance). */
@Composable
internal fun OptionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwoverseTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        shape = TwoverseTheme.shapes.card,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = TwoverseTheme.spacing.minTouchTarget)
                            .selectable(
                                selected = index == selectedIndex,
                                role = Role.RadioButton,
                                onClick = { onSelect(index) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.primary,
                                unselectedColor = colors.onSurfaceVariant,
                            ),
                        )
                        Text(text = option, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TwoverseTextButton(text = stringResource(R.string.common_cancel), onClick = onDismiss)
        },
    )
}

@PreviewLightDark
@Composable
private fun OptionDialogPreview() {
    TwoverseTheme {
        OptionDialog(
            title = "Appearance",
            options = listOf("System", "Light", "Dark"),
            selectedIndex = 0,
            onSelect = {},
            onDismiss = {},
        )
    }
}
