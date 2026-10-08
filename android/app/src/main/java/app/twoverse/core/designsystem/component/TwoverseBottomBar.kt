package app.twoverse.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

@Immutable
data class TwoverseBottomBarItem(
    val label: String,
    @param:DrawableRes val icon: Int,
)

private val BarTopPadding = 10.dp
private val BarBottomPadding = 8.dp
private val IconSize = 24.dp

/** Tab bar. Which tabs it shows is decided in `navigation/`. */
@Composable
fun TwoverseBottomBar(
    items: List<TwoverseBottomBarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    Column(modifier = modifier.background(colors.surface)) {
        HorizontalDivider(thickness = 1.dp, color = colors.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = BarTopPadding, bottom = BarBottomPadding)
                .selectableGroup(),
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val contentColor = if (selected) colors.primary else colors.onSurfaceVariant
                // Equal widths, so five tabs share a 360 dp screen; each label stays on one line.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = TwoverseTheme.spacing.minTouchTarget)
                        .clip(TwoverseTheme.shapes.iconTile)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onItemSelected(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs),
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(IconSize),
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TwoverseBottomBarPreview() {
    TwoversePreviewBackground {
        TwoverseBottomBar(
            items = listOf(
                TwoverseBottomBarItem(stringResource(R.string.nav_universe), R.drawable.ic_orbit),
                TwoverseBottomBarItem(stringResource(R.string.nav_your_star), R.drawable.ic_compass),
                TwoverseBottomBarItem(stringResource(R.string.nav_ours), R.drawable.ic_lock),
                TwoverseBottomBarItem(stringResource(R.string.nav_our_orbit), R.drawable.ic_our_orbit),
                TwoverseBottomBarItem(stringResource(R.string.nav_us), R.drawable.ic_person),
            ),
            selectedIndex = 0,
            onItemSelected = {},
        )
    }
}
