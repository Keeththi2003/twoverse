package app.twoverse.feature.pairing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.OrDivider
import app.twoverse.core.designsystem.component.Planet
import app.twoverse.core.designsystem.component.PlanetKind
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoverseOutlineButton
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseSecondaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError

private val ExpiryIconSize = 16.dp
private val WaitingHerSize = 14.dp
private val WaitingYouSize = 11.dp
private val WaitingLineWidth = 36.dp
private val WaitingLineStroke = 2.dp
private val WaitingDash = 4.dp

/** Connect your worlds (FR-PAIR-1 to FR-PAIR-5). */
@Composable
fun PairScreen(
    uiState: PairUiState,
    onBack: (() -> Unit)?,
    onShareCode: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    onRetryCode: () -> Unit,
    onPartnerCodeChange: (String) -> Unit,
    onConnect: () -> Unit,
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
            Column {
                if (onBack != null) {
                    TwoverseBackButton(onClick = onBack)
                    Spacer(modifier = Modifier.height(spacing.md))
                }
                Text(
                    text = stringResource(R.string.pair_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(R.string.pair_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(spacing.xl))
                CoupleCodeCard(
                    uiState = uiState,
                    onShareCode = onShareCode,
                    onCopyCode = onCopyCode,
                    onRetryCode = onRetryCode,
                )
                if (uiState.isWaitingForPartner) {
                    WaitingForPartner(modifier = Modifier.padding(top = spacing.lg))
                }
                OrDivider(modifier = Modifier.padding(top = spacing.xl, bottom = spacing.lg))
                TwoverseTextField(
                    value = uiState.partnerCode,
                    onValueChange = onPartnerCodeChange,
                    label = stringResource(R.string.pair_partner_code_label),
                    placeholder = stringResource(R.string.pair_partner_code_placeholder),
                    textStyle = TwoverseTheme.textStyles.coupleCodeInput,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConnect() }),
                    errorText = uiState.joinError?.let { stringResource(it.messageRes()) },
                )
            }
            TwoverseOutlineButton(
                text = stringResource(R.string.pair_connect),
                onClick = onConnect,
                enabled = uiState.canConnect,
                accentColor = colors.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xl),
            )
        }
    }
}

@Composable
private fun CoupleCodeCard(
    uiState: PairUiState,
    onShareCode: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    onRetryCode: () -> Unit,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    val code = uiState.coupleCode
    TwoverseCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.smd),
        ) {
            Text(
                text = stringResource(R.string.pair_your_code).uppercase(),
                style = TwoverseTheme.textStyles.sectionHeader,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = code ?: stringResource(R.string.pair_partner_code_placeholder),
                style = TwoverseTheme.textStyles.coupleCode,
                color = if (code != null) colors.onSurface else colors.outline,
            )
            uiState.codeError?.let { error ->
                Text(
                    text = stringResource(error.messageRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                    textAlign = TextAlign.Center,
                )
                TwoverseTextButton(text = stringResource(R.string.common_retry), onClick = onRetryCode)
                return@Column
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_clock),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(ExpiryIconSize),
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.pair_expires_in_hours,
                        uiState.codeExpiresInHours.toInt(),
                        uiState.codeExpiresInHours,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                TwoversePrimaryButton(
                    text = stringResource(R.string.pair_share),
                    onClick = { code?.let(onShareCode) },
                    enabled = code != null,
                    small = true,
                    modifier = Modifier.weight(1f),
                )
                TwoverseSecondaryButton(
                    text = stringResource(R.string.pair_copy),
                    onClick = { code?.let(onCopyCode) },
                    enabled = code != null,
                    leadingIcon = R.drawable.ic_copy,
                    small = true,
                )
            }
        }
    }
}

@Composable
private fun WaitingForPartner(modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs),
        ) {
            Planet(kind = PlanetKind.Her, size = WaitingHerSize)
            Canvas(modifier = Modifier.size(width = WaitingLineWidth, height = WaitingLineStroke)) {
                drawLine(
                    color = colors.outline,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(WaitingDash.toPx(), WaitingDash.toPx())),
                )
            }
            Planet(kind = PlanetKind.You, size = WaitingYouSize)
        }
        Text(
            text = stringResource(R.string.pair_waiting),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = colors.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun PairScreenPreview() {
    TwoverseTheme {
        PairScreen(
            uiState = PairUiState(coupleCode = "AB72-KP91", codeExpiresInHours = 24),
            onBack = {},
            onShareCode = {},
            onCopyCode = {},
            onRetryCode = {},
            onPartnerCodeChange = {},
            onConnect = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PairScreenInvalidCodePreview() {
    TwoverseTheme {
        PairScreen(
            uiState = PairUiState(
                coupleCode = "AB72-KP91",
                codeExpiresInHours = 23,
                partnerCode = "ZZ00-0000",
                joinError = DataError.InvalidCoupleCode,
            ),
            onBack = null,
            onShareCode = {},
            onCopyCode = {},
            onRetryCode = {},
            onPartnerCodeChange = {},
            onConnect = {},
        )
    }
}
