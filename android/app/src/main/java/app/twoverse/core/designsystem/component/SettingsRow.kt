package app.twoverse.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val RowMinHeight = 56.dp
private val RowWithSubtitleMinHeight = 58.dp
private val RowVerticalPadding = 8.dp
private val ChevronSize = 18.dp
private val SubtitleGap = 2.dp

/** Settings row with a trailing switch. The whole row toggles. */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    SettingsRowLayout(
        title = title,
        subtitle = subtitle,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        TwoverseSwitch(checked = checked, onCheckedChange = null)
    }
}

/** Settings row showing the current value and a chevron. The whole row is clickable. */
@Composable
fun SettingsValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val muted = TwoverseTheme.colors.onSurfaceVariant
    SettingsRowLayout(
        title = title,
        subtitle = subtitle,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = muted,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(ChevronSize),
        )
    }
}

@Composable
private fun SettingsRowLayout(
    title: String,
    subtitle: String?,
    modifier: Modifier,
    trailing: @Composable RowScope.() -> Unit,
) {
    val spacing = TwoverseTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) RowMinHeight else RowWithSubtitleMinHeight)
            .padding(horizontal = spacing.md, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SubtitleGap),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = TwoverseTheme.colors.onSurfaceVariant,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
            content = trailing,
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsRowPreview() {
    TwoversePreviewBackground {
        TwoverseCard(shape = TwoverseTheme.shapes.group) {
            SettingsSwitchRow(
                title = "Share my location",
                subtitle = "Only used for distance and your star",
                checked = true,
                onCheckedChange = {},
            )
            HorizontalDivider(color = TwoverseTheme.colors.outline)
            SettingsValueRow(title = "Location precision", value = "Approximate", onClick = {})
        }
    }
}
