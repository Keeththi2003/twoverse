package app.twoverse.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness

// Glance previews for every size and state (FR-WGT-1, FR-WGT-6), light and dark. Sizes are
// typical 2×2, 4×2, 4×3 and 4×4 cells.

private val Unavailable = SampleWidgetState.copy(
    location = WidgetLocation.Unavailable,
    distance = null,
    freshness = LocationFreshness.Unavailable,
    updatedAgo = null,
    direction = null,
)

@Composable
private fun WidgetPreview(state: WidgetState, widthDp: Int, heightDp: Int, dark: Boolean) {
    WidgetPreviewTheme(dark = dark) {
        WidgetBody(state = state, size = DpSize(widthDp.dp, heightDp.dp), actions = WidgetActions())
    }
}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallLiveLightPreview() {
    WidgetPreview(SampleWidgetState, 150, 150, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallLiveDarkPreview() {
    WidgetPreview(SampleWidgetState, 150, 150, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallRecentLightPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Recent, updatedAgo = ElapsedTime.Minutes(5), hasWaitingStar = true), 150, 150, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallRecentDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Recent, updatedAgo = ElapsedTime.Minutes(5), hasWaitingStar = true), 150, 150, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallOfflineLightPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Outdated, updatedAgo = ElapsedTime.Hours(3), isOffline = true), 150, 150, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallOfflineDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Outdated, updatedAgo = ElapsedTime.Hours(3), isOffline = true), 150, 150, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallNoReunionLightPreview() {
    WidgetPreview(SampleWidgetState.copy(reunion = null, newMemoryCount = 0), 150, 150, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallNoReunionDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(reunion = null, newMemoryCount = 0), 150, 150, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallSharingOffLightPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.SharingOff), 150, 150, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallSharingOffDarkPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.SharingOff), 150, 150, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallDistanceUnavailableLightPreview() {
    WidgetPreview(Unavailable, 150, 150, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallDistanceUnavailableDarkPreview() {
    WidgetPreview(Unavailable, 150, 150, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallNotPairedLightPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.NotPaired, reunion = null, newMemoryCount = 0), 150, 150, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 150, heightDp = 150)
@Composable
private fun SmallNotPairedDarkPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.NotPaired, reunion = null, newMemoryCount = 0), 150, 150, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumLiveLightPreview() {
    WidgetPreview(SampleWidgetState, 280, 140, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumLiveDarkPreview() {
    WidgetPreview(SampleWidgetState, 280, 140, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumRecentLightPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Recent, updatedAgo = ElapsedTime.Minutes(5), hasWaitingStar = true), 280, 140, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumRecentDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Recent, updatedAgo = ElapsedTime.Minutes(5), hasWaitingStar = true), 280, 140, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumOfflineLightPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Outdated, updatedAgo = ElapsedTime.Hours(3), isOffline = true), 280, 140, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumOfflineDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Outdated, updatedAgo = ElapsedTime.Hours(3), isOffline = true), 280, 140, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumNoReunionLightPreview() {
    WidgetPreview(SampleWidgetState.copy(reunion = null, newMemoryCount = 0), 280, 140, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumNoReunionDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(reunion = null, newMemoryCount = 0), 280, 140, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumSharingOffLightPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.SharingOff), 280, 140, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumSharingOffDarkPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.SharingOff), 280, 140, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumDistanceUnavailableLightPreview() {
    WidgetPreview(Unavailable, 280, 140, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumDistanceUnavailableDarkPreview() {
    WidgetPreview(Unavailable, 280, 140, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumNotPairedLightPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.NotPaired, reunion = null, newMemoryCount = 0), 280, 140, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Composable
private fun MediumNotPairedDarkPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.NotPaired, reunion = null, newMemoryCount = 0), 280, 140, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeLiveLightPreview() {
    WidgetPreview(SampleWidgetState, 280, 210, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeLiveDarkPreview() {
    WidgetPreview(SampleWidgetState, 280, 210, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeRecentLightPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Recent, updatedAgo = ElapsedTime.Minutes(5), hasWaitingStar = true), 280, 210, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeRecentDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Recent, updatedAgo = ElapsedTime.Minutes(5), hasWaitingStar = true), 280, 210, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeOfflineLightPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Outdated, updatedAgo = ElapsedTime.Hours(3), isOffline = true), 280, 210, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeOfflineDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Outdated, updatedAgo = ElapsedTime.Hours(3), isOffline = true), 280, 210, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeNoReunionLightPreview() {
    WidgetPreview(SampleWidgetState.copy(reunion = null, newMemoryCount = 0), 280, 210, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeNoReunionDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(reunion = null, newMemoryCount = 0), 280, 210, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeSharingOffLightPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.SharingOff), 280, 210, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeSharingOffDarkPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.SharingOff), 280, 210, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeDistanceUnavailableLightPreview() {
    WidgetPreview(Unavailable, 280, 210, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeDistanceUnavailableDarkPreview() {
    WidgetPreview(Unavailable, 280, 210, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeNotPairedLightPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.NotPaired, reunion = null, newMemoryCount = 0), 280, 210, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 210)
@Composable
private fun LargeNotPairedDarkPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.NotPaired, reunion = null, newMemoryCount = 0), 280, 210, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallLiveLightPreview() {
    WidgetPreview(SampleWidgetState, 280, 300, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallLiveDarkPreview() {
    WidgetPreview(SampleWidgetState, 280, 300, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallRecentLightPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Recent, updatedAgo = ElapsedTime.Minutes(5), hasWaitingStar = true), 280, 300, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallRecentDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Recent, updatedAgo = ElapsedTime.Minutes(5), hasWaitingStar = true), 280, 300, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallOfflineLightPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Outdated, updatedAgo = ElapsedTime.Hours(3), isOffline = true), 280, 300, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallOfflineDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(freshness = LocationFreshness.Outdated, updatedAgo = ElapsedTime.Hours(3), isOffline = true), 280, 300, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallNoReunionLightPreview() {
    WidgetPreview(SampleWidgetState.copy(reunion = null, newMemoryCount = 0), 280, 300, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallNoReunionDarkPreview() {
    WidgetPreview(SampleWidgetState.copy(reunion = null, newMemoryCount = 0), 280, 300, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallSharingOffLightPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.SharingOff), 280, 300, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallSharingOffDarkPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.SharingOff), 280, 300, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallDistanceUnavailableLightPreview() {
    WidgetPreview(Unavailable, 280, 300, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallDistanceUnavailableDarkPreview() {
    WidgetPreview(Unavailable, 280, 300, dark = true)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallNotPairedLightPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.NotPaired, reunion = null, newMemoryCount = 0), 280, 300, dark = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 300)
@Composable
private fun TallNotPairedDarkPreview() {
    WidgetPreview(Unavailable.copy(location = WidgetLocation.NotPaired, reunion = null, newMemoryCount = 0), 280, 300, dark = true)
}
