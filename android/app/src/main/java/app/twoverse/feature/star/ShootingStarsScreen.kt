package app.twoverse.feature.star

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoverseConfirmDialog
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseStatusChip
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError
import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val DateTimePattern = "EEEdMMMjmm"
private const val HeadlineMaxLines = 2

/** Everything the Shooting Stars list can ask for. */
data class ShootingStarsActions(
    val onBack: () -> Unit = {},
    val onCompose: () -> Unit = {},
    val onEdit: (String) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onDeleteConfirmed: () -> Unit = {},
    val onDeleteDismissed: () -> Unit = {},
    val onOpenReceived: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** Shooting Stars: the ones sent, with status, edit and delete while unseen (FR-STAR-9), and the ones received (FR-STAR-13). */
@Composable
fun ShootingStarsScreen(
    uiState: ShootingStarsUiState,
    actions: ShootingStarsActions,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = spacing.xl)
            .padding(bottom = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            TwoverseBackButton(onClick = actions.onBack)
            Text(
                text = stringResource(R.string.stars_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        TwoversePrimaryButton(
            text = stringResource(R.string.star_composer_title),
            onClick = actions.onCompose,
            leadingIcon = R.drawable.ic_plus,
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.error?.let { ErrorText(it) }

        SectionHeader(R.string.stars_sent)
        when {
            uiState.loadError != null -> {
                ErrorText(uiState.loadError)
                TwoverseTextButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
            }
            uiState.isLoading -> Unit
            uiState.sent.isEmpty() -> EmptyText(R.string.stars_sent_empty)
            else -> uiState.sent.forEach { item ->
                SentStarCard(item = item, onEdit = { actions.onEdit(item.id) }, onDelete = { actions.onDelete(item.id) })
            }
        }

        SectionHeader(R.string.stars_received)
        if (uiState.received.isEmpty()) {
            EmptyText(R.string.stars_received_empty)
        } else {
            uiState.received.forEach { item ->
                StarCard(item = item, onClick = { actions.onOpenReceived(item.id) })
            }
        }
    }
    if (uiState.pendingDeleteId != null) {
        TwoverseConfirmDialog(
            title = stringResource(R.string.stars_delete_title),
            message = stringResource(R.string.stars_delete_message),
            confirmLabel = stringResource(R.string.stars_delete),
            onConfirm = actions.onDeleteConfirmed,
            onDismiss = actions.onDeleteDismissed,
            destructive = true,
        )
    }
}

@Composable
private fun SentStarCard(item: StarListItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    StarCard(item = item, onClick = null) {
        if (item.isEditable) {
            Row(horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs)) {
                TwoverseTextButton(text = stringResource(R.string.stars_edit), onClick = onEdit)
                TwoverseTextButton(text = stringResource(R.string.stars_delete), onClick = onDelete, color = TwoverseTheme.colors.error)
            }
        }
    }
}

@Composable
private fun StarCard(item: StarListItem, onClick: (() -> Unit)?, footer: @Composable () -> Unit = {}) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.minTouchTarget)
                .padding(horizontal = spacing.md)
                .padding(top = spacing.md, bottom = if (item.isEditable && onClick == null) spacing.xxs else spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = item.headline ?: stringResource(item.layout.labelRes()),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                maxLines = HeadlineMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            TwoverseStatusChip(
                text = statusText(item),
                dotColor = when (item.status) {
                    StarStatus.Scheduled -> colors.lavender
                    StarStatus.Waiting -> colors.gold
                    StarStatus.Seen -> null
                },
                icon = if (item.status == StarStatus.Seen) R.drawable.ic_eye else null,
            )
            footer()
        }
    }
}

/** The status in words, so it never relies on the dot colour alone. */
@Composable
private fun statusText(item: StarListItem): String {
    val locale = LocalConfiguration.current.locales[0]
    val time = item.time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, DateTimePattern), locale))
    return when (item.status) {
        StarStatus.Scheduled -> stringResource(R.string.stars_status_scheduled, time)
        StarStatus.Waiting -> stringResource(R.string.stars_status_waiting)
        StarStatus.Seen -> stringResource(R.string.stars_status_seen)
    }
}

@Composable
private fun SectionHeader(@StringRes text: Int) {
    Text(
        text = stringResource(text).uppercase(LocalConfiguration.current.locales[0]),
        style = TwoverseTheme.textStyles.sectionHeader,
        color = TwoverseTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .padding(top = TwoverseTheme.spacing.xs)
            .semantics { heading() },
    )
}

@Composable
private fun EmptyText(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = TwoverseTheme.colors.onSurfaceVariant,
    )
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

private val PreviewTime: LocalDateTime = LocalDateTime.of(2026, 10, 12, 9, 0)

@PreviewLightDark
@Composable
private fun ShootingStarsScreenPreview() {
    TwoverseTheme {
        ShootingStarsScreen(
            uiState = ShootingStarsUiState(
                isLoading = false,
                sent = listOf(
                    StarListItem("1", "Happy Birthday", StarLayout.PhotoMessage, StarStatus.Scheduled, PreviewTime, isEditable = true),
                    StarListItem("2", "Good luck", StarLayout.MessageOnly, StarStatus.Waiting, PreviewTime.minusDays(3), isEditable = true),
                    StarListItem("3", null, StarLayout.FullPhoto, StarStatus.Seen, PreviewTime.minusDays(20), isEditable = false),
                ),
                received = listOf(
                    StarListItem("4", "Thinking of you", StarLayout.MessageOnly, StarStatus.Seen, PreviewTime.minusDays(5), isEditable = false),
                ),
            ),
            actions = ShootingStarsActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun ShootingStarsScreenEmptyPreview() {
    TwoverseTheme {
        ShootingStarsScreen(uiState = ShootingStarsUiState(isLoading = false), actions = ShootingStarsActions())
    }
}
