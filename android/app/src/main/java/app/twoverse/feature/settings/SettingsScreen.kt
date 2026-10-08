package app.twoverse.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.common.defaultShortName
import app.twoverse.core.common.formatDate
import app.twoverse.core.common.toPartnerName
import app.twoverse.BuildConfig
import app.twoverse.R
import app.twoverse.core.designsystem.component.Planet
import app.twoverse.core.designsystem.component.PlanetKind
import app.twoverse.core.designsystem.component.SettingsSwitchRow
import app.twoverse.core.designsystem.component.SettingsValueRow
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoverseConfirmDialog
import app.twoverse.core.designsystem.component.TwoversePreviewBackground
import app.twoverse.core.designsystem.component.TwoverseTextFieldDialog
import app.twoverse.core.designsystem.text.PronounStrings
import app.twoverse.core.designsystem.text.labelRes
import app.twoverse.core.designsystem.text.resFor
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.Pronouns
import app.twoverse.core.model.UserProfile
import app.twoverse.core.model.UserSettings
import java.time.LocalDate

private val CouplePadding = 18.dp
private val PartnerPlanetSize = 44.dp
private val YouPlanetSize = 34.dp
private val YouPlanetOffsetX = 30.dp
private val YouPlanetOffsetY = 5.dp
private val YouPlanetRing = 3.dp
private val PlanetsWidth = 66.dp
private val ActionRowMinHeight = 54.dp
private val SectionHeaderInset = 4.dp
private val CreditLineGap = 6.dp
private const val DatePattern = "dMMMy"

private val StarsSendSubtitle = PronounStrings(
    she = R.string.settings_stars_send_subtitle_she,
    he = R.string.settings_stars_send_subtitle_he,
    they = R.string.settings_stars_send_subtitle_they,
)

/** You & {partner} (FR-SET-1 to FR-SET-6, FR-PRO-4, FR-PRO-5). */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    versionName: String = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    val settings = uiState.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenHorizontal)
            .padding(bottom = spacing.md),
    ) {
        Text(
            text = uiState.partnerDisplayName?.let { stringResource(R.string.settings_title, it) }
                ?: stringResource(R.string.settings_title_unpaired),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        if (settings == null) return@Column
        SettingsGroup(modifier = Modifier.padding(top = spacing.md)) {
            SettingsValueRow(
                title = stringResource(R.string.settings_your_profile),
                value = "",
                onClick = actions.onOpenProfile,
            )
        }
        CoupleCard(
            isConnected = uiState.isConnected,
            connectedSince = uiState.connectedSince,
            modifier = Modifier.padding(top = spacing.md),
        )
        uiState.partner?.let { partner ->
            SectionHeader(R.string.settings_partner_details)
            PartnerDetails(partner = partner, actions = actions)
        }
        SectionHeader(R.string.settings_privacy)
        SettingsGroup {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_share_location),
                subtitle = stringResource(R.string.settings_share_location_subtitle),
                checked = settings.shareLocation,
                onCheckedChange = { share ->
                    if (share) actions.onOpenLocationSetup() else actions.onShareLocationChange(false)
                },
            )
            GroupDivider()
            SettingsValueRow(
                title = stringResource(R.string.settings_location_precision),
                value = stringResource(settings.locationPrecision.labelRes()),
                onClick = { actions.onOpenDialog(SettingsDialog.LocationPrecision) },
            )
            GroupDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_lock_ours),
                subtitle = stringResource(R.string.settings_lock_ours_subtitle),
                checked = settings.lockOurs,
                onCheckedChange = actions.onLockOursChange,
            )
        }
        if (uiState.isConnected) {
            SectionHeader(R.string.settings_stars)
            SettingsGroup {
                SettingsValueRow(
                    title = stringResource(R.string.settings_stars_send),
                    subtitle = stringResource(StarsSendSubtitle.resFor(uiState.partner?.pronouns)),
                    value = "",
                    onClick = actions.onSendShootingStar,
                )
                GroupDivider()
                SettingsValueRow(
                    title = stringResource(R.string.settings_stars_list),
                    subtitle = stringResource(R.string.settings_stars_list_subtitle),
                    value = "",
                    onClick = actions.onOpenShootingStars,
                )
            }
        }
        SectionHeader(R.string.settings_preferences)
        SettingsGroup {
            SettingsValueRow(
                title = stringResource(R.string.settings_distance_unit),
                value = stringResource(settings.distanceUnit.labelRes()),
                onClick = { actions.onOpenDialog(SettingsDialog.DistanceUnit) },
            )
            GroupDivider()
            SettingsValueRow(
                title = stringResource(R.string.settings_appearance),
                value = stringResource(settings.appearance.labelRes()),
                onClick = { actions.onOpenDialog(SettingsDialog.Appearance) },
            )
        }
        SettingsGroup(modifier = Modifier.padding(top = spacing.md)) {
            ActionRow(R.string.settings_log_out, colors.onSurface) { actions.onOpenDialog(SettingsDialog.LogOut) }
            if (uiState.isConnected) {
                GroupDivider()
                ActionRow(R.string.settings_disconnect, colors.error) { actions.onOpenDialog(SettingsDialog.Disconnect) }
            }
            GroupDivider()
            ActionRow(R.string.settings_delete_account, colors.error) {
                actions.onOpenDialog(SettingsDialog.DeleteAccount)
            }
        }
        uiState.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier
                    .padding(horizontal = SectionHeaderInset, vertical = spacing.xs)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        AboutSection(versionName = versionName, versionCode = versionCode)
    }
    if (settings != null) {
        SettingsDialogs(
            openDialog = uiState.openDialog,
            settings = settings,
            nicknameDraft = uiState.nicknameDraft,
            hasNickname = uiState.partner?.nickname != null,
            actions = actions,
        )
    }
}

/** The app version and a short credit, at the end of the screen. */
@Composable
private fun AboutSection(versionName: String, versionCode: Int) {
    val colors = TwoverseTheme.colors
    val versionDescription = stringResource(R.string.settings_version_description, versionName)
    SectionHeader(R.string.settings_about)
    SettingsGroup {
        Column(modifier = Modifier.clearAndSetSemantics { contentDescription = versionDescription }) {
            DetailRow(
                label = stringResource(R.string.settings_version),
                value = stringResource(R.string.settings_version_value, versionName, versionCode),
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwoverseTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CreditLineGap),
    ) {
        Text(
            text = stringResource(R.string.settings_credit, stringResource(R.string.developer_name)),
            style = TwoverseTheme.textStyles.credit,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )
        val heartDescription = stringResource(R.string.settings_credit_heart_description)
        Row(horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs)) {
            Text(
                text = stringResource(R.string.settings_credit_tagline),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = stringResource(R.string.settings_credit_heart),
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                modifier = Modifier
                    .alignByBaseline()
                    .semantics { contentDescription = heartDescription },
            )
        }
    }
}

@Composable
private fun CoupleCard(isConnected: Boolean, connectedSince: LocalDate?, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    TwoverseCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(CouplePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.smd),
        ) {
            Box(modifier = Modifier.size(width = PlanetsWidth, height = PartnerPlanetSize)) {
                Planet(kind = PlanetKind.Her, size = PartnerPlanetSize)
                Planet(
                    kind = PlanetKind.You,
                    size = YouPlanetSize,
                    modifier = Modifier
                        .offset(x = YouPlanetOffsetX, y = YouPlanetOffsetY)
                        .border(YouPlanetRing, colors.surface, TwoverseTheme.shapes.circle),
                )
            }
            Column {
                Text(
                    text = stringResource(if (isConnected) R.string.settings_connected else R.string.settings_not_connected),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                if (isConnected && connectedSince != null) {
                    val locale = LocalConfiguration.current.locales[0]
                    val date = formatDate(connectedSince, DatePattern, locale)
                    Text(
                        text = stringResource(R.string.settings_connected_since, date),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The partner's names, pronouns, shared contact details and my private nickname (FR-PRO-5). */
@Composable
private fun PartnerDetails(partner: UserProfile, actions: SettingsActions) {
    val notShared = stringResource(R.string.settings_partner_not_shared)
    val name = partner.toPartnerName().name
    SettingsGroup {
        DetailRow(
            label = stringResource(R.string.settings_partner_short_name),
            value = partner.shortName ?: defaultShortName(partner.fullName),
        )
        GroupDivider()
        DetailRow(label = stringResource(R.string.settings_partner_full_name), value = partner.fullName)
        GroupDivider()
        DetailRow(
            label = stringResource(R.string.settings_partner_pronouns),
            value = stringResource(partner.pronouns?.labelRes() ?: R.string.settings_partner_pronouns_unknown),
        )
        GroupDivider()
        DetailRow(
            label = stringResource(R.string.settings_partner_email),
            value = partner.email ?: notShared,
            onClickLabel = stringResource(R.string.settings_partner_email_action, name),
            onClick = partner.email?.let { email -> { actions.onEmailPartner(email) } },
        )
        GroupDivider()
        DetailRow(
            label = stringResource(R.string.settings_partner_phone),
            value = partner.phone ?: notShared,
            onClickLabel = stringResource(R.string.settings_partner_phone_action, name),
            onClick = partner.phone?.let { phone -> { actions.onCallPartner(phone) } },
        )
        GroupDivider()
        SettingsValueRow(
            title = stringResource(R.string.settings_nickname),
            subtitle = stringResource(R.string.settings_nickname_note),
            value = partner.nickname ?: stringResource(R.string.settings_nickname_none),
            onClick = { actions.onOpenDialog(SettingsDialog.Nickname) },
        )
    }
}

/** A read-only label and value; tappable (in the primary colour) when [onClick] is set. */
@Composable
private fun DetailRow(label: String, value: String, onClickLabel: String? = null, onClick: (() -> Unit)? = null) {
    val colors = TwoverseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ActionRowMinHeight)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = TwoverseTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) colors.primary else colors.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionHeader(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes).uppercase(LocalConfiguration.current.locales[0]),
        style = TwoverseTheme.textStyles.sectionHeader,
        color = TwoverseTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .padding(start = SectionHeaderInset, end = SectionHeaderInset, top = TwoverseTheme.spacing.lg)
            .padding(bottom = TwoverseTheme.spacing.xs)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    TwoverseCard(shape = TwoverseTheme.shapes.group, modifier = modifier.fillMaxWidth()) {
        content()
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(color = TwoverseTheme.colors.outline)
}

@Composable
private fun ActionRow(@StringRes textRes: Int, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ActionRowMinHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TwoverseTheme.spacing.md),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = stringResource(textRes), style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Composable
private fun SettingsDialogs(
    openDialog: SettingsDialog?,
    settings: UserSettings,
    nicknameDraft: String,
    hasNickname: Boolean,
    actions: SettingsActions,
) {
    when (openDialog) {
        null -> Unit
        SettingsDialog.LocationPrecision -> ChoiceDialog(
            titleRes = R.string.settings_location_precision,
            options = LocationPrecision.entries,
            selected = settings.locationPrecision,
            label = { it.labelRes() },
            onSelect = actions.onLocationPrecisionSelected,
            onDismiss = actions.onDismissDialog,
        )
        SettingsDialog.DistanceUnit -> ChoiceDialog(
            titleRes = R.string.settings_distance_unit,
            options = DistanceUnit.entries,
            selected = settings.distanceUnit,
            label = { it.labelRes() },
            onSelect = actions.onDistanceUnitSelected,
            onDismiss = actions.onDismissDialog,
        )
        SettingsDialog.Appearance -> ChoiceDialog(
            titleRes = R.string.settings_appearance,
            options = AppearanceMode.entries,
            selected = settings.appearance,
            label = { it.labelRes() },
            onSelect = actions.onAppearanceSelected,
            onDismiss = actions.onDismissDialog,
        )
        SettingsDialog.Nickname -> TwoverseTextFieldDialog(
            title = stringResource(R.string.settings_nickname_dialog_title),
            value = nicknameDraft,
            onValueChange = actions.onNicknameChange,
            label = stringResource(R.string.settings_nickname_field),
            confirmLabel = stringResource(R.string.settings_nickname_save),
            onConfirm = actions.onSaveNickname,
            onDismiss = actions.onDismissDialog,
            note = stringResource(R.string.settings_nickname_note),
            extraActionLabel = stringResource(R.string.settings_nickname_clear).takeIf { hasNickname },
            onExtraAction = actions.onClearNickname,
        )
        SettingsDialog.LogOut -> TwoverseConfirmDialog(
            title = stringResource(R.string.settings_log_out_title),
            message = stringResource(R.string.settings_log_out_message),
            confirmLabel = stringResource(R.string.settings_log_out),
            onConfirm = actions.onLogOutConfirmed,
            onDismiss = actions.onDismissDialog,
        )
        SettingsDialog.Disconnect -> TwoverseConfirmDialog(
            title = stringResource(R.string.settings_disconnect_title),
            message = stringResource(R.string.settings_disconnect_message),
            confirmLabel = stringResource(R.string.settings_disconnect_confirm),
            onConfirm = actions.onDisconnectConfirmed,
            onDismiss = actions.onDismissDialog,
            destructive = true,
        )
        SettingsDialog.DeleteAccount -> TwoverseConfirmDialog(
            title = stringResource(R.string.settings_delete_account_title),
            message = stringResource(R.string.settings_delete_account_message),
            confirmLabel = stringResource(R.string.settings_delete_account),
            onConfirm = actions.onDeleteAccountConfirmed,
            onDismiss = actions.onDismissDialog,
            destructive = true,
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    @StringRes titleRes: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    OptionDialog(
        title = stringResource(titleRes),
        options = options.map { stringResource(label(it)) },
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
        onDismiss = onDismiss,
    )
}

private fun LocationPrecision.labelRes(): Int = when (this) {
    LocationPrecision.Approximate -> R.string.settings_precision_approximate
    LocationPrecision.Precise -> R.string.settings_precision_precise
}

private fun DistanceUnit.labelRes(): Int = when (this) {
    DistanceUnit.Kilometres -> R.string.settings_unit_kilometres
    DistanceUnit.Miles -> R.string.settings_unit_miles
}

private fun AppearanceMode.labelRes(): Int = when (this) {
    AppearanceMode.System -> R.string.settings_appearance_system
    AppearanceMode.Light -> R.string.settings_appearance_light
    AppearanceMode.Dark -> R.string.settings_appearance_dark
}

private val PreviewSettings = UserSettings(
    shareLocation = true,
    locationPrecision = LocationPrecision.Approximate,
    lockOurs = true,
    distanceUnit = DistanceUnit.Kilometres,
    appearance = AppearanceMode.System,
)

private val PreviewPartner = UserProfile(
    id = "user-partner",
    fullName = "Ammu Perera",
    shortName = "Ammu",
    pronouns = Pronouns.She,
    email = "ammu@example.com",
    phone = "+94771234567",
)

@Composable
private fun SettingsPreview(partner: UserProfile?) {
    TwoverseTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                settings = PreviewSettings,
                isConnected = partner != null,
                connectedSince = LocalDate.of(2026, 9, 28).takeIf { partner != null },
                partner = partner,
            ),
            actions = SettingsActions(),
            versionName = "1.0.0",
            versionCode = 1,
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsSheSharedPreview() {
    SettingsPreview(partner = PreviewPartner)
}

@PreviewLightDark
@Composable
private fun SettingsHeNicknamePreview() {
    SettingsPreview(
        partner = PreviewPartner.copy(
            fullName = "Keeththi Lan",
            shortName = "Keeththi",
            pronouns = Pronouns.He,
            nickname = "Kanna",
            email = null,
            phone = null,
        ),
    )
}

@PreviewLightDark
@Composable
private fun SettingsTheyLongNamesPreview() {
    SettingsPreview(
        partner = PreviewPartner.copy(
            fullName = "Alexandria Maximiliana Wickramasinghe-Ratnayake",
            shortName = "Alexandria-Maximiliana-Wickr",
            pronouns = Pronouns.They,
            phone = null,
        ),
    )
}

@PreviewLightDark
@Composable
private fun SettingsNoPronounsPreview() {
    SettingsPreview(partner = PreviewPartner.copy(pronouns = null, email = null))
}

@PreviewLightDark
@Composable
private fun SettingsUnpairedPreview() {
    SettingsPreview(partner = null)
}

@PreviewLightDark
@Composable
private fun SettingsNicknameDialogPreview() {
    TwoverseTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                settings = PreviewSettings,
                isConnected = true,
                partner = PreviewPartner.copy(nickname = "Chellam"),
                openDialog = SettingsDialog.Nickname,
                nicknameDraft = "Chellam",
            ),
            actions = SettingsActions(),
            versionName = "1.0.0",
            versionCode = 1,
        )
    }
}

@PreviewLightDark
@Composable
private fun AboutSectionPreview() {
    TwoversePreviewBackground {
        Column { AboutSection(versionName = "1.0.0", versionCode = 1) }
    }
}
