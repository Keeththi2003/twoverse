package app.twoverse.feature.settings

import android.text.format.DateFormat
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.Planet
import app.twoverse.core.designsystem.component.PlanetKind
import app.twoverse.core.designsystem.component.SettingsSwitchRow
import app.twoverse.core.designsystem.component.SettingsValueRow
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoverseConfirmDialog
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.UserSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CouplePadding = 18.dp
private val HerPlanetSize = 44.dp
private val YouPlanetSize = 34.dp
private val YouPlanetOffsetX = 30.dp
private val YouPlanetOffsetY = 5.dp
private val YouPlanetRing = 3.dp
private val PlanetsWidth = 66.dp
private val ActionRowMinHeight = 54.dp
private val SectionHeaderInset = 4.dp
private const val DatePattern = "dMMMy"

/** You & Her (FR-SET-1 to FR-SET-6). */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    val settings = uiState.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = spacing.screenHorizontal)
            .padding(bottom = spacing.md),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (settings == null) return@Column
        CoupleCard(
            isConnected = uiState.isConnected,
            connectedSince = uiState.connectedSince,
            modifier = Modifier.padding(top = spacing.md),
        )
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
            SectionHeader(R.string.settings_birthday)
            SettingsGroup {
                SettingsValueRow(
                    title = stringResource(R.string.settings_birthday_message),
                    subtitle = stringResource(R.string.settings_birthday_message_subtitle),
                    value = "",
                    onClick = actions.onEditBirthdayMessage,
                )
                if (uiState.hasBirthdayWelcome) {
                    GroupDivider()
                    SettingsValueRow(
                        title = stringResource(R.string.settings_birthday_view),
                        value = "",
                        onClick = actions.onShowBirthday,
                    )
                }
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
    }
    if (settings != null) {
        SettingsDialogs(openDialog = uiState.openDialog, settings = settings, actions = actions)
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
            Box(modifier = Modifier.size(width = PlanetsWidth, height = HerPlanetSize)) {
                Planet(kind = PlanetKind.Her, size = HerPlanetSize)
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
                    val date = connectedSince.format(
                        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, DatePattern), locale),
                    )
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
private fun SettingsDialogs(openDialog: SettingsDialog?, settings: UserSettings, actions: SettingsActions) {
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

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    TwoverseTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                settings = UserSettings(
                    shareLocation = true,
                    locationPrecision = LocationPrecision.Approximate,
                    lockOurs = true,
                    distanceUnit = DistanceUnit.Kilometres,
                    appearance = AppearanceMode.System,
                ),
                isConnected = true,
                connectedSince = LocalDate.of(2026, 2, 14),
                hasBirthdayWelcome = true,
            ),
            actions = SettingsActions(),
        )
    }
}
