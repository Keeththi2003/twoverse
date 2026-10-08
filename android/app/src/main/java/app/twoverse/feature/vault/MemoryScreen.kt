package app.twoverse.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.text.partnerString
import app.twoverse.core.designsystem.text.partnerNameStart
import app.twoverse.core.designsystem.text.partnerName
import app.twoverse.core.designsystem.text.PronounStrings
import app.twoverse.core.common.formatDate
import app.twoverse.R
import app.twoverse.core.common.ExpiryBadge
import app.twoverse.core.designsystem.component.Planet
import app.twoverse.core.designsystem.component.PlanetKind
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCircleIconButton
import app.twoverse.core.designsystem.component.TwoverseConfirmDialog
import app.twoverse.core.designsystem.component.TwoverseOutlineButton
import app.twoverse.core.designsystem.component.TwoverseSecondaryButton
import app.twoverse.core.designsystem.component.TwoverseStatusChip
import app.twoverse.core.designsystem.text.longText
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError
import app.twoverse.core.model.MemorySender
import coil3.compose.AsyncImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val PhotoAspectRatio = 358f / 470f
private val PhotoOverlayInset = 16.dp
private val PlaceholderIconSize = 44.dp
private val SenderPlanetSize = 36.dp
private val DetailsHorizontalPadding = 8.dp
private val NoteIconSize = 14.dp
private const val DatePattern = "dMMMy"

/** "Ammu sent it, so she still has it." (SRS 12: the sender controls the memory.) */
private val HideMessage = PronounStrings(R.string.memory_hide_message_she, R.string.memory_hide_message_he, R.string.memory_hide_message_they)

/** Memory viewer (FR-VLT-8, FR-DEL-2, FR-DEL-3; the recipient hides instead of deleting, SRS 12). */
@Composable
fun MemoryScreen(
    uiState: MemoryUiState,
    onBack: () -> Unit,
    onUnlock: () -> Unit,
    onKeepForever: () -> Unit,
    onRemove: () -> Unit,
    onRemoveConfirmed: () -> Unit,
    onRemoveDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    if (uiState.isLocked) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .safeDrawingPadding()
                .padding(horizontal = spacing.md),
        ) {
            TwoverseBackButton(onClick = onBack)
            OursLocked(onUnlock = onUnlock, modifier = Modifier.weight(1f))
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md)
            .padding(bottom = spacing.md),
    ) {
        when (val content = uiState.content) {
            MemoryContent.Loading -> Unit
            MemoryContent.Expired -> ExpiredMemory(onBack = onBack)
            is MemoryContent.Viewing -> {
                MemoryPhoto(content = content, onBack = onBack, onRemove = onRemove)
                MemoryDetails(
                    content = content,
                    error = uiState.error,
                    onKeepForever = onKeepForever,
                    onRemove = onRemove,
                )
            }
        }
    }
    val removal = (uiState.content as? MemoryContent.Viewing)?.removal
    if (uiState.isRemoveDialogOpen && removal != null) {
        TwoverseConfirmDialog(
            title = stringResource(
                if (removal == MemoryRemoval.Delete) R.string.memory_delete_title else R.string.memory_hide_title,
            ),
            message = if (removal == MemoryRemoval.Delete) {
                stringResource(R.string.memory_delete_message)
            } else {
                partnerString(HideMessage, partnerNameStart())
            },
            confirmLabel = stringResource(removal.labelRes()),
            onConfirm = onRemoveConfirmed,
            onDismiss = onRemoveDismissed,
            destructive = removal == MemoryRemoval.Delete,
        )
    }
}

private fun MemoryRemoval.labelRes(): Int = when (this) {
    MemoryRemoval.Delete -> R.string.memory_delete
    MemoryRemoval.Hide -> R.string.memory_hide
}

private fun MemoryRemoval.iconRes(): Int = when (this) {
    MemoryRemoval.Delete -> R.drawable.ic_trash
    MemoryRemoval.Hide -> R.drawable.ic_eye_off
}

@Composable
private fun MemoryPhoto(content: MemoryContent.Viewing, onBack: () -> Unit, onRemove: () -> Unit) {
    val colors = TwoverseTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PhotoAspectRatio)
            .clip(TwoverseTheme.shapes.cardLarge)
            .background(colors.chip),
        contentAlignment = Alignment.Center,
    ) {
        if (content.imageUrl != null) {
            AsyncImage(
                model = content.imageUrl,
                contentDescription = if (content.sender == MemorySender.Me) {
                    stringResource(R.string.memory_photo_from_you)
                } else {
                    stringResource(R.string.memory_photo_from_partner, partnerName())
                },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(PlaceholderIconSize),
                )
                Text(
                    text = stringResource(R.string.memory_photo_placeholder),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(PhotoOverlayInset - TwoverseTheme.spacing.xxs / 2),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TwoverseBackButton(onClick = onBack, elevated = false)
            MoreOptions(removal = content.removal, onRemove = onRemove)
        }
        content.expiryBadge?.let { badge ->
            TwoverseStatusChip(
                text = stringResource(R.string.memory_expires_in, badge.longText()),
                icon = R.drawable.ic_clock,
                containerColor = colors.surface,
                textStyle = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(PhotoOverlayInset),
            )
        }
    }
}

@Composable
private fun MoreOptions(removal: MemoryRemoval, onRemove: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        TwoverseCircleIconButton(
            icon = R.drawable.ic_more,
            contentDescription = stringResource(R.string.memory_more_options),
            onClick = { expanded = true },
            elevated = false,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = TwoverseTheme.colors.surface,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(removal.labelRes()),
                        color = if (removal == MemoryRemoval.Delete) TwoverseTheme.colors.error else TwoverseTheme.colors.onSurface,
                    )
                },
                onClick = {
                    expanded = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun MemoryDetails(
    content: MemoryContent.Viewing,
    error: DataError?,
    onKeepForever: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Column(modifier = Modifier.padding(start = DetailsHorizontalPadding, end = DetailsHorizontalPadding, top = spacing.lg)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Planet(
                kind = if (content.sender == MemorySender.Me) PlanetKind.You else PlanetKind.Her,
                size = SenderPlanetSize,
            )
            Column {
                Text(
                    text = if (content.sender == MemorySender.Me) {
                        stringResource(R.string.memory_from_you)
                    } else {
                        stringResource(R.string.memory_from_partner, partnerName())
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onSurface,
                )
                Text(
                    text = sentAtText(content.sentAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        content.caption?.let { caption ->
            Text(
                text = caption,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                color = colors.onSurface,
                modifier = Modifier.padding(top = spacing.smd),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (content.canKeepForever) {
                TwoverseSecondaryButton(
                    text = stringResource(R.string.memory_keep_forever),
                    onClick = onKeepForever,
                    leadingIcon = R.drawable.ic_heart,
                    small = true,
                    modifier = Modifier.weight(1f),
                )
            }
            TwoverseOutlineButton(
                text = stringResource(content.removal.labelRes()),
                onClick = onRemove,
                leadingIcon = content.removal.iconRes(),
                small = true,
                accentColor = if (content.removal == MemoryRemoval.Delete) colors.error else colors.onSurfaceVariant,
                modifier = if (content.canKeepForever) Modifier else Modifier.weight(1f),
            )
        }
        error?.let {
            Text(
                text = stringResource(it.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.sm)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        ScreenshotNote(modifier = Modifier.padding(top = spacing.md))
    }
}

@Composable
private fun sentAtText(sentAt: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val date = formatDate(sentAt, DatePattern, locale)
    val time = sentAt.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    return stringResource(R.string.memory_date_time, date, time)
}

@Composable
private fun ScreenshotNote(modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(NoteIconSize),
        )
        Text(
            text = stringResource(R.string.memory_screenshots_blocked),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpiredMemory(onBack: () -> Unit) {
    TwoverseBackButton(onClick = onBack)
    Text(
        text = stringResource(R.string.memory_expired),
        style = MaterialTheme.typography.headlineSmall,
        color = TwoverseTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwoverseTheme.spacing.xxl * 2),
    )
}

@PreviewLightDark
@Composable
private fun MemoryScreenPreview() {
    TwoverseTheme {
        MemoryScreen(
            uiState = MemoryUiState(
                content = MemoryContent.Viewing(
                    sender = MemorySender.Partner,
                    imageUrl = null,
                    caption = "Our first sunset call.",
                    sentAt = LocalDateTime.of(2026, 9, 24, 21, 12),
                    expiryBadge = ExpiryBadge.Days(2),
                    canKeepForever = true,
                    removal = MemoryRemoval.Hide,
                ),
            ),
            onBack = {},
            onUnlock = {},
            onKeepForever = {},
            onRemove = {},
            onRemoveConfirmed = {},
            onRemoveDismissed = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MemoryScreenExpiredPreview() {
    TwoverseTheme {
        MemoryScreen(
            uiState = MemoryUiState(content = MemoryContent.Expired),
            onBack = {},
            onUnlock = {},
            onKeepForever = {},
            onRemove = {},
            onRemoveConfirmed = {},
            onRemoveDismissed = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MemoryScreenSentPreview() {
    TwoverseTheme {
        MemoryScreen(
            uiState = MemoryUiState(
                content = MemoryContent.Viewing(
                    sender = MemorySender.Me,
                    imageUrl = null,
                    caption = null,
                    sentAt = LocalDateTime.of(2026, 9, 25, 8, 30),
                    expiryBadge = null,
                    canKeepForever = false,
                    removal = MemoryRemoval.Delete,
                ),
                error = DataError.Network,
            ),
            onBack = {},
            onUnlock = {},
            onKeepForever = {},
            onRemove = {},
            onRemoveConfirmed = {},
            onRemoveDismissed = {},
        )
    }
}
