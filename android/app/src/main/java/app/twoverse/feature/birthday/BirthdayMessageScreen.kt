package app.twoverse.feature.birthday

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.IconTile
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.BirthdayMessageDraft
import app.twoverse.core.model.DataError
import coil3.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val PhotoHeight = 180.dp
private val PickerIconTileSize = 36.dp
private val PickerIconSize = 18.dp
private val PhotoIconSize = 28.dp
private const val CounterFrom = 900
private const val DatePattern = "EEEEdMMMMy"

/** Write the birthday welcome for the partner, with an optional photo and date (FR-BDY-1). */
@Composable
fun BirthdayMessageScreen(
    uiState: BirthdayMessageUiState,
    actions: BirthdayMessageActions,
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
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    TwoverseBackButton(onClick = actions.onBack)
                    Text(
                        text = stringResource(R.string.birthday_message_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Text(
                    text = stringResource(R.string.birthday_message_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                if (uiState.loadError != null) {
                    ErrorText(uiState.loadError)
                    TwoverseTextButton(text = stringResource(R.string.common_retry), onClick = actions.onRetryLoad)
                    return@Column
                }
                if (uiState.isLoading) return@Column
                MessageEditor(message = uiState.message, onMessageChange = actions.onMessageChange)
                PhotoPicker(photo = uiState.photo, onPickPhoto = actions.onPickPhoto, onRemovePhoto = actions.onRemovePhoto)
                ShowOnRow(showOn = uiState.showOn, onClick = actions.onOpenDatePicker)
                if (uiState.showOn != null) {
                    TwoverseTextButton(text = stringResource(R.string.birthday_message_clear_date), onClick = actions.onClearDate)
                }
                if (uiState.wasSeen) {
                    Text(
                        text = stringResource(R.string.birthday_message_seen_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                uiState.error?.let { ErrorText(it) }
            }
            TwoversePrimaryButton(
                text = stringResource(R.string.birthday_message_save),
                onClick = actions.onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xl),
            )
        }
    }
    if (uiState.isDatePickerOpen) {
        BirthdayDatePicker(
            initialDate = uiState.showOn,
            today = uiState.today,
            onDismiss = actions.onDismissDatePicker,
            onDateSelected = actions.onDateSelected,
        )
    }
}

@Composable
private fun MessageEditor(message: String, onMessageChange: (String) -> Unit) {
    Column {
        TwoverseTextField(
            value = message,
            onValueChange = onMessageChange,
            label = stringResource(R.string.birthday_message_label),
            placeholder = stringResource(R.string.birthday_message_placeholder),
            singleLine = false,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
        )
        if (message.length >= CounterFrom) {
            Text(
                text = stringResource(R.string.birthday_message_count, message.length, BirthdayMessageDraft.MaxMessageLength),
                style = MaterialTheme.typography.bodySmall,
                color = TwoverseTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwoverseTheme.spacing.xxs),
            )
        }
    }
}

@Composable
private fun PhotoPicker(photo: String?, onPickPhoto: () -> Unit, onRemovePhoto: () -> Unit) {
    val colors = TwoverseTheme.colors
    val description = stringResource(
        if (photo == null) R.string.birthday_message_add_photo else R.string.birthday_message_change_photo,
    )
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PhotoHeight)
                .clip(TwoverseTheme.shapes.card)
                .background(colors.surface)
                .clickable(role = Role.Button, onClick = onPickPhoto)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
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
                        tint = colors.primary,
                        modifier = Modifier.size(PhotoIconSize),
                    )
                    Text(
                        text = stringResource(R.string.birthday_message_add_photo),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                }
            }
        }
        if (photo != null) {
            TwoverseTextButton(
                text = stringResource(R.string.birthday_message_remove_photo),
                onClick = onRemovePhoto,
                color = colors.error,
            )
        }
    }
}

@Composable
private fun ShowOnRow(showOn: LocalDate?, onClick: () -> Unit) {
    val colors = TwoverseTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TwoverseTheme.spacing.minTouchTarget)
                .padding(TwoverseTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.smd),
        ) {
            IconTile(icon = R.drawable.ic_calendar, size = PickerIconTileSize, iconSize = PickerIconSize)
            Column {
                Text(
                    text = stringResource(R.string.birthday_message_show_on),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = showOn?.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, DatePattern), locale))
                        ?: stringResource(R.string.birthday_message_next_open),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ErrorText(error: DataError) {
    Text(
        text = stringResource(error.messageRes()),
        style = MaterialTheme.typography.bodySmall,
        color = TwoverseTheme.colors.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@PreviewLightDark
@Composable
private fun BirthdayMessageScreenPreview() {
    TwoverseTheme {
        BirthdayMessageScreen(
            uiState = BirthdayMessageUiState(
                today = LocalDate.of(2026, 9, 28),
                isLoading = false,
                message = "Happy birthday, my love. I built a little universe, just for the two of us.",
                showOn = LocalDate.of(2026, 10, 12),
                wasSeen = true,
            ),
            actions = BirthdayMessageActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun BirthdayMessageScreenEmptyPreview() {
    TwoverseTheme {
        BirthdayMessageScreen(
            uiState = BirthdayMessageUiState(today = LocalDate.of(2026, 9, 28), isLoading = false, error = DataError.Network),
            actions = BirthdayMessageActions(),
        )
    }
}
