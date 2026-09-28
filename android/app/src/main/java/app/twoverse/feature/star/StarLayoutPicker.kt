package app.twoverse.feature.star

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoversePreviewBackground
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.StarLayout

private const val ThumbnailAspectRatio = 9f / 16f
private const val LineAlpha = 0.45f
private val ThumbnailPadding = 8.dp
private val SelectedBorder = 2.dp
private val UnselectedBorder = 1.dp
private val ThumbPhotoSize = 26.dp
private val ThumbStarSize = 8.dp
private val LineHeight = 3.dp
private val LineGap = 3.dp
private val ButtonHeight = 6.dp
private val WideLine = 34.dp
private val MediumLine = 26.dp
private val ShortLine = 18.dp

/** The three layouts, each with a small preview of how it looks (FR-STAR-2). */
@Composable
internal fun StarLayoutPicker(selected: StarLayout, onSelect: (StarLayout) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
    ) {
        StarLayout.entries.forEach { layout ->
            LayoutOption(
                layout = layout,
                selected = layout == selected,
                onClick = { onSelect(layout) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@StringRes
internal fun StarLayout.labelRes(): Int = when (this) {
    StarLayout.PhotoMessage -> R.string.star_layout_photo_message
    StarLayout.MessageOnly -> R.string.star_layout_message_only
    StarLayout.FullPhoto -> R.string.star_layout_full_photo
}

@Composable
private fun LayoutOption(layout: StarLayout, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val shape = TwoverseTheme.shapes.cardSmall
    Column(
        modifier = modifier
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(TwoverseTheme.spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ThumbnailAspectRatio)
                .clip(shape)
                .background(colors.background)
                .border(
                    width = if (selected) SelectedBorder else UnselectedBorder,
                    color = if (selected) colors.primary else colors.outline,
                    shape = shape,
                ),
        ) {
            when (layout) {
                StarLayout.PhotoMessage -> PhotoMessageThumbnail()
                StarLayout.MessageOnly -> MessageOnlyThumbnail()
                StarLayout.FullPhoto -> FullPhotoThumbnail()
            }
        }
        Text(
            text = stringResource(layout.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhotoMessageThumbnail() {
    ThumbnailColumn(verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(LineGap)) {
            Box(
                modifier = Modifier
                    .padding(bottom = LineGap)
                    .size(ThumbPhotoSize)
                    .clip(TwoverseTheme.shapes.segment)
                    .background(TwoverseTheme.colors.lavender),
            )
            TextLines(TwoverseTheme.colors.onSurfaceVariant)
        }
        ThumbButton()
    }
}

@Composable
private fun MessageOnlyThumbnail() {
    ThumbnailColumn(verticalArrangement = Arrangement.SpaceBetween) {
        Spacer(modifier = Modifier.height(ButtonHeight))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(LineGap)) {
            Box(
                modifier = Modifier
                    .padding(bottom = LineGap)
                    .size(ThumbStarSize)
                    .clip(TwoverseTheme.shapes.circle)
                    .background(TwoverseTheme.colors.gold),
            )
            TextLines(TwoverseTheme.colors.onSurfaceVariant)
        }
        ThumbButton()
    }
}

@Composable
private fun FullPhotoThumbnail() {
    val colors = TwoverseTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.lavender),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.photoScrim.copy(alpha = 0f), colors.photoScrim)))
                .padding(ThumbnailPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LineGap),
        ) {
            Spacer(modifier = Modifier.height(ThumbPhotoSize))
            TextLines(colors.onPhotoScrim)
            Spacer(modifier = Modifier.height(LineGap))
            ThumbButton()
        }
    }
}

@Composable
private fun ThumbnailColumn(verticalArrangement: Arrangement.Vertical, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ThumbnailPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** Three lines standing in for the title and message. */
@Composable
private fun TextLines(color: Color) {
    listOf(WideLine, MediumLine, ShortLine).forEach { width -> Line(width, color) }
}

@Composable
private fun Line(width: Dp, color: Color) {
    Box(
        modifier = Modifier
            .width(width)
            .height(LineHeight)
            .clip(TwoverseTheme.shapes.circle)
            .background(color.copy(alpha = LineAlpha)),
    )
}

/** Stands in for the Enter Twoverse button. */
@Composable
private fun ThumbButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ButtonHeight)
            .clip(TwoverseTheme.shapes.circle)
            .background(TwoverseTheme.colors.primary),
    )
}

@PreviewLightDark
@Composable
private fun StarLayoutPickerPreview() {
    TwoversePreviewBackground {
        StarLayoutPicker(selected = StarLayout.MessageOnly, onSelect = {})
    }
}
