package app.twoverse.feature.pairing

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.Planet
import app.twoverse.core.designsystem.component.PlanetKind
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError
import app.twoverse.core.model.ReconnectRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val HerSize = 56.dp
private val YouSize = 40.dp
private const val DatePattern = "EEEEdMMMM"

/** Reconnect within 7 days of a disconnect; both partners must confirm (SRS 12). */
@Composable
fun ReconnectScreen(
    uiState: ReconnectUiState,
    onBack: () -> Unit,
    onReconnect: () -> Unit,
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
                .padding(bottom = spacing.sm),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                TwoverseBackButton(onClick = onBack)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Planet(kind = PlanetKind.Her, size = HerSize)
                    Planet(kind = PlanetKind.You, size = YouSize)
                }
                Text(
                    text = stringResource(R.string.reconnect_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                when (uiState) {
                    ReconnectUiState.Loading, ReconnectUiState.Reconnected -> Unit
                    ReconnectUiState.Unavailable -> BodyText(stringResource(R.string.error_reconnect_unavailable))
                    is ReconnectUiState.Available -> AvailableDetails(uiState)
                }
            }
            if (uiState is ReconnectUiState.Available && uiState.request != ReconnectRequest.ByMe) {
                TwoversePrimaryButton(
                    text = stringResource(
                        if (uiState.request == ReconnectRequest.ByPartner) R.string.reconnect_confirm else R.string.reconnect_ask,
                    ),
                    onClick = onReconnect,
                    enabled = uiState.canReconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xl),
                )
            }
        }
    }
}

@Composable
private fun AvailableDetails(uiState: ReconnectUiState.Available) {
    val colors = TwoverseTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val date = uiState.deleteOn.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, DatePattern), locale))
    BodyText(
        stringResource(
            if (uiState.request == ReconnectRequest.ByPartner) R.string.reconnect_partner_asked else R.string.reconnect_body,
            date,
        ),
    )
    if (uiState.request == ReconnectRequest.ByMe) {
        TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.reconnect_waiting),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                modifier = Modifier
                    .padding(TwoverseTheme.spacing.md)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    BodyText(stringResource(R.string.reconnect_note))
    uiState.error?.let { error ->
        Text(
            text = stringResource(error.messageRes()),
            style = MaterialTheme.typography.bodySmall,
            color = colors.error,
            textAlign = TextAlign.Start,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TwoverseTheme.colors.onSurfaceVariant)
}

@PreviewLightDark
@Composable
private fun ReconnectScreenPreview() {
    TwoverseTheme {
        ReconnectScreen(
            uiState = ReconnectUiState.Available(deleteOn = LocalDate.of(2026, 10, 4), request = ReconnectRequest.None),
            onBack = {},
            onReconnect = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ReconnectScreenWaitingPreview() {
    TwoverseTheme {
        ReconnectScreen(
            uiState = ReconnectUiState.Available(
                deleteOn = LocalDate.of(2026, 10, 4),
                request = ReconnectRequest.ByMe,
                error = DataError.Network,
            ),
            onBack = {},
            onReconnect = {},
        )
    }
}
