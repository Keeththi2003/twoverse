package app.twoverse.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.component.SettingsSwitchRow
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.component.TwoverseTextFieldDialog
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError
import app.twoverse.core.model.Pronouns

/** Everything the profile screen can ask for. */
data class ProfileActions(
    val onBack: () -> Unit = {},
    val onFullNameChange: (String) -> Unit = {},
    val onShortNameChange: (String) -> Unit = {},
    val onPronounsSelected: (Pronouns) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onShareEmailChange: (Boolean) -> Unit = {},
    val onSharePhoneChange: (Boolean) -> Unit = {},
    val onSave: () -> Unit = {},
    val onChangeEmail: () -> Unit = {},
    val onNewEmailChange: (String) -> Unit = {},
    val onConfirmEmail: () -> Unit = {},
    val onDismissEmailDialog: () -> Unit = {},
)

/** Your profile: names, pronouns, phone, email and what the partner may see (FR-PRO-4). */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
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
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (uiState.isLoading) return@Column
        TwoverseTextField(
            value = uiState.fullName,
            onValueChange = actions.onFullNameChange,
            label = stringResource(R.string.profile_full_name),
            errorText = stringResource(R.string.profile_problem_full_name)
                .takeIf { uiState.problem == ProfileProblem.FullNameMissing },
        )
        TwoverseTextField(
            value = uiState.shortName,
            onValueChange = actions.onShortNameChange,
            label = stringResource(R.string.profile_short_name),
            errorText = stringResource(R.string.profile_problem_short_name)
                .takeIf { uiState.problem == ProfileProblem.ShortNameMissing },
        )
        Text(
            text = stringResource(R.string.profile_pronouns),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
        PronounsPicker(selected = uiState.pronouns, onSelect = actions.onPronounsSelected)
        TwoverseTextField(
            value = uiState.phone,
            onValueChange = actions.onPhoneChange,
            label = stringResource(R.string.profile_phone),
            placeholder = stringResource(R.string.profile_phone_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            errorText = stringResource(R.string.profile_phone_invalid)
                .takeIf { uiState.problem == ProfileProblem.PhoneInvalid },
        )
        EmailSection(uiState = uiState, onChangeEmail = actions.onChangeEmail)
        Text(
            text = stringResource(R.string.profile_sharing),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        TwoverseCard(shape = TwoverseTheme.shapes.group, modifier = Modifier.fillMaxWidth()) {
            SettingsSwitchRow(
                title = stringResource(R.string.profile_share_email),
                checked = uiState.shareEmail,
                onCheckedChange = actions.onShareEmailChange,
            )
            HorizontalDivider(color = colors.outline)
            SettingsSwitchRow(
                title = stringResource(R.string.profile_share_phone),
                subtitle = stringResource(R.string.profile_share_phone_needs_number).takeIf { uiState.phone.isBlank() },
                checked = uiState.sharePhone,
                onCheckedChange = actions.onSharePhoneChange,
            )
        }
        val status = when {
            uiState.error != null -> stringResource(uiState.error.messageRes())
            uiState.isSaved -> stringResource(R.string.profile_saved)
            else -> null
        }
        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.error != null) colors.error else colors.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        TwoversePrimaryButton(
            text = stringResource(R.string.profile_save),
            onClick = actions.onSave,
            enabled = uiState.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (uiState.isEmailDialogOpen) {
        TwoverseTextFieldDialog(
            title = stringResource(R.string.profile_email_change),
            value = uiState.newEmail,
            onValueChange = actions.onNewEmailChange,
            label = stringResource(R.string.profile_email_new),
            confirmLabel = stringResource(R.string.profile_email_change),
            onConfirm = actions.onConfirmEmail,
            onDismiss = actions.onDismissEmailDialog,
            errorText = stringResource(R.string.profile_email_invalid).takeIf { uiState.isNewEmailInvalid },
            keyboardType = KeyboardType.Email,
        )
    }
}

/** Email: read-only for Google accounts; otherwise changed through a confirmation email. */
@Composable
private fun EmailSection(uiState: ProfileUiState, onChangeEmail: () -> Unit) {
    val colors = TwoverseTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.profile_email),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
        Text(
            text = uiState.email.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val note = when {
            !uiState.canChangeEmail -> stringResource(R.string.profile_email_google)
            uiState.pendingEmail != null ->
                stringResource(R.string.profile_email_pending, uiState.pendingEmail, uiState.email.orEmpty())
            uiState.isEmailChangeSent -> stringResource(R.string.profile_email_sent)
            else -> null
        }
        note?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (uiState.canChangeEmail) {
            TwoverseTextButton(text = stringResource(R.string.profile_email_change), onClick = onChangeEmail)
        }
    }
}

private val PreviewProfile = ProfileUiState(
    isLoading = false,
    fullName = "Keeththi Lan",
    shortName = "Keeththi",
    pronouns = Pronouns.He,
    phone = "+94771234567",
    shareEmail = true,
    sharePhone = false,
    email = "keeththi@example.com",
    canChangeEmail = true,
)

@PreviewLightDark
@Composable
private fun ProfileEmailAccountPreview() {
    TwoverseTheme { ProfileScreen(uiState = PreviewProfile, actions = ProfileActions()) }
}

@PreviewLightDark
@Composable
private fun ProfileGooglePendingPreview() {
    TwoverseTheme {
        ProfileScreen(
            uiState = PreviewProfile.copy(
                fullName = "Ammu Perera",
                shortName = "Ammu",
                pronouns = Pronouns.She,
                phone = "",
                shareEmail = false,
                canChangeEmail = false,
                isSaved = true,
            ),
            actions = ProfileActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfileProblemsPreview() {
    TwoverseTheme {
        ProfileScreen(
            uiState = PreviewProfile.copy(
                fullName = "Alexandria Maximiliana Wickramasinghe-Ratnayake",
                pronouns = Pronouns.They,
                phone = "0771",
                pendingEmail = "alex.new@example.com",
                problem = ProfileProblem.PhoneInvalid,
                error = DataError.Network,
            ),
            actions = ProfileActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfileEmailDialogPreview() {
    TwoverseTheme {
        ProfileScreen(
            uiState = PreviewProfile.copy(isEmailDialogOpen = true, newEmail = "keeththi@", isNewEmailInvalid = true),
            actions = ProfileActions(),
        )
    }
}
