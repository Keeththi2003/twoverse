package app.twoverse.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.text.partnerString
import app.twoverse.core.designsystem.text.partnerName
import app.twoverse.core.designsystem.text.PronounStrings
import app.twoverse.R
import app.twoverse.core.designsystem.component.SegmentedOptions
import app.twoverse.core.designsystem.component.SettingsSwitchRow
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoverseCircleIconButton
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.MemoryExpiry
import coil3.compose.AsyncImage
import kotlin.math.roundToInt

private val PhotoAreaHeight = 200.dp

/** "She can save it in Ours permanently." (FR-MEM-5) */
private val KeepSubtitle = PronounStrings(
    R.string.add_memory_keep_subtitle_she,
    R.string.add_memory_keep_subtitle_he,
    R.string.add_memory_keep_subtitle_they,
)

/** "She gets a notification, never a preview of the photo." (FR-NOT-1) */
private val SendNote = PronounStrings(R.string.add_memory_send_note_she, R.string.add_memory_send_note_he, R.string.add_memory_send_note_they)
private val PhotoBorderWidth = 2.dp
private val PhotoDash = 6.dp

/** Matches `TwoverseTheme.shapes.card` so the dashed border follows the clip. */
private val PhotoCornerRadius = 24.dp
private val PickerIconCircle = 56.dp
private val PickerIconSize = 24.dp
private val HeaderButtonSpace = 48.dp
private const val CaptionCounterFrom = 400
private val UploadTrackHeight = 6.dp
private const val PercentMax = 100

/** New memory (FR-MEM-1 to FR-MEM-6). */
@Composable
fun AddMemoryScreen(
    uiState: AddMemoryUiState,
    onClose: () -> Unit,
    onPickPhoto: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onExpirySelected: (MemoryExpiry) -> Unit,
    onAllowKeepChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl)
                .padding(bottom = spacing.xs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                AddMemoryHeader(onClose = onClose)
                PhotoPicker(
                    photoUri = uiState.photoUri,
                    onPickPhoto = onPickPhoto,
                    modifier = Modifier.padding(top = spacing.lg),
                )
                TwoverseTextField(
                    value = uiState.caption,
                    onValueChange = onCaptionChange,
                    label = stringResource(R.string.add_memory_caption_label),
                    placeholder = stringResource(R.string.add_memory_caption_placeholder, partnerName()),
                    singleLine = false,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                    modifier = Modifier.padding(top = spacing.lg),
                )
                if (uiState.caption.length >= CaptionCounterFrom) {
                    Text(
                        text = stringResource(
                            R.string.add_memory_caption_count,
                            uiState.caption.length,
                            AddMemoryUiState.CaptionMaxLength,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.xxs),
                    )
                }
                Text(
                    text = stringResource(R.string.add_memory_expiry_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.lg),
                )
                SegmentedOptions(
                    options = MemoryExpiry.entries.map { stringResource(it.labelRes()) },
                    selectedIndex = uiState.expiry.ordinal,
                    onSelect = { onExpirySelected(MemoryExpiry.entries[it]) },
                    modifier = Modifier.padding(top = spacing.xxs),
                )
                if (uiState.showKeepOption) {
                    TwoverseCard(
                        shape = TwoverseTheme.shapes.cardSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.md),
                    ) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.add_memory_keep_title, partnerName()),
                            subtitle = partnerString(KeepSubtitle),
                            checked = uiState.allowKeep,
                            onCheckedChange = onAllowKeepChange,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(top = spacing.xl)) {
                if (uiState.sendFailed) {
                    Text(
                        text = stringResource(R.string.add_memory_upload_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.xs)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (uiState.isSending) {
                    UploadProgress(progress = uiState.uploadProgress, modifier = Modifier.padding(bottom = spacing.sm))
                }
                TwoversePrimaryButton(
                    text = if (uiState.sendFailed) stringResource(R.string.add_memory_retry) else stringResource(R.string.add_memory_send, partnerName()),
                    onClick = onSend,
                    enabled = uiState.canSend,
                    leadingIcon = R.drawable.ic_send,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = partnerString(SendNote),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xs),
                )
            }
        }
    }
}

/** Upload progress (FR-MEM-6), announced to screen readers as it changes. */
@Composable
private fun UploadProgress(progress: Float, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val percent = (progress * PercentMax).roundToInt()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.add_memory_sending, percent),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        LinearProgressIndicator(
            progress = { progress },
            color = colors.primary,
            trackColor = colors.chip,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TwoverseTheme.spacing.xs)
                .height(UploadTrackHeight)
                .clip(TwoverseTheme.shapes.circle),
        )
    }
}

@Composable
private fun AddMemoryHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TwoverseCircleIconButton(
            icon = R.drawable.ic_close,
            contentDescription = stringResource(R.string.common_close),
            onClick = onClose,
        )
        Text(
            text = stringResource(R.string.add_memory_title),
            style = MaterialTheme.typography.titleLarge,
            color = TwoverseTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.size(HeaderButtonSpace))
    }
}

@Composable
private fun PhotoPicker(photoUri: String?, onPickPhoto: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val shape = TwoverseTheme.shapes.card
    val description = stringResource(
        if (photoUri == null) R.string.add_memory_choose_photo else R.string.add_memory_change_photo,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PhotoAreaHeight)
            .clip(shape)
            .background(colors.surface)
            .drawBehind {
                if (photoUri != null) return@drawBehind
                val stroke = PhotoBorderWidth.toPx()
                drawRoundRect(
                    color = colors.outline,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(PhotoCornerRadius.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(PhotoDash.toPx(), PhotoDash.toPx())),
                    ),
                )
            }
            .clickable(role = Role.Button, onClick = onPickPhoto)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = stringResource(R.string.add_memory_chosen_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            return@Box
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(PickerIconCircle)
                    .background(colors.chip, TwoverseTheme.shapes.circle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(PickerIconSize),
                )
            }
            Text(
                text = stringResource(R.string.add_memory_choose_photo),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.add_memory_photo_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

private fun MemoryExpiry.labelRes(): Int = when (this) {
    MemoryExpiry.Never -> R.string.add_memory_expiry_never
    MemoryExpiry.Hours24 -> R.string.add_memory_expiry_24h
    MemoryExpiry.Days7 -> R.string.add_memory_expiry_7d
    MemoryExpiry.Days30 -> R.string.add_memory_expiry_30d
}

@PreviewLightDark
@Composable
private fun AddMemoryScreenPreview() {
    TwoverseTheme {
        AddMemoryScreen(
            uiState = AddMemoryUiState(expiry = MemoryExpiry.Days7, allowKeep = true),
            onClose = {},
            onPickPhoto = {},
            onCaptionChange = {},
            onExpirySelected = {},
            onAllowKeepChange = {},
            onSend = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AddMemoryScreenSendingPreview() {
    TwoverseTheme {
        AddMemoryScreen(
            uiState = AddMemoryUiState(photoUri = "content://photo", caption = "Sunset", isSending = true, uploadProgress = 0.45f),
            onClose = {},
            onPickPhoto = {},
            onCaptionChange = {},
            onExpirySelected = {},
            onAllowKeepChange = {},
            onSend = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AddMemoryScreenFailedPreview() {
    TwoverseTheme {
        AddMemoryScreen(
            uiState = AddMemoryUiState(photoUri = "content://photo", caption = "Sunset", sendFailed = true),
            onClose = {},
            onPickPhoto = {},
            onCaptionChange = {},
            onExpirySelected = {},
            onAllowKeepChange = {},
            onSend = {},
        )
    }
}
