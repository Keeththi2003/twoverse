package app.twoverse.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.twoverse.R
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.designsystem.theme.DarkTwoverseColors
import app.twoverse.core.designsystem.theme.LightTwoverseColors
import app.twoverse.core.designsystem.theme.toColorScheme
import app.twoverse.core.model.DistanceUnit

/** Twoverse palette for Glance, switching with the system theme (FR-WGT-5). */
private val WidgetColors = ColorProviders(
    light = LightTwoverseColors.toColorScheme(),
    dark = DarkTwoverseColors.toColorScheme(),
)
private val GoldText = ColorProvider(day = LightTwoverseColors.goldText, night = DarkTwoverseColors.goldText)
private val Gold = ColorProvider(day = LightTwoverseColors.gold, night = DarkTwoverseColors.gold)

/** Glance has no access to the app's Compose typography, so the widget mirrors its sizes here. */
private val LabelSize = 13.sp
private val BadgeSize = 12.sp
private val DistanceSize = 34.sp
private val FootnoteSize = 14.sp
private val WidgetPadding = 16.dp
private val WidgetCorner = 24.dp
private val DotSize = 7.dp
private val DotGap = 6.dp
private val LabelGap = 4.dp

@Composable
internal fun TwoverseWidgetContent(state: WidgetState?, openHome: Intent) {
    GlanceTheme(colors = WidgetColors) {
        WidgetBody(state = state, modifier = GlanceModifier.clickable(actionStartActivity(openHome)))
    }
}

@Composable
private fun WidgetBody(state: WidgetState?, modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .cornerRadius(WidgetCorner)
            .background(colors.surface)
            .padding(WidgetPadding),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = context.getString(R.string.widget_distance_label),
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = LabelSize, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
            )
            state?.let { FreshnessBadge(it.freshness) }
        }
        Spacer(modifier = GlanceModifier.height(LabelGap))
        Text(
            text = state?.distance?.let {
                context.getString(R.string.widget_distance, it, context.getString(state.distanceUnit.unitRes()))
            } ?: context.getString(R.string.home_distance_unavailable),
            style = TextStyle(
                color = colors.onSurface,
                fontSize = if (state?.distance != null) DistanceSize else FootnoteSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
            ),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        val days = state?.daysUntilReunion
        Text(
            text = if (days != null) {
                context.resources.getQuantityString(R.plurals.widget_days_until, days.toInt(), days)
            } else {
                context.getString(R.string.widget_no_date)
            },
            style = TextStyle(color = colors.onSurface, fontSize = FootnoteSize, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun FreshnessBadge(freshness: LocationFreshness) {
    val context = LocalContext.current
    val labelRes = when (freshness) {
        LocationFreshness.Live -> R.string.home_freshness_live
        LocationFreshness.Recent -> R.string.home_freshness_recent
        LocationFreshness.Outdated -> R.string.home_freshness_outdated
        LocationFreshness.Unavailable -> return
    }
    val isLive = freshness == LocationFreshness.Live
    val muted = GlanceTheme.colors.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .size(DotSize)
                .cornerRadius(DotSize / 2)
                .background(if (isLive) Gold else muted),
        ) {}
        Spacer(modifier = GlanceModifier.width(DotGap))
        Text(
            text = context.getString(labelRes),
            style = TextStyle(
                color = if (isLive) GoldText else muted,
                fontSize = BadgeSize,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

private fun DistanceUnit.unitRes(): Int = when (this) {
    DistanceUnit.Kilometres -> R.string.unit_km
    DistanceUnit.Miles -> R.string.unit_miles
}

private val PreviewState = WidgetState(
    distance = "94.6",
    distanceUnit = DistanceUnit.Kilometres,
    freshness = LocationFreshness.Live,
    daysUntilReunion = 12,
)

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 140)
@Composable
private fun WidgetLightPreview() {
    GlanceTheme(colors = ColorProviders(LightTwoverseColors.toColorScheme())) {
        WidgetBody(state = PreviewState)
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 140)
@Composable
private fun WidgetDarkPreview() {
    GlanceTheme(colors = ColorProviders(DarkTwoverseColors.toColorScheme())) {
        WidgetBody(state = PreviewState)
    }
}
