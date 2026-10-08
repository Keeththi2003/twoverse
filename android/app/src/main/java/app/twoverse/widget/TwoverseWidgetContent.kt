package app.twoverse.widget

import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
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
import java.time.ZoneId

/** Glance picks the largest of these that fits the widget (FR-WGT-1). */
internal val SmallWidget = DpSize(110.dp, 110.dp)
internal val MediumWidget = DpSize(250.dp, 110.dp)
internal val LargeWidget = DpSize(250.dp, 180.dp)
internal val WidgetSizes = setOf(SmallWidget, MediumWidget, LargeWidget)

/** From this height the large widget adds her direction, both local times and the extras line. */
private val TallHeight = 250.dp

private val WidgetPadding = 16.dp
private val Gap = 8.dp
private val SmallGap = 4.dp
private val DotSize = 8.dp
private val DotGap = 6.dp
private val IconSize = 14.dp
private val ProgressPlanetSize = 12.dp
private val ProgressHeight = 4.dp
private val CardPadding = 12.dp
private val MediumOrbitWidth = 112.dp
private val MediumOrbitHeight = 68.dp
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
    WidgetTheme {
        WidgetBody(state = state, size = LocalSize.current, actions = actions)
    }
}

/** Chooses the layout for [size]; while the saved data loads, only the faded orbit shows. */
@Composable
internal fun WidgetBody(state: WidgetState?, size: DpSize, actions: WidgetActions) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground()
            .padding(WidgetPadding)
            .onTap(actions.openHome),
    ) {
        when {
            state == null -> Orbit(R.drawable.widget_orbit_small, faded = true, modifier = GlanceModifier.fillMaxSize())
            size.width >= MediumWidget.width && size.height >= LargeWidget.height ->
                LargeLayout(state, isTall = size.height >= TallHeight, actions = actions)
            size.width >= MediumWidget.width -> MediumLayout(state, actions)
            else -> SmallLayout(state)
        }
    }
}

/** 2×2: planets and arc, the distance and how fresh it is. */
@Composable
private fun ColumnScope.SmallLayout(state: WidgetState) {
    val context = LocalContext.current
    Orbit(
        drawable = R.drawable.widget_orbit_small,
        faded = state.location != WidgetLocation.Available,
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

/** 4×2: distance, "apart" and freshness beside the planets, with the reunion below. */
@Composable
private fun ColumnScope.MediumLayout(state: WidgetState, actions: WidgetActions) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            if (state.distance != null) {
                Distance(value = state.distance, unit = context.unitApart(state.distanceUnit), size = WidgetText.DistanceMedium)
                Freshness(state, short = false)
            } else {
                StatusMessage(state)
            }
        }
        Orbit(
            drawable = R.drawable.widget_orbit_medium,
            faded = state.location != WidgetLocation.Available,
            modifier = GlanceModifier.size(MediumOrbitWidth, MediumOrbitHeight),
        )
    }
    if (state.isPaired) {
        Spacer(modifier = GlanceModifier.height(Gap))
        ReunionLine(state.reunion, orbit = state.orbit, onTap = actions.openCountdown)
    }
}

/** 4×3 and bigger: the orbit with the distance inside, details as space allows, and the reunion card. */
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
            drawable = R.drawable.widget_orbit_large,
            faded = state.location != WidgetLocation.Available,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.distance != null) {
                Text(text = state.distance, style = WidgetText.serif(WidgetText.DistanceLarge), maxLines = 1)
                Text(text = context.unitApart(state.distanceUnit), style = WidgetText.muted())
            } else {
                StatusMessage(state, centered = true)
            }
        }
    }
    if (isTall) InfoRow(state)
    state.orbit?.let { OrbitRow(it, onTap = actions.openOrbit) }
    if (state.isPaired) {
        Spacer(modifier = GlanceModifier.height(Gap))
        ReunionCard(state.reunion, onTap = actions.openCountdown)
        if (isTall) Extras(state, actions)
    }
}

@Composable
private fun Orbit(@DrawableRes drawable: Int, faded: Boolean, modifier: GlanceModifier) {
    Image(
        provider = ImageProvider(drawable),
        contentDescription = LocalContext.current.getString(R.string.widget_orbit_description),
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
                provider = ImageProvider(if (isLive) R.drawable.widget_dot_live else R.drawable.widget_dot_muted),
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

/** "16 days until we meet · 845 days together" with a calendar icon (medium widget). */
@Composable
private fun ReunionLine(reunion: WidgetReunion?, orbit: WidgetOrbit?, onTap: Action?) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxWidth().onTap(onTap), verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.widget_ic_calendar),
            contentDescription = null,
            modifier = GlanceModifier.size(IconSize),
        )
        Spacer(modifier = GlanceModifier.width(DotGap))
        val reunionText = reunion?.let { context.daysUntilText(it) } ?: context.getString(R.string.widget_no_date)
        Text(
            text = listOfNotNull(reunionText, orbit?.let { context.daysTogetherText(it.totalDays) }).joinToString(Separator),
            style = if (reunion != null) WidgetText.sans(WidgetText.Body) else WidgetText.muted(),
            maxLines = 1,
        )
    }
}

/** Days until the reunion, its date and the planets moving closer, yours left and hers right (large widget). */
@Composable
private fun ReunionCard(reunion: WidgetReunion?, onTap: Action?) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_card_background))
            .padding(CardPadding)
            .onTap(onTap),
    ) {
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
                Text(text = reunion.daysUntil.toString(), style = WidgetText.serif(WidgetText.Days), maxLines = 1)
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
            Image(ImageProvider(R.drawable.widget_planet_you), null, GlanceModifier.size(ProgressPlanetSize))
            LinearProgressIndicator(
                progress = reunion.progress,
                modifier = GlanceModifier.defaultWeight().height(ProgressHeight).padding(horizontal = SmallGap),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.outline,
            )
            Image(ImageProvider(R.drawable.widget_planet_her), null, GlanceModifier.size(ProgressPlanetSize))
        }
    }
}

/** Together for, times met, and the anniversary when it is close; opens Our Orbit (FR-WGT-8). */
@Composable
private fun OrbitRow(orbit: WidgetOrbit, onTap: Action?) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxWidth().padding(top = SmallGap).onTap(onTap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.widget_ic_heart),
                contentDescription = null,
                modifier = GlanceModifier.size(IconSize),
            )
            Spacer(modifier = GlanceModifier.width(DotGap))
            Text(text = context.orbitSummary(orbit), style = WidgetText.sans(WidgetText.Small), maxLines = 1)
        }
        orbit.anniversary?.let {
            Text(
                text = context.anniversarySoonText(it.daysUntil),
                style = WidgetText.sans(WidgetText.Small, color = widgetPalette.goldText, weight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.padding(start = IconSize + DotGap),
            )
        }
    }
}

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
        if (herZone != null) LocalTimes(herZone)
    }
}

/** Live clocks (TextClock), so the times stay right between widget updates. */
@Composable
private fun LocalTimes(herZone: ZoneId) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = context.getString(R.string.home_you) + " ", style = WidgetText.muted(WidgetText.Small))
        AndroidRemoteViews(RemoteViews(context.packageName, R.layout.widget_clock))
        Text(text = Separator + context.getString(R.string.home_her) + " ", style = WidgetText.muted(WidgetText.Small))
        AndroidRemoteViews(
            RemoteViews(context.packageName, R.layout.widget_clock).apply {
                setString(R.id.widget_clock, "setTimeZone", herZone.id)
            },
        )
    }
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
