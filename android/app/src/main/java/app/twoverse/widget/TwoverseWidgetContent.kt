package app.twoverse.widget

import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.twoverse.R
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.common.PartnerName
import java.time.ZoneId

/**
 * Glance picks the largest of these that fits the widget (FR-WGT-1). Medium needs room for the
 * distance, the reunion line and the Together card; a 4×2 shorter than that gets the small layout.
 */
internal val SmallWidget = DpSize(110.dp, 110.dp)
internal val MediumWidget = DpSize(250.dp, 150.dp)
internal val LargeWidget = DpSize(250.dp, 230.dp)

/** From this height the large widget adds her direction, both local times and the extras line. */
internal val TallWidget = DpSize(250.dp, 300.dp)
internal val WidgetSizes = setOf(SmallWidget, MediumWidget, LargeWidget, TallWidget)

private val WidgetPadding = 16.dp
private val CompactPadding = 12.dp
private val Gap = 8.dp
private val SmallGap = 4.dp
private val TinyGap = 2.dp
private val DotSize = 8.dp
private val DotGap = 6.dp
private val IconSize = 14.dp
private val ProgressPlanetSize = 12.dp
private val ProgressHeight = 4.dp
private val CardPadding = 12.dp
private val CompactCardPadding = 10.dp
private val MediumOrbitWidth = 96.dp
private val MediumOrbitHeight = 58.dp
private const val FadedAlpha = 0.35f

/** Where each part of the widget leads (FR-WGT-4); null in previews. */
internal data class WidgetActions(
    val openHome: Action? = null,
    val openCountdown: Action? = null,
    val openVault: Action? = null,
    val openShootingStar: Action? = null,
    val openOrbit: Action? = null,
)

@Composable
internal fun TwoverseWidgetContent(state: WidgetState?, actions: WidgetActions) {
    WidgetTheme(mode = widgetThemeMode(state?.appearance ?: AppearanceMode.System)) {
        WidgetBody(state = state, size = LocalSize.current, actions = actions)
    }
}

/** Chooses the layout for [size]; while the saved data loads, only the faded orbit shows. */
@Composable
internal fun WidgetBody(state: WidgetState?, size: DpSize, actions: WidgetActions) {
    val isMedium = size.width >= MediumWidget.width && size.height >= MediumWidget.height
    val isLarge = size.width >= LargeWidget.width && size.height >= LargeWidget.height
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground()
            .padding(if (isMedium && !isLarge) CompactPadding else WidgetPadding)
            .onTap(actions.openHome),
    ) {
        when {
            state == null -> Orbit(widgetPalette.drawables.orbitSmall, faded = true, modifier = GlanceModifier.fillMaxSize())
            isLarge -> LargeLayout(state, isTall = size.height >= TallWidget.height, actions = actions)
            isMedium -> MediumLayout(state, actions)
            else -> SmallLayout(state)
        }
    }
}

/** 2×2: planets and arc, the distance and how fresh it is, and the days together. */
@Composable
private fun ColumnScope.SmallLayout(state: WidgetState) {
    val context = LocalContext.current
    Orbit(
        drawable = widgetPalette.drawables.orbitSmall,
        faded = state.location != WidgetLocation.Available,
        partner = state.partner,
        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
    )
    Spacer(modifier = GlanceModifier.height(Gap))
    val orbit = state.orbit
    when {
        state.distance != null -> {
            Distance(value = state.distance, unit = context.unitShort(state.distanceUnit), size = WidgetText.DistanceSmall)
            Freshness(state, short = true)
            orbit?.let { Text(text = context.daysTogetherText(it.totalDays), style = WidgetText.muted(WidgetText.Small), maxLines = 1) }
        }
        // Without a distance, the days together take its place, with the reason in small print (FR-WGT-8).
        orbit != null -> {
            Distance(value = orbit.totalDays.toString(), unit = context.daysTogetherLabel(orbit.totalDays), size = WidgetText.DistanceSmall)
            context.locationMessage(state.location)?.let { Text(text = it, style = WidgetText.muted(WidgetText.Small), maxLines = 1) }
        }
        else -> StatusMessage(state)
    }
}

/** 4×2: distance and freshness beside the planets, the reunion line, and the Together card. */
@Composable
private fun ColumnScope.MediumLayout(state: WidgetState, actions: WidgetActions) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            if (state.distance != null) {
                Distance(value = state.distance, unit = context.unitApart(state.distanceUnit), size = WidgetText.DistanceSmall)
                Freshness(state, short = false)
            } else {
                StatusMessage(state)
            }
        }
        Orbit(
            drawable = widgetPalette.drawables.orbitMedium,
            faded = state.location != WidgetLocation.Available,
            partner = state.partner,
            modifier = GlanceModifier.size(MediumOrbitWidth, MediumOrbitHeight),
        )
    }
    BottomCards(state, compact = true, actions = actions)
}

/**
 * 4×3 and bigger: the orbit with the distance inside, details as space allows, the reunion line
 * and the Together card. Without a distance the orbit fades and the reason sits below it, never on it.
 */
@Composable
private fun ColumnScope.LargeLayout(state: WidgetState, isTall: Boolean, actions: WidgetActions) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = context.getString(R.string.widget_distance_label),
            style = WidgetText.muted(),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Freshness(state, short = false)
    }
    Box(
        modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(vertical = SmallGap),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Orbit(
            drawable = widgetPalette.drawables.orbitLarge,
            faded = state.location != WidgetLocation.Available,
            partner = state.partner,
            modifier = GlanceModifier.fillMaxSize(),
        )
        if (state.distance != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = state.distance, style = WidgetText.serif(WidgetText.DistanceLarge), maxLines = 1)
                Text(text = context.unitApart(state.distanceUnit), style = WidgetText.muted())
            }
        }
    }
    if (state.distance == null) {
        Column(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            StatusMessage(state, centered = true)
        }
    }
    if (isTall) InfoRow(state)
    BottomCards(state, compact = false, actions = actions)
    if (isTall && state.isPaired) Extras(state, actions)
}

/**
 * The reunion line (only above the Together card, only with a date) and the big card: Together
 * when "together since" is set, else Until we meet (FR-WGT-8).
 */
@Composable
private fun BottomCards(state: WidgetState, compact: Boolean, actions: WidgetActions) {
    state.reunionLine?.let {
        Spacer(modifier = GlanceModifier.height(if (compact) SmallGap else Gap))
        ReunionLine(it, onTap = actions.openCountdown)
    }
    val orbit = state.orbit
    when (state.card) {
        WidgetCard.Together -> if (orbit != null) {
            Spacer(modifier = GlanceModifier.height(SmallGap))
            TogetherCard(orbit, compact = compact, onTap = actions.openOrbit)
        }
        WidgetCard.UntilWeMeet -> {
            Spacer(modifier = GlanceModifier.height(if (compact) SmallGap else Gap))
            ReunionCard(state.reunion, compact = compact, onTap = actions.openCountdown)
        }
        WidgetCard.None -> Unit
    }
}

@Composable
private fun Orbit(@DrawableRes drawable: Int, faded: Boolean, modifier: GlanceModifier, partner: PartnerName? = null) {
    val context = LocalContext.current
    Image(
        provider = ImageProvider(drawable),
        contentDescription = context.getString(R.string.widget_orbit_description, context.partnerLabel(partner)),
        alpha = if (faded) FadedAlpha else 1f,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun Distance(value: String, unit: String, size: TextUnit) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text = value, style = WidgetText.serif(size), maxLines = 1)
        Text(
            text = unit,
            style = WidgetText.muted(),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = SmallGap, bottom = SmallGap),
        )
    }
}

/** Live in gold; older positions say how old they are, so saved data never looks live (BR-8). */
@Composable
private fun Freshness(state: WidgetState, short: Boolean) {
    val context = LocalContext.current
    val freshness = context.freshnessText(state.freshness, state.updatedAgo, short)
    val offline = if (state.isOffline) {
        context.getString(if (short || freshness != null) R.string.widget_offline_short else R.string.widget_offline)
    } else {
        null
    }
    val text = listOfNotNull(offline, freshness).joinToString(Separator)
    if (text.isEmpty()) return
    val isLive = state.freshness == LocationFreshness.Live && !state.isOffline
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (freshness != null) {
            Image(
                provider = ImageProvider(if (isLive) widgetPalette.drawables.dotLive else widgetPalette.drawables.dotMuted),
                contentDescription = null,
                modifier = GlanceModifier.size(DotSize),
            )
            Spacer(modifier = GlanceModifier.width(DotGap))
        }
        Text(
            text = text,
            style = WidgetText.sans(
                WidgetText.Small,
                color = if (isLive) widgetPalette.goldText else GlanceTheme.colors.onSurfaceVariant,
                weight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

private const val Separator = " · "

@Composable
private fun StatusMessage(state: WidgetState, centered: Boolean = false) {
    val context = LocalContext.current
    val message = context.locationMessage(state.location) ?: return
    Text(
        text = message,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = WidgetText.Status,
            fontWeight = FontWeight.Medium,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        ),
        maxLines = 2,
    )
    if (state.isOffline && state.isPaired) {
        Text(text = context.getString(R.string.widget_offline), style = WidgetText.muted(WidgetText.Small), maxLines = 1)
    }
}

/** "6 days until we meet · Fri, 16 Oct" with a calendar icon, above the Together card; opens Until We Meet. */
@Composable
private fun ReunionLine(reunion: WidgetReunion, onTap: Action?) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxWidth().onTap(onTap), verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(widgetPalette.drawables.calendar),
            contentDescription = null,
            modifier = GlanceModifier.size(IconSize),
        )
        Spacer(modifier = GlanceModifier.width(DotGap))
        Text(text = context.reunionLineText(reunion), style = WidgetText.sans(WidgetText.Body), maxLines = 1)
    }
}

/**
 * Until we meet as the big card, when "together since" isn't set: days until the reunion, its date
 * and the planets moving closer, yours left and the partner's right.
 */
@Composable
private fun ReunionCard(reunion: WidgetReunion?, compact: Boolean, onTap: Action?) {
    val context = LocalContext.current
    val drawables = widgetPalette.drawables
    Column(modifier = cardModifier(compact, onTap)) {
        if (reunion == null) {
            Text(text = context.getString(R.string.widget_no_date), style = WidgetText.sans(WidgetText.Body), maxLines = 1)
            Text(text = context.getString(R.string.widget_set_date), style = WidgetText.muted(WidgetText.Small), maxLines = 1)
            return@Column
        }
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            if (reunion.isToday) {
                Text(
                    text = context.getString(R.string.widget_reunion_today),
                    style = WidgetText.serif(WidgetText.Status),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            } else {
                Text(text = reunion.daysUntil.toString(), style = WidgetText.serif(cardNumberSize(compact)), maxLines = 1)
                Text(
                    text = context.daysLabel(reunion.daysUntil),
                    style = WidgetText.sans(WidgetText.Body),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight().padding(start = SmallGap, bottom = SmallGap),
                )
            }
            Text(
                text = context.reunionDate(reunion.date),
                style = WidgetText.muted(WidgetText.Small),
                maxLines = 1,
                modifier = GlanceModifier.padding(bottom = SmallGap),
            )
        }
        Spacer(modifier = GlanceModifier.height(SmallGap))
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(drawables.planetYou), null, GlanceModifier.size(ProgressPlanetSize))
            LinearProgressIndicator(
                progress = reunion.progress,
                modifier = GlanceModifier.defaultWeight().height(ProgressHeight).padding(horizontal = SmallGap),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.outline,
            )
            Image(ImageProvider(drawables.planetHer), null, GlanceModifier.size(ProgressPlanetSize))
        }
    }
}

/**
 * Together: the days together, since when, and a thin bar toward the next day milestone
 * ("25 days to 500"); the large widget adds "1y 3m 17d · Met 2 times". Opens Our Orbit (FR-WGT-8).
 */
@Composable
private fun TogetherCard(orbit: WidgetOrbit, compact: Boolean, onTap: Action?) {
    val context = LocalContext.current
    Column(modifier = cardModifier(compact, onTap)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(widgetPalette.drawables.heart),
                contentDescription = null,
                modifier = GlanceModifier.size(IconSize),
            )
            Spacer(modifier = GlanceModifier.width(DotGap))
            Text(text = orbit.totalDays.toString(), style = WidgetText.serif(cardNumberSize(compact)), maxLines = 1)
            Text(
                text = context.daysTogetherLabel(orbit.totalDays),
                style = WidgetText.sans(WidgetText.Body),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight().padding(start = SmallGap),
            )
            Text(text = context.sinceText(orbit.since), style = WidgetText.muted(WidgetText.Small), maxLines = 1)
        }
        Spacer(modifier = GlanceModifier.height(SmallGap))
        LinearProgressIndicator(
            progress = orbit.milestoneProgress,
            modifier = GlanceModifier.fillMaxWidth().height(ProgressHeight),
            color = GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.outline,
        )
        Spacer(modifier = GlanceModifier.height(TinyGap))
        Text(text = context.milestoneText(orbit), style = WidgetText.muted(WidgetText.Small), maxLines = 1)
        if (!compact) {
            Text(text = context.orbitSummary(orbit), style = WidgetText.sans(WidgetText.Small), maxLines = 1)
        }
    }
}

@Composable
private fun cardModifier(compact: Boolean, onTap: Action?): GlanceModifier = GlanceModifier
    .fillMaxWidth()
    .background(ImageProvider(widgetPalette.drawables.card))
    .padding(if (compact) CompactCardPadding else CardPadding)
    .onTap(onTap)

private fun cardNumberSize(compact: Boolean): TextUnit = if (compact) WidgetText.CardNumberCompact else WidgetText.Days

/** Her direction, and both local times when you are in different time zones (tall large widget). */
@Composable
private fun InfoRow(state: WidgetState) {
    val context = LocalContext.current
    val direction = context.directionText(state)
    val herZone = state.partnerTimeZone
    if (direction == null && herZone == null) return
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = direction.orEmpty(),
            style = WidgetText.sans(WidgetText.Small),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (herZone != null) LocalTimes(herZone, state.partner)
    }
}

/** Live clocks (TextClock), so the times stay right between widget updates. */
@Composable
private fun LocalTimes(herZone: ZoneId, partner: PartnerName?) {
    val context = LocalContext.current
    val clockText = widgetPalette.clockText?.toArgb()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = context.getString(R.string.home_you) + " ", style = WidgetText.muted(WidgetText.Small))
        AndroidRemoteViews(clock(context.packageName, zone = null, clockText))
        Text(text = Separator + context.partnerLabelStart(partner) + " ", style = WidgetText.muted(WidgetText.Small), maxLines = 1)
        AndroidRemoteViews(clock(context.packageName, zone = herZone, clockText))
    }
}

/** A live clock in [zone] (this phone's when null), in the forced theme's text colour when set. */
private fun clock(packageName: String, zone: ZoneId?, textColor: Int?): RemoteViews =
    RemoteViews(packageName, R.layout.widget_clock).apply {
        zone?.let { setString(R.id.widget_clock, "setTimeZone", it.id) }
        textColor?.let { setTextColor(R.id.widget_clock, it) }
    }

/** A waiting Shooting Star (opens it) or new memories (opens Ours): text only (FR-WGT-3). */
@Composable
private fun Extras(state: WidgetState, actions: WidgetActions) {
    val context = LocalContext.current
    val (text, action) = when {
        state.hasWaitingStar -> context.getString(R.string.widget_star_waiting) to actions.openShootingStar
        state.newMemoryCount > 0 -> context.newMemoriesText(state.newMemoryCount) to actions.openVault
        else -> return
    }
    Text(
        text = text,
        style = WidgetText.sans(WidgetText.Small, color = widgetPalette.goldText, weight = FontWeight.Bold),
        maxLines = 1,
        modifier = GlanceModifier.fillMaxWidth().padding(top = Gap).onTap(action),
    )
}

private fun GlanceModifier.onTap(action: Action?): GlanceModifier = if (action != null) clickable(action) else this
